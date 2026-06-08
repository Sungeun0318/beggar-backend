package com.beggar.api.service;

import com.beggar.api.dto.room.RoomEventDto;
import com.beggar.api.entity.*;
import com.beggar.api.dto.budget.BudgetResultResponse;
import com.beggar.api.repository.BudgetRepository;
import com.beggar.api.repository.RoomRepository;
import com.beggar.api.repository.RoomMemberRepository; // 👥 멤버수 체크용 (프로젝트 상황에 맞게 확인!)
import com.beggar.api.repository.RoomBudgetResultRepository; // 📊 확정 결과 저장용
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
     * 💰 1. 본인 예산 제출 (INSERT or UPDATE)
     */
    @Transactional
    public void submitBudget(Long userNo, Long roomNo, Integer budgetAmount) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        // 방 상태 검증
        if (room.getStatus() != RoomStatus.BUDGET_INPUT) {
            throw new IllegalArgumentException("현재는 예산을 제출할 수 있는 상태가 아닙니다.");
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
     *  2. 강제 확정 / 자동 확정 처리
     */
    @Transactional
    public void confirmBudget(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

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

        // 2. 📊 결과 테이블 기록
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

    /**
     *  3. 확정된 예산 결과 조회
     */
    public BudgetResultResponse getResult(Long roomNo) {
        RoomBudgetResult roomBudgetResult = roomBudgetResultRepository.findByRoom_RoomNo(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("아직 예산이 확정되지 않은 방입니다."));

        return BudgetResultResponse.from(roomBudgetResult);
    }
}
