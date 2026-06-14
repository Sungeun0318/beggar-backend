package com.beggar.api.service;

import com.beggar.api.dto.receipt.ReceiptDuplicateCheckRequest;
import com.beggar.api.dto.receipt.ReceiptDuplicateCheckResponse;
import com.beggar.api.entity.Receipt;
import com.beggar.api.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptDuplicateService {

    private static final long DUPLICATE_WINDOW_MINUTES = 30;
    private static final int MAX_CANDIDATES = 5;

    private final ReceiptRepository receiptRepository;

    public ReceiptDuplicateCheckResponse check(Long roomNo, Long userNo, ReceiptDuplicateCheckRequest request) {
        if (request.amount() == null || request.amount() <= 0) {
            return ReceiptDuplicateCheckResponse.empty();
        }

        String storeName = normalize(request.storeName());
        String address = normalize(request.address());
        if (storeName.isBlank() && address.isBlank()) {
            return ReceiptDuplicateCheckResponse.empty();
        }

        LocalDateTime targetTime = request.receiptIssuedAt() == null
                ? LocalDateTime.now()
                : request.receiptIssuedAt();
        LocalDateTime fromTime = targetTime.minusMinutes(DUPLICATE_WINDOW_MINUTES);
        LocalDateTime toTime = targetTime.plusMinutes(DUPLICATE_WINDOW_MINUTES);

        List<Receipt> candidates = receiptRepository.findDuplicateCandidates(
                        roomNo,
                        userNo,
                        request.amount(),
                        storeName,
                        address,
                        fromTime,
                        toTime,
                        request.excludeReceiptId()
                ).stream()
                .limit(MAX_CANDIDATES)
                .toList();

        return ReceiptDuplicateCheckResponse.from(candidates);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
