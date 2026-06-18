package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.receipt.MyReceiptHistoryResponse;
import com.beggar.api.dto.receipt.ReceiptCreateRequest;
import com.beggar.api.dto.receipt.ReceiptResponse;
import com.beggar.api.dto.receipt.ReceiptUpdateRequest;
import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.ReceiptSplit;
import com.beggar.api.entity.ReceiptSplitGroup;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.ReceiptSplitGroupRepository;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
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
    private final ReceiptSplitGroupRepository receiptSplitGroupRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final GoodPriceMatchService goodPriceMatchService;
    private final LocationService locationService;
    private final OcrService ocrService;
    private final S3Service s3Service;
    private final BeggarScoreService beggarScoreService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ReceiptService self;

    @Transactional
    public ReceiptResponse create(Long roomNo, Long userNo, ReceiptCreateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

        RoomMember uploader = roomMemberRepository
                .findByRoom_RoomNoAndUser_UserNo(roomNo, userNo)
                .orElseThrow(() -> new IllegalArgumentException("방 멤버만 영수증을 등록할 수 있습니다."));
        if (uploader.getStatus() != RoomMember.Status.ACTIVE) {
            throw new IllegalArgumentException("활성 방 멤버만 영수증을 등록할 수 있습니다.");
        }

        ReceiptSplitGroup splitGroup = resolveSplitGroup(roomNo, request);

        BigDecimal lat = request.centerLat();
        BigDecimal lng = request.centerLng();
        String storeName = request.storeName();
        String address = request.address();

        if (splitGroup != null) {
            storeName = splitGroup.getStoreName();
            address = splitGroup.getAddress();
            lat = splitGroup.getCenterLat();
            lng = splitGroup.getCenterLng();
        }

        if ((lat == null || lng == null) && address != null && !address.isBlank()) {
            var resolved = locationService.resolveAddress(address);
            if (resolved.isPresent()) {
                lat = BigDecimal.valueOf(resolved.get().lat());
                lng = BigDecimal.valueOf(resolved.get().lng());
            }
        }

        boolean confirmed = true;
        if (request.receiptType() == Receipt.ReceiptType.COMBINED && request.inputMethod() != Receipt.InputMethod.MANUAL) {
            confirmed = false;
        }

        Receipt receipt = Receipt.builder()
                .room(room)
                .uploader(uploader)
                .receiptType(request.receiptType())
                .inputMethod(request.inputMethod())
                .imageUrl(request.imageUrl())
                .storeName(storeName)
                .totalAmount(request.totalAmount())
                .amount(request.amount())
                .receiptIssuedAt(request.receiptIssuedAt())
                .address(address)
                .centerLat(lat)
                .centerLng(lng)
                .splitGroup(splitGroup)
                .confirmed(confirmed)
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
            self.processOcrAsync(roomNo, saved);
        }

        beggarScoreService.recalculate(roomNo);
        return toResponse(saved);
    }

    public void processOcrAsync(Long roomNo, Receipt receipt) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
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
                Integer totalAmount = parseTotalAmount(data.get("total_amount"));
                LocalDateTime receiptIssuedAt = parseReceiptIssuedAt(data.get("date"));

                if (address != null) {
                    address = address.split("\\(")[0].trim();
                }

                self.updateOcrResultInternal(roomNo, receipt.getReceiptId(), storeName, totalAmount, receiptIssuedAt, address);

            } catch (Exception e) {
                log.error("비동기 OCR 처리 중 오류 발생: {}", receipt.getReceiptId(), e);
                self.updateOcrStatusInternal(receipt.getReceiptId(), Receipt.OcrStatus.FAILED);
            }
        });
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
    public void updateOcrResultInternal(Long roomNo, Long receiptId, String storeName, Integer totalAmount,
                                        LocalDateTime receiptIssuedAt, String address) {
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

        receipt.applyOcrResult(storeName, totalAmount, receiptIssuedAt, address, lat, lng);
        applyGoodPriceMatch(receipt);
        beggarScoreService.recalculate(roomNo);
    }

    public List<ReceiptResponse> read(Long roomNo) {
        return receiptRepository.findAllByRoom_RoomNoOrderByCreatedAtDesc(roomNo).stream()
                .filter(Receipt::getConfirmed)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MyReceiptHistoryResponse readMyReceiptHistory(Long userNo) {
        List<Long> roomNos = roomMemberRepository.findByUser_UserNoAndStatus(userNo, RoomMember.Status.ACTIVE)
                .stream()
                .map(member -> member.getRoom().getRoomNo())
                .distinct()
                .toList();

        if (roomNos.isEmpty()) {
            return MyReceiptHistoryResponse.empty();
        }

        List<Receipt> receipts = receiptRepository.findAllByRoom_RoomNoInAndReceiptTypeInOrderByCreatedAtDesc(
                roomNos,
                EnumSet.of(Receipt.ReceiptType.COMBINED, Receipt.ReceiptType.SPLIT)
        ).stream()
                .filter(Receipt::getConfirmed)
                .toList();

        return MyReceiptHistoryResponse.from(receipts);
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
        receipt.updateManualInfo(
                request.storeName(),
                request.receiptIssuedAt(),
                request.address(),
                request.centerLat(),
                request.centerLng()
        );
        receipt.confirm();

        if (receipt.getReceiptType() == Receipt.ReceiptType.SPLIT && request.splits() != null) {
            receiptSplitRepository.deleteAllByReceipt_ReceiptId(receipt.getReceiptId());
            receipt.getSplits().clear();

            request.splits().forEach(splitItem -> {
                RoomMember splitMember = roomMemberRepository.findById(splitItem.roomMemberId())
                        .orElseThrow(() -> new IllegalArgumentException("분할 대상 멤버를 찾을 수 없습니다. ID: " + splitItem.roomMemberId()));
                
                ReceiptSplit split = ReceiptSplit.builder()
                        .receipt(receipt)
                        .roomMember(splitMember)
                        .amount(splitItem.amount())
                        .build();
                
                receiptSplitRepository.save(split);
                receipt.addSplit(split);
            });
        }

        applyGoodPriceMatch(receipt);

        beggarScoreService.recalculate(roomNo);
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
                request.storeName(), request.totalAmount(), request.receiptIssuedAt(),
                request.address(), lat, lng
        );
        applyGoodPriceMatch(receipt);

        beggarScoreService.recalculate(roomNo);
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
                original.receiptIssuedAt(),
                original.address(),
                original.centerLat(),
                original.centerLng(),
                original.splitGroupId(),
                original.goodPriceMatched(),
                original.goodPriceStoreId(),
                original.goodPriceStoreName(),
                original.goodPriceStoreAddress(),
                original.goodPriceVerifiedAt(),
                original.confirmed(),
                original.createdAt(),
                original.updatedAt(),
                original.splits()
        );
    }

    private ReceiptSplitGroup resolveSplitGroup(Long roomNo, ReceiptCreateRequest request) {
        if (request.splitGroupId() == null) {
            return null;
        }
        if (request.receiptType() != Receipt.ReceiptType.SPLIT) {
            throw new IllegalArgumentException("분할 그룹에는 SPLIT 영수증만 등록할 수 있습니다.");
        }

        ReceiptSplitGroup group = receiptSplitGroupRepository.findById(request.splitGroupId())
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("분할 그룹을 찾을 수 없습니다. ID: " + request.splitGroupId()));

        if (group.getStatus() != ReceiptSplitGroup.SplitGroupStatus.OPEN) {
            throw new IllegalArgumentException("닫힌 분할 그룹에는 영수증을 추가할 수 없습니다.");
        }
        return group;
    }

    @Transactional
    public void delete(Long roomNo, Long userNo, Long receiptId) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));

        if (!receipt.getUploader().getUser().getUserNo().equals(userNo)) {
            throw new IllegalArgumentException("본인이 등록한 영수증만 삭제할 수 있습니다.");
        }

        receiptRepository.delete(receipt);
        beggarScoreService.recalculate(roomNo);
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

    private LocalDateTime parseReceiptIssuedAt(Object rawDate) {
        if (rawDate == null) {
            return null;
        }

        String value = String.valueOf(rawDate).trim();
        if (value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷으로 시도한다.
            }
        }

        log.warn("영수증 발행시간 파싱 실패. rawDate={}", rawDate);
        return null;
    }

    private Integer parseTotalAmount(Object rawAmount) {
        if (rawAmount == null) {
            return null;
        }
        if (rawAmount instanceof Number number) {
            return number.intValue();
        }

        String value = String.valueOf(rawAmount).replaceAll("[^0-9]", "");
        if (value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("영수증 총액 파싱 실패. rawAmount={}", rawAmount);
            return null;
        }
    }
}
