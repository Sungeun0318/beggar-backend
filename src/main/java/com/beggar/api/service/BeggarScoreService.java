package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.ranking.BeggarScoreResponse;
import com.beggar.api.dto.ranking.RankingEntryResponse;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomBeggarScore;
import com.beggar.api.entity.RoomBudgetResult;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomBeggarScoreRepository;
import com.beggar.api.repository.RoomBudgetResultRepository;
import com.beggar.api.repository.RoomRepository;
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
@Transactional(readOnly = true)
public class BeggarScoreService {

    // 방별 거지력 점수 산식 (0~100):
    //   score = 예산준수율 × 0.35 + 절약률 × 0.35 + 착한가격업소 인증 점수 × 0.30
    //
    // 칭호 5단계: 아기 거지(0-19) / 성장하는 거지(20-39) / 알뜰한 거지(40-59)
    //              / 프로 거지(60-79) / 전설의 거지(80-100)
    //
    // 트리거: 영수증 INSERT/UPDATE/DELETE, OCR 반영, 예산 확정
    private static final BigDecimal BUDGET_COMPLIANCE_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal SAVINGS_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal GOOD_PRICE_WEIGHT = BigDecimal.valueOf(0.30);
    private static final int GOOD_PRICE_SCORE_PER_RECEIPT = 20;

    private final RoomRepository roomRepository;
    private final RoomBudgetResultRepository roomBudgetResultRepository;
    private final ReceiptRepository receiptRepository;
    private final RoomBeggarScoreRepository roomBeggarScoreRepository;

    @Transactional
    public BeggarScoreResponse getRoomScore(Long roomNo) {
        return BeggarScoreResponse.from(recalculate(roomNo));
    }

    public List<RankingEntryResponse> getRoomRanking(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        AtomicInteger rank = new AtomicInteger(1);

        return roomBeggarScoreRepository.findTopRoomScores(PageRequest.of(0, safeLimit)).stream()
                .map(score -> RankingEntryResponse.of(rank.getAndIncrement(), score))
                .toList();
    }

    @Transactional
    public RoomBeggarScore recalculate(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        RoomBeggarScore score = roomBeggarScoreRepository.findByRoom_RoomNo(roomNo)
                .orElseGet(() -> new RoomBeggarScore(room));

        var budgetResult = roomBudgetResultRepository.findByRoom_RoomNo(roomNo);
        if (budgetResult.isEmpty()) {
            score.update(0, receiptRepository.sumAmountByRoomNo(roomNo), 0L, 0, percent(0), percent(0));
            return roomBeggarScoreRepository.save(score);
        }

        ScoreSnapshot snapshot = calculate(budgetResult.get());
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

    private ScoreSnapshot calculate(RoomBudgetResult budgetResult) {
        Long roomNo = budgetResult.getRoom().getRoomNo();
        long totalBudget = Math.max(0L, budgetResult.getTotalBudget());
        long totalSpent = Math.max(0L, receiptRepository.sumAmountByRoomNo(roomNo));
        long totalSaved = Math.max(0L, totalBudget - totalSpent);
        int verifiedCount = Math.toIntExact(receiptRepository.countGoodPriceMatchedByRoomNo(roomNo));

        BigDecimal complianceRate = calculateBudgetComplianceRate(totalBudget, totalSpent);
        BigDecimal savingsRatio = calculateSavingsRatio(totalBudget, totalSaved);
        BigDecimal goodPriceScore = percent(Math.min(100, verifiedCount * GOOD_PRICE_SCORE_PER_RECEIPT));

        int finalScore = complianceRate.multiply(BUDGET_COMPLIANCE_WEIGHT)
                .add(savingsRatio.multiply(SAVINGS_WEIGHT))
                .add(goodPriceScore.multiply(GOOD_PRICE_WEIGHT))
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
        if (totalSpent <= totalBudget) {
            return percent(100);
        }
        return BigDecimal.valueOf(totalBudget)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSpent), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSavingsRatio(long totalBudget, long totalSaved) {
        if (totalBudget <= 0L) {
            return percent(0);
        }
        return BigDecimal.valueOf(totalSaved)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBudget), 2, RoundingMode.HALF_UP);
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
}
