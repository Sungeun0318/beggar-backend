package com.beggar.api.service;

import com.beggar.api.dto.receipt.ReceiptCreateRequest;
import com.beggar.api.dto.receipt.ReceiptResponse;
import com.beggar.api.dto.receipt.ReceiptUpdateRequest;
import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.ReceiptSplit;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.ReceiptSplitRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final ReceiptSplitRepository receiptSplitRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final GoodPriceMatchService goodPriceMatchService;
    private final LocationService locationService;
    private final BeggarScoreService beggarScoreService;
    private final WebClient aiServerWebClient;

    // TODO: applyOcrResult(receiptId, payload)     — OCR 콜백 (AI 서버 → 결과 반영)

    @Transactional
    public ReceiptResponse create(Long roomNo, ReceiptCreateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다. ID: " + roomNo));
        RoomMember uploader = roomMemberRepository
                .findByRoom_RoomNoAndUser_UserNo(roomNo, request.uploaderUserNo())
                .orElseThrow(() -> new IllegalArgumentException("방 멤버만 영수증을 등록할 수 있습니다."));

        BigDecimal lat = request.centerLat();
        BigDecimal lng = request.centerLng();

        // 주소는 있는데 좌표가 없는 경우 자동 변환
        if ((lat == null || lng == null) && request.address() != null && !request.address().isBlank()) {
            var resolved = locationService.resolveAddress(request.address());
            if (resolved.isPresent()) {
                lat = BigDecimal.valueOf(resolved.get().lat());
                lng = BigDecimal.valueOf(resolved.get().lng());
            }
        }

        Receipt receipt = Receipt.builder()
                .room(room)
                .uploader(uploader)
                .receiptType(request.receiptType())
                .inputMethod(request.inputMethod())
                .imageUrl(request.imageUrl())
                .storeName(request.storeName())
                .totalAmount(request.totalAmount())
                .amount(request.amount())
                .address(request.address())
                .centerLat(lat)
                .centerLng(lng)
                .build();

        Receipt saved = receiptRepository.save(receipt);
        applyGoodPriceMatch(saved);

        if (request.receiptType() == Receipt.ReceiptType.SPLIT && request.splits() != null) {
            request.splits().forEach(split -> {
                RoomMember splitMember = roomMemberRepository.getReferenceById(split.roomMemberId());
                receiptSplitRepository.save(ReceiptSplit.builder()
                        .receipt(saved)
                        .roomMember(splitMember)
                        .amount(split.amount())
                        .build());
            });
        }

        // 카메라/갤러리 입력인 경우 파이썬 OCR 서버 호출
        if (request.inputMethod() != Receipt.InputMethod.MANUAL) {
            triggerOcr(roomNo, saved);
        }

        return ReceiptResponse.from(saved);
    }

    private void triggerOcr(Long roomNo, Receipt receipt) {
        Map<String, Object> body = Map.of(
                "room_no", roomNo,
                "receipt_id", receipt.getReceiptId(),
                "image_url", receipt.getImageUrl()
        );

        aiServerWebClient.post()
                .uri("/api/v1/ocr")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
    }

    public List<ReceiptResponse> read(Long roomNo) {
        return receiptRepository.findAllByRoom_RoomNoOrderByCreatedAtDesc(roomNo).stream()
                .map(ReceiptResponse::from)
                .collect(Collectors.toList());
    }

    public ReceiptResponse readOne(Long roomNo, Long receiptId) {
        return receiptRepository.findById(receiptId)
                .filter(receipt -> receipt.getRoom().getRoomNo().equals(roomNo))
                .map(ReceiptResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));
    }

    @Transactional
    public ReceiptResponse updateAmount(Long roomNo, Long receiptId, ReceiptUpdateRequest request) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));
        receipt.updateAmount(request.amount());

        return ReceiptResponse.from(receipt);
    }

    @Transactional
    public ReceiptResponse applyOcrResult(Long roomNo, Long receiptId, ReceiptCreateRequest request) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));

        BigDecimal lat = request.centerLat();
        BigDecimal lng = request.centerLng();

        if ((lat == null || lng == null) && request.address() != null && !request.address().isBlank()) {
            var resolved = locationService.resolveAddress(request.address());
            if (resolved.isPresent()) {
                lat = BigDecimal.valueOf(resolved.get().lat());
                lng = BigDecimal.valueOf(resolved.get().lng());
            }
        }

        receipt.applyOcrResult(
                request.storeName(), request.totalAmount(),
                request.address(), lat, lng
        );
        applyGoodPriceMatch(receipt);

        return ReceiptResponse.from(receipt);
    }

    @Transactional
    public void delete(Long roomNo, Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));
        receiptRepository.delete(receipt);
    }

    private void applyGoodPriceMatch(Receipt receipt) {
        goodPriceMatchService.match(receipt.getStoreName(), receipt.getAddress())
                .ifPresentOrElse(
                        store -> receipt.applyGoodPriceMatch(
                                store.storeId(),
                                store.name(),
                                store.address(),
                                LocalDateTime.now()
                        ),
                        receipt::clearGoodPriceMatch
                );
    }
}
