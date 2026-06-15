package com.beggar.api.service.admin;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.admin.ai.BudgetRiskPredictionRequest;
import com.beggar.api.dto.admin.ai.BudgetRiskPredictionResponse;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiInsightService {

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

    public BudgetRiskPredictionResponse getBudgetRiskPredictions() {
        BudgetRiskPredictionRequest request = new BudgetRiskPredictionRequest(
                buildRiskRooms(),
                buildRiskReceipts(),
                buildRiskBudgets()
        );

        try {
            return aiServerWebClient.post()
                    .uri("/api/v1/predictions/budget-risk")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BudgetRiskPredictionResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new CustomException(
                    ErrorCode.EXTERNAL_API_FAILED,
                    "AI 예산 위험 예측 API 호출에 실패했습니다. status=" + e.getStatusCode().value()
            );
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED, "AI 예산 위험 예측 API 호출에 실패했습니다.");
        }
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
