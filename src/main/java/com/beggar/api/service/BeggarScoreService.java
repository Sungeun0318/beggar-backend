package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.ranking.BeggarScoreResponse;
import com.beggar.api.dto.ranking.RankingEntryResponse;
import com.beggar.api.entity.Budget;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomBeggarScore;
import com.beggar.api.entity.RoomBudgetResult;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional
public class BeggarScoreService {

    // 방별 거지력 점수 산식 (0~100):
    //   score = 절약률 × 예산준수율 × 활동 성숙도 × 멤버수 × 착한가격업소 이용 보너스
    //   활동 성숙도 = min(1.0, 영수증 수 / 20.0) ^ 2
    //   착한가격업소 이용 보너스 = 미이용 1.0 / 이용 1.2
    //
    // 예산을 한두 번만 적게 쓰고 칭호가 바로 오르는 것을 막기 위해 초반 영수증은 낮게 반영하고,
    // 최소 20건의 지출 흐름을 본 뒤 원점수가 온전히 반영되도록 한다.
    //
    // 칭호 5단계: 아기 거지(0-19) / 성장하는 거지(20-39) / 알뜰한 거지(40-59)
    //              / 프로 거지(60-79) / 전설의 거지(80-100)
    //
    // 트리거: 영수증 INSERT/UPDATE/DELETE, OCR 반영, 예산 확정
    private static final BigDecimal FULL_SCORE_RECEIPT_COUNT = BigDecimal.valueOf(20);
    private static final BigDecimal MIN_ACTIVITY_MULTIPLIER = BigDecimal.valueOf(0.5);
    private static final BigDecimal DEFAULT_GOOD_PRICE_MULTIPLIER = BigDecimal.ONE;
    private static final BigDecimal GOOD_PRICE_USED_MULTIPLIER = BigDecimal.valueOf(1.2);

    private final RoomRepository roomRepository;
    private final RoomBudgetResultRepository roomBudgetResultRepository;
    private final ReceiptRepository receiptRepository;
    private final RoomBeggarScoreRepository roomBeggarScoreRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final BudgetRepository budgetRepository;

    @Transactional
    public BeggarScoreResponse getRoomScore(Long roomNo) {
        return BeggarScoreResponse.from(recalculate(roomNo));
    }

    @Transactional
    public List<RankingEntryResponse> getRoomRanking(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        AtomicInteger rank = new AtomicInteger(1);

        roomBeggarScoreRepository.findAll()
                .forEach(score -> recalculate(score.getRoom().getRoomNo()));

        return roomBeggarScoreRepository.findTopRoomScores(PageRequest.of(0, safeLimit)).stream()
                .map(score -> RankingEntryResponse.of(rank.getAndIncrement(), score))
                .toList();
    }

    @Transactional
    public UserScoreResult getUserScore(Long userNo) {
        // 유저가 참여 중인 모든 방의 점수를 최신 산식으로 재계산한 뒤 평균을 계산한다.
        // 기존 room_beggar_scores 저장값만 평균 내면 산식 변경 후에도 과거 점수가 계속 보인다.
        List<Long> roomNos = roomMemberRepository.findByUser_UserNoAndStatus(userNo, RoomMember.Status.ACTIVE)
                .stream()
                .map(rm -> rm.getRoom().getRoomNo())
                .toList();

        if (roomNos.isEmpty()) {
            return new UserScoreResult(0, RoomBeggarScore.resolveTitle(0));
        }

        List<RoomBeggarScore> roomScores = roomNos.stream()
                .map(this::recalculate)
                .toList();

        double avgScore = roomScores.stream()
                .mapToInt(RoomBeggarScore::getScore)
                .average()
                .orElse(0.0);

        int finalScore = (int) Math.round(avgScore);
        return new UserScoreResult(finalScore, RoomBeggarScore.resolveTitle(finalScore));
    }

    @Transactional
    public RoomBeggarScore recalculate(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        RoomBeggarScore score = roomBeggarScoreRepository.findByRoom_RoomNo(roomNo)
                .orElseGet(() -> new RoomBeggarScore(room));

        ScoreSnapshot snapshot = roomBudgetResultRepository.findByRoom_RoomNo(roomNo)
                .map(this::calculate)
                .orElseGet(() -> calculateWithoutBudgetResult(room));
        score.update(
                snapshot.score(),
                snapshot.totalSpentAmount(),
                snapshot.totalSavedAmount(),
                snapshot.goodPriceVerifiedCount(),
                snapshot.budgetComplianceRate(),
                snapshot.avgSavingsRatio()
        );
        return roomBeggarScoreRepository.save(score);
    }

    private ScoreSnapshot calculateWithoutBudgetResult(Room room) {
        Long roomNo = room.getRoomNo();
        List<Budget> budgets = budgetRepository.findByRoomNo(roomNo);

        if (!budgets.isEmpty()) {
            int minBudget = budgets.stream()
                    .map(Budget::getAmount)
                    .filter(amount -> amount != null && amount > 0)
                    .min(Integer::compareTo)
                    .orElse(0);
            return calculate(roomNo, (long) minBudget * budgets.size(), budgets.size());
        }

        long totalBudget = Math.max(0L, room.getTotalBudget() == null ? 0L : room.getTotalBudget());
        int memberCount = Math.toIntExact(Math.max(
                1L,
                roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, RoomMember.Status.ACTIVE)
        ));
        return calculate(roomNo, totalBudget, memberCount);
    }

    private ScoreSnapshot calculate(RoomBudgetResult budgetResult) {
        Long roomNo = budgetResult.getRoom().getRoomNo();
        long totalBudget = Math.max(0L, budgetResult.getTotalBudget());
        int memberCount = Math.max(1, budgetResult.getMemberCount());

        return calculate(roomNo, totalBudget, memberCount);
    }

    private ScoreSnapshot calculate(Long roomNo, long totalBudget, int memberCount) {
        System.out.println("roomNo = " + roomNo + ", totalBudget = " + totalBudget + ", memberCount = " + memberCount);

        long totalSpent = Math.max(0L, receiptRepository.sumAmountByRoomNo(roomNo));
        System.out.println("totalSpent = " + totalSpent);

        long totalSaved = Math.max(0L, totalBudget - totalSpent);
        System.out.println("totalSaved = " + totalSaved);

        int verifiedCount = Math.toIntExact(receiptRepository.countGoodPriceMatchedByRoomNo(roomNo));
        System.out.println("verifiedCount = " + verifiedCount);

        long receiptCount = receiptRepository.countByRoom_RoomNo(roomNo);
        System.out.println("receiptCount = " + receiptCount);

        BigDecimal complianceRate = calculateBudgetComplianceRate(totalBudget, totalSpent);
        BigDecimal savingsRatio = calculateSavingsRatio(totalBudget, totalSaved);
        BigDecimal activityMaturity = calculateActivityMaturity(receiptCount);
        BigDecimal memberMultiplier = BigDecimal.valueOf(memberCount);
        BigDecimal goodPriceMultiplier = calculateGoodPriceMultiplier(verifiedCount);

        BigDecimal budgetComplianceMultiplier = complianceRate
                .divide(percent(100), 2, RoundingMode.HALF_UP);

        int finalScore = savingsRatio
                .multiply(budgetComplianceMultiplier)
                .multiply(activityMaturity)
                .multiply(memberMultiplier)
                .multiply(goodPriceMultiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        return new ScoreSnapshot(
                clamp(finalScore),
                totalSpent,
                totalSaved,
                verifiedCount,
                complianceRate,
                savingsRatio
        );
    }

    private BigDecimal calculateBudgetComplianceRate(long totalBudget, long totalSpent) {
        if (totalBudget <= 0L) {
            return percent(0);
        }
        if (totalSpent >= totalBudget) {
            return percent(0);
        }
        return BigDecimal.valueOf(totalBudget - totalSpent)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBudget), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSavingsRatio(long totalBudget, long totalSaved) {

        System.out.println("totalBudget = " + totalBudget + ", totalSaved = " + totalSaved);



        if (totalBudget <= 0L) {
            return percent(0);
        }
        return BigDecimal.valueOf(totalSaved)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBudget), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateActivityMaturity(long receiptCount) {


        if (receiptCount <= 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal receiptProgress = BigDecimal.valueOf(receiptCount)
                .divide(FULL_SCORE_RECEIPT_COUNT, 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);

        return MIN_ACTIVITY_MULTIPLIER
                .add(BigDecimal.ONE.subtract(MIN_ACTIVITY_MULTIPLIER).multiply(receiptProgress))
                .min(BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGoodPriceMultiplier(int verifiedCount) {
        return verifiedCount > 0 ? GOOD_PRICE_USED_MULTIPLIER : DEFAULT_GOOD_PRICE_MULTIPLIER;
    }

    private BigDecimal percent(int value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private record ScoreSnapshot(
            int score,
            long totalSpentAmount,
            long totalSavedAmount,
            int goodPriceVerifiedCount,
            BigDecimal budgetComplianceRate,
            BigDecimal avgSavingsRatio
    ) {
    }

    public record UserScoreResult(int score, String title) {
    }
}
