package com.beggar.api.service;

import com.beggar.api.entity.Budget;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomBudgetResult;
import com.beggar.api.entity.RoomMember;
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
    private final RoomMemberRepository roomMemberRepository; // 👥 추가
    private final RoomBudgetResultRepository roomBudgetResultRepository; // 📊 추가

    /**
     * 💰 1. 본인 예산 제출 (INSERT or UPDATE)
     * [테스트 프리패스 버전] 내가 제출하면 묻지도 따지지도 않고 무조건 자동 확정!
     */
    @Transactional
    public void submitBudget(Long userNo, Long roomNo, Integer budgetAmount) {
        // 기존에 이 방에 제출한 예산이 있는지 조회
        Budget budget = budgetRepository.findByRoomNoAndUserNo(roomNo, userNo)
                .orElse(null);

        if (budget == null) {
            budget = new Budget(roomNo, userNo, budgetAmount);
        } else {
            budget.updateAmount(budgetAmount);
        }
        budgetRepository.save(budget);

        System.out.println("💰 예산 제출 임시 성공! 방 번호: " + roomNo + ", 금액: " + budgetAmount);

        // [치트키 방어막] 아직 RoomMember 로직이 연동되지 않아 totalMembers가 0이어도
        // 혼자 테스트할 때는 제출 즉시 무조건 강제 확정되도록 예외 필터를 뚫어버림
        long totalMembers = roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, RoomMember.Status.ACTIVE);

        if (totalMembers == 0) {
            System.out.println("⚠️ 멤버 데이터가 없으므로 1인 테스트 모드로 강제 확정합니다!");
            this.confirmBudget(roomNo);
        } else {
            long submittedCount = budgetRepository.countByRoomNo(roomNo);
            if (totalMembers == submittedCount) {
                this.confirmBudget(roomNo);
            }
        }
    }

    /**
     *  2. 강제 확정 / 자동 확정 처리
     * rooms 테이블 동기화뿐만 아니라 RoomBudgetResult(확정 테이블) 생성까지 완료
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

        // 1. rooms 테이블 총예산 동기화
        room.updateTotalBudget(totalBudget);
        roomRepository.save(room);

        // 2. 📊 RoomBudgetResult 엔티티 생성 후 기록 저장!
        RoomBudgetResult result = RoomBudgetResult.builder()
                .room(room)
                .minBudgetPerPerson(minAmount)
                .memberCount(memberCount)
                .totalBudget(totalBudget)
                .confirmedAt(LocalDateTime.now())
                .build();
        roomBudgetResultRepository.save(result);

        // 3. TODO: 거지력/거지등급 점수 재계산 로직이 필요하다면 여기에 트리거
        // beggarScoreService.recalculate(roomNo, minAmount);

        System.out.println("거지방 [" + roomNo + "] 예산 최종 확정 완료 및 결과 테이블 기록 끝!");
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
