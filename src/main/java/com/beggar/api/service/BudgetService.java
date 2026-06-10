package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.budget.BudgetResultResponse;
import com.beggar.api.dto.room.RoomEventDto;
import com.beggar.api.entity.Budget;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomBudgetResult;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.BudgetRepository;
import com.beggar.api.repository.RoomBudgetResultRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomBudgetResultRepository roomBudgetResultRepository;
    private final RoomEventService roomEventService;

    /**
     *  본인 예산 제출 (INSERT or UPDATE)
     */
    @Transactional
    public void submitBudget(Long userNo, Long roomNo, Integer budgetAmount) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "존재하지 않는 거지방입니다."));

        // 방 상태 검증
        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

        if (room.getStatus() != RoomStatus.BUDGET_INPUT) {
            throw new IllegalArgumentException("현재 방 상태가 '" + room.getStatus() + "'입니다. 예산 입력 단계(BUDGET_INPUT)에서만 제출이 가능합니다.");
        }

        // 멤버 상태 검증
        RoomMember member = roomMemberRepository.findByRoom_RoomNoAndUser_UserNo(roomNo, userNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 멤버가 아닙니다."));
        if (member.getStatus() != RoomMember.Status.ACTIVE) {
            throw new IllegalArgumentException("활성화된 멤버만 예산을 제출할 수 있습니다.");
        }

        // 예산 저장/수정
        Budget budget = budgetRepository.findByRoomNoAndUserNo(roomNo, userNo)
                .orElse(null);

        if (budget == null) {
            budget = new Budget(roomNo, userNo, budgetAmount);
        } else {
            budget.updateAmount(budgetAmount);
        }
        budgetRepository.save(budget);

        // 이벤트 발행 (제출 상태)
        long totalMembers = roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, RoomMember.Status.ACTIVE);
        long submittedCount = budgetRepository.countByRoomNo(roomNo);
        
        roomEventService.publishBudgetSubmitted(roomNo, java.util.Map.of(
                "submittedCount", submittedCount,
                "memberCount", totalMembers
        ));

        // 전원 제출 시 자동 확정
        if (totalMembers > 0 && totalMembers == submittedCount) {
            this.confirmBudget(roomNo);
        }
    }

    /**
     *  1-1. 본인이 제출한 예산 조회
     */
    public Integer findMyBudget(Long roomNo, Long userNo) {
        return budgetRepository.findByRoomNoAndUserNo(roomNo, userNo)
                .map(Budget::getAmount)
                .orElse(null);
    }

    /**
     *  2. 강제 확정 / 자동 확정 처리
     */
    @Transactional
    public void confirmBudget(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "존재하지 않는 거지방입니다."));

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

        List<Budget> budgets = budgetRepository.findByRoomNo(roomNo);
        if (budgets.isEmpty()) {
            throw new IllegalStateException("제출된 예산이 없어 확정할 수 없습니다.");
        }

        // 최저 금액(MIN) 산출
        int minAmount = budgets.stream()
                .mapToInt(Budget::getAmount)
                .min()
                .orElse(0);

        int memberCount = budgets.size();
        int totalBudget = minAmount * memberCount;

        // 1. rooms 테이블 총예산 및 상태 동기화
        room.updateTotalBudget(totalBudget);
        room.completeBudgetInput();

        // 2. 결과 테이블 기록
        RoomBudgetResult result = roomBudgetResultRepository.findByRoom_RoomNo(roomNo)
                .orElseGet(() -> RoomBudgetResult.builder()
                        .room(room)
                        .minBudgetPerPerson(minAmount)
                        .memberCount(memberCount)
                        .totalBudget(totalBudget)
                        .confirmedAt(LocalDateTime.now())
                        .build());
        result.update(minAmount, memberCount, totalBudget);
        roomBudgetResultRepository.save(result);

        // 3. 확정 이벤트 발행
        roomEventService.publishStateChanged(roomNo, RoomEventDto.EventType.BUDGET_CONFIRMED, "/budget/result?roomNo=" + roomNo);

        System.out.println("거지방 [" + roomNo + "] 예산 최종 확정 완료!");
    }

    /*
     *  3. 확정된 예산 결과 조회
     */
    public BudgetResultResponse getResult(Long roomNo) {
        RoomBudgetResult roomBudgetResult = roomBudgetResultRepository.findByRoom_RoomNo(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("아직 예산이 확정되지 않은 방입니다."));

        return BudgetResultResponse.from(roomBudgetResult);
    }

    /**
     * 4. 확정된 예산 결과 엑셀 다운로드
     */
    public void exportBudgetToExcel(Long roomNo, OutputStream outputStream) throws IOException {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        RoomBudgetResult result = roomBudgetResultRepository.findByRoom_RoomNo(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("아직 예산이 확정되지 않은 방입니다."));

        // 1. 엑셀 워크북 및 시트 생성
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("거지방 예산 정산서");

            // 스타일 설정 (헤더용)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 2. 헤더 행 생성
            String[] headers = {"항목", "내용"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3. 데이터 채우기
            int rowIdx = 1;
            addExcelRow(sheet, rowIdx++, "거지방 이름", room.getRoomName());
            addExcelRow(sheet, rowIdx++, "최종 확정 일시", result.getConfirmedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            addExcelRow(sheet, rowIdx++, "참여 인원", result.getMemberCount() + "명");
            addExcelRow(sheet, rowIdx++, "1인당 목표 예산", String.format("%,d원", result.getMinBudgetPerPerson()));
            addExcelRow(sheet, rowIdx++, "방 전체 총 예산", String.format("%,d원", result.getTotalBudget()));

            // 컬럼 너비 자동 조정
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            // 4. 출력 스트림에 쓰기
            workbook.write(outputStream);
        }
    }

    private void addExcelRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}
