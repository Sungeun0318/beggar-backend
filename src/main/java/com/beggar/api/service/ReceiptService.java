package com.beggar.api.service;

import com.beggar.api.dto.receipt.ReceiptCreateRequest;
import com.beggar.api.dto.receipt.ReceiptResponse;
import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    // TODO: create(userNo, request)                — 영수증 등록 (ocrStatus = PENDING)
    //                                                동일 트랜잭션 내 거지력 재계산
    // TODO: updateAmount(userNo, receiptId, req)   — 수동 금액 보정 → 거지력 재계산
    // TODO: listByRoom(roomNo)                     — 방별 영수증 (최신순)
    // TODO: applyOcrResult(receiptId, payload)     — OCR 콜백 (AI 서버 → 결과 반영)

    @Transactional
    public Long create(ReceiptCreateRequest request) {
        Room room = roomRepository.getReferenceById(request.roomNo());
        RoomMember uploader = roomMemberRepository.getReferenceById(request.uploaderUserNo());
        Receipt savedEntity = receiptRepository.save(request.toEntity(room, uploader));
        return savedEntity.getReceiptId();
    }

    public List<ReceiptCreateRequest> read() {
        return receiptRepository.findAll().stream()
                .map(ReceiptCreateRequest::from)
                .collect(Collectors.toList());
    }

    public ReceiptCreateRequest readOne(Long receiptId) {
        return receiptRepository.findById(receiptId)
                .map(ReceiptCreateRequest::from)
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없습니다. ID: " + receiptId));
    }

    @Transactional
    public boolean update(Long receiptId, ReceiptCreateRequest request) {
        return receiptRepository.findById(receiptId)
                .map(receipt -> {
                    receipt.applyOcrResult(
                            request.storeName(), request.totalAmount(),
                            request.address(), request.centerLat(), request.centerLng()
                    );
                    return true;
                }).orElse(false);
    }

    @Transactional
    public boolean delete(Long receiptId) {
        if (receiptRepository.existsById(receiptId)) {
            receiptRepository.deleteById(receiptId);
            return true;
        }
        return false;
    }
}
