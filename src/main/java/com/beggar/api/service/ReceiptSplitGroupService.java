package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.receipt.SplitGroupCreateRequest;
import com.beggar.api.dto.receipt.SplitGroupResponse;
import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.ReceiptSplitGroup;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.ReceiptSplitGroupRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReceiptSplitGroupService {

    private final ReceiptSplitGroupRepository splitGroupRepository;
    private final ReceiptRepository receiptRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final S3Service s3Service;

    @Transactional
    public SplitGroupResponse create(Long roomNo, Long userNo, SplitGroupCreateRequest request) {
        Room room = findOpenRoom(roomNo);
        RoomMember member = findActiveMember(roomNo, userNo);

        ReceiptSplitGroup group = ReceiptSplitGroup.builder()
                .room(room)
                .storeName(request.storeName())
                .address(request.address())
                .centerLat(request.centerLat())
                .centerLng(request.centerLng())
                .createdBy(member)
                .build();

        return toResponse(splitGroupRepository.save(group));
    }

    public List<SplitGroupResponse> list(Long roomNo, ReceiptSplitGroup.SplitGroupStatus status) {
        List<ReceiptSplitGroup> groups = status == null
                ? splitGroupRepository.findAllByRoom_RoomNoOrderByCreatedAtDesc(roomNo)
                : splitGroupRepository.findAllByRoom_RoomNoAndStatus(roomNo, status);

        return groups.stream()
                .map(this::toResponse)
                .toList();
    }

    public SplitGroupResponse get(Long roomNo, Long groupId) {
        return toResponse(findGroup(roomNo, groupId));
    }

    @Transactional
    public SplitGroupResponse close(Long roomNo, Long userNo, Long groupId) {
        findActiveMember(roomNo, userNo);
        ReceiptSplitGroup group = findGroup(roomNo, groupId);
        if (group.getStatus() == ReceiptSplitGroup.SplitGroupStatus.OPEN) {
            group.close();
        }
        return toResponse(group);
    }

    private Room findOpenRoom(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. ID: " + roomNo));
        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }
        return room;
    }

    private RoomMember findActiveMember(Long roomNo, Long userNo) {
        RoomMember member = roomMemberRepository.findByRoom_RoomNoAndUser_UserNo(roomNo, userNo)
                .orElseThrow(() -> new IllegalArgumentException("방 멤버만 분할 영수증을 관리할 수 있습니다."));
        if (member.getStatus() != RoomMember.Status.ACTIVE) {
            throw new IllegalArgumentException("활성 방 멤버만 분할 영수증을 관리할 수 있습니다.");
        }
        return member;
    }

    private ReceiptSplitGroup findGroup(Long roomNo, Long groupId) {
        return splitGroupRepository.findById(groupId)
                .filter(group -> group.getRoom().getRoomNo().equals(roomNo))
                .orElseThrow(() -> new IllegalArgumentException("분할 그룹을 찾을 수 없습니다. ID: " + groupId));
    }

    private SplitGroupResponse toResponse(ReceiptSplitGroup group) {
        List<Receipt> receipts = receiptRepository.findAllBySplitGroup_SplitGroupId(group.getSplitGroupId());
        List<SplitGroupResponse.Item> items = receipts.stream()
                .map(receipt -> new SplitGroupResponse.Item(
                        receipt.getReceiptId(),
                        receipt.getUploader().getUser().getUserNo(),
                        receipt.getUploader().getUser().getUserName(),
                        receipt.getAmount(),
                        s3Service.generatePresignedGetUrl(receipt.getImageUrl())
                ))
                .toList();

        int totalAmount = receipts.stream()
                .map(Receipt::getAmount)
                .filter(amount -> amount != null)
                .mapToInt(Integer::intValue)
                .sum();

        int contributorCount = receipts.stream()
                .map(receipt -> receipt.getUploader().getUser().getUserNo())
                .distinct()
                .toList()
                .size();

        return new SplitGroupResponse(
                group.getSplitGroupId(),
                group.getRoom().getRoomNo(),
                group.getStoreName(),
                group.getAddress(),
                group.getStatus().name(),
                totalAmount,
                receipts.size(),
                contributorCount,
                group.getCreatedAt(),
                group.getClosedAt(),
                items
        );
    }
}
