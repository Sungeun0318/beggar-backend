package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.receipt.ReceiptCreateRequest;
import com.beggar.api.dto.receipt.ReceiptResponse;
import com.beggar.api.dto.receipt.ReceiptUpdateRequest;
import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.ReceiptSplit;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.ReceiptSplitRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
    private final OcrService ocrService;
    private final S3Service s3Service;
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ReceiptService self;

    @Transactional
    public ReceiptResponse create(Long roomNo, ReceiptCreateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

        RoomMember uploader = roomMemberRepository
                .findByRoom_RoomNoAndUser_UserNo(roomNo, request.uploaderUserNo())
                .orElseThrow(() -> new IllegalArgumentException("방 멤버만 영수증을 등록할 수 있습니다."));

        BigDecimal lat = request.centerLat();
        BigDecimal lng = request.centerLng();

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
            request.splits().forEach(splitItem -> {
                RoomMember splitMember = roomMemberRepository.findById(splitItem.roomMemberId())
                        .orElseThrow(() -> new IllegalArgumentException("분할 대상 멤버를 찾을 수 없습니다. ID: " + splitItem.roomMemberId()));
                
                ReceiptSplit split = ReceiptSplit.builder()
                        .receipt(saved)
                        .roomMember(splitMember)
                        .amount(splitItem.amount())
                        .build();
                
                receiptSplitRepository.save(split);
                saved.addSplit(split);
            });
        }

        if (request.inputMethod() != Receipt.InputMethod.MANUAL) {
            processOcrAsync(roomNo, saved);
        }

        return toResponse(saved);
    }

    @Async
    public void processOcrAsync(Long roomNo, Receipt receipt) {
        try {
            String encodedKey = receipt.getImageUrl().substring(receipt.getImageUrl().lastIndexOf("/") + 1);
            String key = java.net.URLDecoder.decode(encodedKey, java.nio.charset.StandardCharsets.UTF_8);
            byte[] imageBytes = s3Service.getFileBytes(key);
            
            String allText = ocrService.detectTextFromBytes(imageBytes);
            if (allText == null || allText.isBlank()) {
                log.error("OCR 텍스트 추출 실패: {}", receipt.getReceiptId());
                self.updateOcrStatusInternal(receipt.getReceiptId(), Receipt.OcrStatus.FAILED);
                return;
            }

            Map<String, Object> data = ocrService.analyzeWithGroq(allText);
            if (data == null) {
                log.error("Groq 분석 실패: {}", receipt.getReceiptId());
                self.updateOcrStatusInternal(receipt.getReceiptId(), Receipt.OcrStatus.FAILED);
                return;
            }

            String storeName = (String) data.get("store_name");
            String address = (String) data.get("address");
            Object totalAmountObj = data.get("total_amount");
            Integer totalAmount = totalAmountObj instanceof Integer ? (Integer) totalAmountObj : ((Double) totalAmountObj).intValue();

            if (address != null) {
                address = address.split("\\(")[0].trim();
            }

            self.updateOcrResultInternal(roomNo, receipt.getReceiptId(), storeName, totalAmount, address);

        } catch (Exception e) {
            log.error("비동기 OCR 처리 중 오류 발생: {}", receipt.getReceiptId(), e);
            self.updateOcrStatusInternal(receipt.getReceiptId(), Receipt.OcrStatus.FAILED);
        }
    }

    @Transactional
    public void updateOcrStatusInternal(Long receiptId, Receipt.OcrStatus status) {
        receiptRepository.findById(receiptId).ifPresent(r -> {
            if (status == Receipt.OcrStatus.FAILED) {
                r.markOcrFailed();
            }
        });
    }

    @Transactional
    public void updateOcrResultInternal(Long roomNo, Long receiptId, String storeName, Integer totalAmount, String address) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));

        BigDecimal lat = null;
        BigDecimal lng = null;

        if (address != null && !address.isBlank()) {
            var resolved = locationService.resolveAddress(address);
            if (resolved.isPresent()) {
                lat = BigDecimal.valueOf(resolved.get().lat());
                lng = BigDecimal.valueOf(resolved.get().lng());
            }
        }

        receipt.applyOcrResult(storeName, totalAmount, address, lat, lng);
        applyGoodPriceMatch(receipt);
    }

    public List<ReceiptResponse> read(Long roomNo) {
        return receiptRepository.findAllByRoom_RoomNoOrderByCreatedAtDesc(roomNo).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReceiptResponse readOne(Long roomNo, Long receiptId) {
        return receiptRepository.findById(receiptId)
                .filter(receipt -> receipt.getRoom().getRoomNo().equals(roomNo))
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));
    }

    @Transactional
    public ReceiptResponse updateAmount(Long roomNo, Long receiptId, ReceiptUpdateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));
        
        receipt.updateAmount(request.amount());
        receipt.updateManualInfo(request.storeName(), request.address(), request.centerLat(), request.centerLng());

        applyGoodPriceMatch(receipt);

        return toResponse(receipt);
    }

    @Transactional
    public ReceiptResponse applyOcrResult(Long roomNo, Long receiptId, ReceiptCreateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

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

        return toResponse(receipt);
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        String presignedUrl = s3Service.generatePresignedGetUrl(receipt.getImageUrl());
        ReceiptResponse original = ReceiptResponse.from(receipt);
        
        return new ReceiptResponse(
                original.receiptId(),
                original.roomNo(),
                original.uploaderUserNo(),
                original.receiptType(),
                original.inputMethod(),
                presignedUrl,
                original.ocrStatus(),
                original.storeName(),
                original.totalAmount(),
                original.amount(),
                original.address(),
                original.centerLat(),
                original.centerLng(),
                original.goodPriceMatched(),
                original.goodPriceStoreId(),
                original.goodPriceStoreName(),
                original.goodPriceStoreAddress(),
                original.goodPriceVerifiedAt(),
                original.createdAt(),
                original.updatedAt(),
                original.splits()
        );
    }

    @Transactional
    public void delete(Long roomNo, Long receiptId) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

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
