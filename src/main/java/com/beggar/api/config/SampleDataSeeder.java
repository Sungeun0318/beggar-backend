package com.beggar.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV 샘플 데이터를 RDS에 1회 적재하는 시더.
 *
 * - aws-1 브랜치 push → EB 배포 → 앱 시작 시 자동 실행된다.
 * - 테이블별로 이미 데이터가 있으면(COUNT > 0) 건너뛰므로 재배포해도 안전하다.
 * - 기본 OFF. 다시 적재가 필요하면 EB 환경변수 APP_SEED_ENABLED=true 를 줄 때만 실행된다.
 *
 * CSV 는 src/main/resources/seed/ 에 있고, jar 에 함께 포함되어 클래스패스에서 읽는다.
 * 컬럼은 "이름"으로 매핑하므로 CSV 컬럼 순서/여분 컬럼(users 의 age 등)에 영향받지 않는다.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class SampleDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);

    private final JdbcTemplate jdbc;

    public SampleDataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** (CSV 리소스, 대상 테이블, 적재할 컬럼) — FK 의존 순서대로 나열한다. */
    private record SeedSpec(String resource, String table, String columns) {}

    private static final List<SeedSpec> SPECS = List.of(
        new SeedSpec("seed/users_sample_1000.csv", "users",
            "user_no,user_name,password_hash,profile_image_url,uemail,role,gender,age_range,created_at,updated_at"),
        new SeedSpec("seed/rooms.csv", "rooms",
            "room_no,owner_user_no,room_name,room_code,max_member_count,total_budget,is_friends,location,status,room_created,ended_at,deleted_at"),
        new SeedSpec("seed/room_members.csv", "room_members",
            "room_member_id,room_no,user_no,status,is_hidden,joined_at,left_at"),
        new SeedSpec("seed/room_purpose_tags.csv", "room_purpose_tags",
            "tag_id,room_no,tag_tags"),
        new SeedSpec("seed/room_budget_results.csv", "room_budget_results",
            "result_id,room_no,min_budget_per_person,member_count,total_budget,confirmed_at"),
        new SeedSpec("seed/room_beggar_scores_1000_integer.csv", "room_beggar_scores",
            "score_id,room_no,score,title,total_spent_amount,total_saved_amount,"
            + "good_price_verified_count,budget_compliance_rate,avg_savings_ratio,last_calculated_at,updated_at"),
        new SeedSpec("seed/receipt_split_groups.csv", "receipt_split_groups",
            "split_group_id,room_no,store_name,address,center_lat,center_lng,status,"
            + "created_by_room_member_id,closed_at,created_at,updated_at"),
        new SeedSpec("seed/receipts.csv", "receipts",
            "receipt_id,room_no,room_member_id,receipt_type,input_method,image_url,image_hash,ocr_status,"
            + "store_name,total_amount,amount,receipt_issued_at,address,center_lat,center_lng,split_group_id,"
            + "good_price_store_id,good_price_store_name,good_price_store_address,good_price_matched,"
            + "good_price_verified_at,confirmed,created_at,updated_at"),
        // budgets.csv 는 현재 Budget 엔티티(테이블 budget: budget_no,room_no,user_no,amount)와 스키마가 달라
        // room_members 로 조인해 변환한 budget.csv 를 사용한다.
        // budget_no(500001~)는 PK 부여용 — 앱 자동생성 ID(낮은 값)와 안 겹치고, 재배포 시 중복 방지.
        new SeedSpec("seed/budget.csv", "budget",
            "budget_no,room_no,user_no,amount")
    );

    @Override
    public void run(ApplicationArguments args) {
        log.info("[seed] 샘플 데이터 시더 시작");
        int seeded = 0;
        for (SeedSpec spec : SPECS) {
            try {
                if (seedOne(spec)) {
                    seeded++;
                }
            } catch (Exception e) {
                // 한 테이블이 실패해도 나머지는 계속 시도한다.
                log.error("[seed] {} 적재 실패: {}", spec.table(), e.getMessage(), e);
            }
        }
        log.info("[seed] 시더 종료 (이번에 적재한 테이블 {}개)", seeded);
    }

    /** @return 실제로 1행이라도 적재했으면 true */
    private boolean seedOne(SeedSpec spec) throws Exception {
        // 기존 데이터가 있어도 적재한다(덧붙이기). 중복은 PK 기준 INSERT IGNORE 로 걸러진다.
        Integer before = jdbc.queryForObject("SELECT COUNT(*) FROM " + spec.table(), Integer.class);

        String[] targetCols = spec.columns().split(",");
        List<String[]> rows = readCsv(spec.resource(), targetCols);
        if (rows.isEmpty()) {
            log.warn("[seed] {} CSV 데이터 없음 → 건너뜀", spec.resource());
            return false;
        }

        String placeholders = "?" + ",?".repeat(targetCols.length - 1);
        String sql = "INSERT IGNORE INTO " + spec.table()
                + " (" + spec.columns() + ") VALUES (" + placeholders + ")";

        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return rows.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String[] r = rows.get(i);
                for (int c = 0; c < r.length; c++) {
                    String v = r[c];
                    if (v == null || v.isEmpty()) {
                        ps.setNull(c + 1, Types.VARCHAR);          // 빈 값 → NULL
                    } else if ("TRUE".equalsIgnoreCase(v)) {
                        ps.setInt(c + 1, 1);                       // BOOLEAN 컬럼 대응
                    } else if ("FALSE".equalsIgnoreCase(v)) {
                        ps.setInt(c + 1, 0);
                    } else {
                        ps.setString(c + 1, v);                    // 숫자/날짜/문자 모두 문자열로 → MySQL 변환
                    }
                }
            }
        });

        Integer after = jdbc.queryForObject("SELECT COUNT(*) FROM " + spec.table(), Integer.class);
        int inserted = (after == null ? 0 : after) - (before == null ? 0 : before);
        int skipped = rows.size() - inserted;
        log.info("[seed] {} 적재: 신규 {}행, 중복건너뜀 {}행 (CSV {}행, 현재 총 {}행)",
                spec.table(), inserted, skipped, rows.size(), after);
        return inserted > 0;
    }

    /** CSV 를 읽어 targetCols 순서대로 값 배열 리스트를 만든다(헤더 이름 기준 매핑). */
    private List<String[]> readCsv(String resource, String[] targetCols) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(resource).getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = br.readLine();
            if (headerLine == null) {
                return rows;
            }
            headerLine = stripBom(headerLine);

            String[] header = headerLine.split(",", -1);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                idx.put(header[i].trim(), i);
            }

            // targetCols 가 CSV 에 실제로 있는지 검증(없으면 즉시 실패)
            int[] map = new int[targetCols.length];
            for (int i = 0; i < targetCols.length; i++) {
                Integer pos = idx.get(targetCols[i].trim());
                if (pos == null) {
                    throw new IllegalStateException(
                        resource + " 에 컬럼 '" + targetCols[i] + "' 없음. 헤더=" + headerLine);
                }
                map[i] = pos;
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                String[] picked = new String[targetCols.length];
                for (int i = 0; i < map.length; i++) {
                    picked[i] = map[i] < fields.length ? fields[map[i]] : "";
                }
                rows.add(picked);
            }
        }
        return rows;
    }

    private String stripBom(String s) {
        return (!s.isEmpty() && s.charAt(0) == '﻿') ? s.substring(1) : s;
    }
}
