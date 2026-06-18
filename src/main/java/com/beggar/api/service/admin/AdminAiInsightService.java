package com.beggar.api.service.admin;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.admin.ai.BudgetRiskPredictionRequest;
import com.beggar.api.dto.admin.ai.SpendingInsightRequest;
import com.beggar.api.dto.admin.ai.SpendingInsightResponse;
import com.beggar.api.entity.Budget;
import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomPurposeTag;
import com.beggar.api.repository.BudgetRepository;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomPurposeTagRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiInsightService {

    private static final int DEFAULT_BUDGET_RISK_PAGE_SIZE = 10;
    private static final int MAX_BUDGET_RISK_PAGE_SIZE = 50;

    private final RoomRepository roomRepository;
    private final ReceiptRepository receiptRepository;
    private final RoomPurposeTagRepository roomPurposeTagRepository;
    private final BudgetRepository budgetRepository;
    private final JdbcTemplate jdbcTemplate;

    @Qualifier("aiServerWebClient")
    private final WebClient aiServerWebClient;

    public SpendingInsightResponse getSpendingSummary() {
        SpendingInsightRequest request = new SpendingInsightRequest(
                buildRooms(),
                buildReceipts(),
                buildRecommendationInteractions()
        );

        try {
            return aiServerWebClient.post()
                    .uri("/api/v1/insights/spending-summary")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SpendingInsightResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new CustomException(
                    ErrorCode.EXTERNAL_API_FAILED,
                    "AI 소비 인사이트 API 호출에 실패했습니다. status=" + e.getStatusCode().value()
            );
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED, "AI 소비 인사이트 API 호출에 실패했습니다.");
        }
    }

    public Map<String, Object> getBudgetRiskPredictions(int page, int size) {
        BudgetRiskPredictionRequest request = new BudgetRiskPredictionRequest(
                buildRiskRooms(),
                buildRiskReceipts(),
                buildRiskBudgets()
        );

        try {
            Map<String, Object> response = aiServerWebClient.post()
                    .uri("/api/v1/predictions/budget-risk")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
            return buildBudgetRiskDashboardResponse(response, page, size);
        } catch (WebClientResponseException e) {
            throw new CustomException(
                    ErrorCode.EXTERNAL_API_FAILED,
                    "AI 예산 위험 예측 API 호출에 실패했습니다. status=" + e.getStatusCode().value()
            );
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED, "AI 예산 위험 예측 API 호출에 실패했습니다.");
        }
    }

    private Map<String, Object> buildBudgetRiskDashboardResponse(Map<String, Object> response, int page, int size) {
        if (response == null) {
            return Map.of(
                    "summary", budgetRiskSummary(List.of()),
                    "items", List.of(),
                    "page", 0,
                    "size", DEFAULT_BUDGET_RISK_PAGE_SIZE,
                    "totalItems", 0,
                    "totalPages", 0,
                    "hasPrevious", false,
                    "hasNext", false
            );
        }
        Map<String, Object> limited = new LinkedHashMap<>(response);
        Object items = response.get("items");
        if (items instanceof List<?> list) {
            int safeSize = clampPageSize(size);
            int totalItems = list.size();
            int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
            int safePage = clampPage(page, totalPages);
            int fromIndex = Math.min(safePage * safeSize, totalItems);
            int toIndex = Math.min(fromIndex + safeSize, totalItems);

            limited.put("summary", budgetRiskSummary(list));
            limited.put("items", new ArrayList<>(list.subList(fromIndex, toIndex)));
            limited.put("page", safePage);
            limited.put("size", safeSize);
            limited.put("totalItems", totalItems);
            limited.put("totalPages", totalPages);
            limited.put("hasPrevious", safePage > 0);
            limited.put("hasNext", totalPages > 0 && safePage < totalPages - 1);
        } else {
            limited.put("summary", budgetRiskSummary(List.of()));
            limited.put("items", List.of());
            limited.put("page", 0);
            limited.put("size", clampPageSize(size));
            limited.put("totalItems", 0);
            limited.put("totalPages", 0);
            limited.put("hasPrevious", false);
            limited.put("hasNext", false);
        }
        return limited;
    }

    private int clampPageSize(int size) {
        if (size <= 0) {
            return DEFAULT_BUDGET_RISK_PAGE_SIZE;
        }
        return Math.min(size, MAX_BUDGET_RISK_PAGE_SIZE);
    }

    private int clampPage(int page, int totalPages) {
        if (page <= 0 || totalPages <= 0) {
            return 0;
        }
        return Math.min(page, totalPages - 1);
    }

    private Map<String, Object> budgetRiskSummary(List<?> items) {
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        double totalScore = 0.0;

        for (Object item : items) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String riskLevel = String.valueOf(map.get("riskLevel"));
            if ("HIGH".equals(riskLevel)) {
                highCount++;
            } else if ("MEDIUM".equals(riskLevel)) {
                mediumCount++;
            } else if ("LOW".equals(riskLevel)) {
                lowCount++;
            }
            Object riskScore = map.get("riskScore");
            if (riskScore instanceof Number number) {
                totalScore += number.doubleValue();
            }
        }

        int totalCount = highCount + mediumCount + lowCount;
        double averageRiskScore = totalCount == 0 ? 0.0 : roundOneDecimal(totalScore / totalCount);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRoomCount", totalCount);
        summary.put("highCount", highCount);
        summary.put("mediumCount", mediumCount);
        summary.put("lowCount", lowCount);
        summary.put("averageRiskScore", averageRiskScore);
        summary.put("highRate", percentage(highCount, totalCount));
        summary.put("mediumRate", percentage(mediumCount, totalCount));
        summary.put("lowRate", percentage(lowCount, totalCount));
        return summary;
    }

    private double percentage(int count, int totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        return roundOneDecimal((double) count / totalCount * 100);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private List<SpendingInsightRequest.RoomInsightItem> buildRooms() {
        Map<Long, String> tagByRoomNo = roomPurposeTagRepository.findAllWithRoom().stream()
                .collect(Collectors.toMap(
                        tag -> tag.getRoom().getRoomNo(),
                        RoomPurposeTag::getTag,
                        (first, ignored) -> first
                ));

        return roomRepository.findAll().stream()
                .map(room -> new SpendingInsightRequest.RoomInsightItem(
                        room.getRoomNo(),
                        room.getRoomName(),
                        room.getLocation(),
                        tagByRoomNo.get(room.getRoomNo()),
                        room.getTotalBudget(),
                        room.getMaxMemberCount(),
                        room.getStatus() == null ? null : room.getStatus().name()
                ))
                .toList();
    }

    private List<BudgetRiskPredictionRequest.RoomRiskItem> buildRiskRooms() {
        Map<Long, String> tagByRoomNo = roomPurposeTagRepository.findAllWithRoom().stream()
                .collect(Collectors.toMap(
                        tag -> tag.getRoom().getRoomNo(),
                        RoomPurposeTag::getTag,
                        (first, ignored) -> first
                ));

        return roomRepository.findAll().stream()
                .map(room -> new BudgetRiskPredictionRequest.RoomRiskItem(
                        room.getRoomNo(),
                        room.getRoomName(),
                        room.getTotalBudget(),
                        room.getLocation(),
                        tagByRoomNo.get(room.getRoomNo()),
                        room.getMaxMemberCount(),
                        room.getStatus() == null ? null : room.getStatus().name(),
                        room.getRoomCreated(),
                        room.getEndedAt()
                ))
                .toList();
    }

    private List<SpendingInsightRequest.ReceiptInsightItem> buildReceipts() {
        return receiptRepository.findAllByConfirmedTrue().stream()
                .map(this::toReceiptInsightItem)
                .toList();
    }

    private SpendingInsightRequest.ReceiptInsightItem toReceiptInsightItem(Receipt receipt) {
        Room room = receipt.getRoom();
        return new SpendingInsightRequest.ReceiptInsightItem(
                receipt.getReceiptId(),
                room.getRoomNo(),
                receipt.getAmount(),
                receipt.getStoreName(),
                receipt.getReceiptType() == null ? null : receipt.getReceiptType().name(),
                Boolean.TRUE.equals(receipt.getGoodPriceMatched()),
                receipt.getReceiptIssuedAt() == null ? receipt.getCreatedAt() : receipt.getReceiptIssuedAt()
        );
    }

    private List<BudgetRiskPredictionRequest.ReceiptRiskItem> buildRiskReceipts() {
        return receiptRepository.findAllByConfirmedTrue().stream()
                .map(receipt -> new BudgetRiskPredictionRequest.ReceiptRiskItem(
                        receipt.getRoom().getRoomNo(),
                        receipt.getAmount(),
                        receipt.getReceiptType() == null ? null : receipt.getReceiptType().name(),
                        Boolean.TRUE.equals(receipt.getGoodPriceMatched()),
                        receipt.getReceiptIssuedAt() == null ? receipt.getCreatedAt() : receipt.getReceiptIssuedAt()
                ))
                .toList();
    }

    private List<BudgetRiskPredictionRequest.BudgetRiskItem> buildRiskBudgets() {
        return budgetRepository.findAll().stream()
                .map(this::toBudgetRiskItem)
                .toList();
    }

    private BudgetRiskPredictionRequest.BudgetRiskItem toBudgetRiskItem(Budget budget) {
        return new BudgetRiskPredictionRequest.BudgetRiskItem(
                budget.getRoomNo(),
                budget.getAmount(),
                null
        );
    }

    private List<SpendingInsightRequest.RecommendationInteractionItem> buildRecommendationInteractions() {
        try {
            return jdbcTemplate.query(
                    """
                    SELECT room_no, requested_tag, action, expected_price, created_at
                      FROM recommendation_interactions
                    """,
                    (rs, rowNum) -> new SpendingInsightRequest.RecommendationInteractionItem(
                            rs.getLong("room_no"),
                            rs.getString("requested_tag"),
                            rs.getString("action"),
                            rs.getObject("expected_price", Integer.class),
                            toLocalDateTime(rs.getTimestamp("created_at"))
                    )
            );
        } catch (DataAccessException e) {
            return List.of();
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
