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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final ReceiptSplitRepository receiptSplitRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    // TODO: create(roomNo, userNo, request)        — 통합/분할 영수증 등록
    //                                                CAMERA/GALLERY는 OCR PENDING, MANUAL은 OCR MANUAL
    //                                                SPLIT이면 receipt_splits까지 저장
    //                                                동일 트랜잭션 내 room_beggar_scores 재계산
    // TODO: updateAmount(roomNo, userNo, receiptId, req) — 수동 금액 보정 → 방 점수 재계산
    // TODO: listByRoom(roomNo)                     — 방별 영수증 (최신순)
    // TODO: applyOcrResult(receiptId, payload)     — OCR 콜백 (AI 서버 → 결과 반영)

    @Transactional
    public ReceiptResponse create(Long roomNo, ReceiptCreateRequest request) {
        Room room = roomRepository.getReferenceById(roomNo);
        RoomMember uploader = roomMemberRepository
                .findByRoom_RoomNoAndUser_UserNo(roomNo, request.uploaderUserNo())
                .orElseThrow(() -> new IllegalArgumentException("방 멤버만 영수증을 등록할 수 있습니다."));

        Receipt receipt = Receipt.builder()
                .room(room)
                .uploader(uploader)
                .receiptType(request.receiptType())
                .inputMethod(request.inputMethod())
                .imageUrl(request.imageUrl())
                .ocrStatus(null)
                .storeName(request.storeName())
                .totalAmount(request.totalAmount())
                .amount(request.amount())
                .address(request.address())
                .centerLat(request.centerLat())
                .centerLng(request.centerLng())
                .build();

        Receipt saved = receiptRepository.save(receipt);

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

        return ReceiptResponse.from(saved);
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
        receipt.applyOcrResult(
                request.storeName(), request.totalAmount(),
                request.address(), request.centerLat(), request.centerLng()
        );
        return ReceiptResponse.from(receipt);
    }

    @Transactional
    public void delete(Long roomNo, Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .filter(found -> found.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));
        receiptRepository.delete(receipt);
    }
}
