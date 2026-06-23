package com.beggar.api.service;

import com.beggar.api.dto.room.RoomMemberResponse;
import com.beggar.api.dto.room.RouletteResultResponse;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomBudgetResult;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.entity.RoomRouletteResult;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomBudgetResultRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import com.beggar.api.repository.RoomRouletteResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RouletteService {
    private final RoomRepository roomRepository;
    private final RoomBudgetResultRepository roomBudgetResultRepository;
    private final RoomRouletteResultRepository roomRouletteResultRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ReceiptRepository receiptRepository;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public RouletteResultResponse getRouletteResult(Long roomNo, Long loginUserNo) {
        Room room = findRoom(roomNo);
        validateParticipant(roomNo, loginUserNo);

        return roomRouletteResultRepository.findByRoom_RoomNo(roomNo)
                .map(result -> toResponse(room, result, loginUserNo))
                .orElse(null);
    }

    public RouletteResultResponse runRoulette(Long roomNo, Long loginUserNo) {
        Room room = findRoom(roomNo);

        if (!room.getOwnerUserNo().equals(loginUserNo)) {
            throw new IllegalArgumentException("방장만 거지 룰렛을 돌릴 수 있습니다.");
        }

        return roomRouletteResultRepository.findByRoom_RoomNo(roomNo)
                .map(result -> toResponse(room, result, loginUserNo))
                .orElseGet(() -> createRouletteResult(room, loginUserNo));
    }

    private RouletteResultResponse createRouletteResult(Room room, Long loginUserNo) {
        if (room.getStatus() != RoomStatus.ENDED) {
            throw new IllegalArgumentException("종료된 방만 거지 룰렛을 돌릴 수 있습니다.");
        }

        Long roomNo = room.getRoomNo();
        RoomBudgetResult budgetResult = roomBudgetResultRepository.findByRoom_RoomNo(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("정산 데이터가 없습니다."));

        long spentAmount = receiptRepository.sumAmountByRoomNo(roomNo);
        long remainingBudget = budgetResult.getTotalBudget().longValue() - spentAmount;
        if (remainingBudget <= 0) {
            throw new IllegalArgumentException("룰렛 상금이 부족합니다.");
        }

        List<RoomMember> members = roomMemberRepository.findByRoom_RoomNoAndStatus(
                roomNo,
                RoomMember.Status.ACTIVE
        );
        if (members.isEmpty()) {
            throw new IllegalArgumentException("룰렛에 참여할 멤버가 없습니다.");
        }

        RoomMember winnerMember = members.get(random.nextInt(members.size()));
        RoomRouletteResult savedResult = roomRouletteResultRepository.save(
                new RoomRouletteResult(room, winnerMember.getUser(), remainingBudget)
        );

        return toResponse(room, savedResult, loginUserNo);
    }

    private RouletteResultResponse toResponse(Room room, RoomRouletteResult result, Long loginUserNo) {
        List<RoomMemberResponse> allMembers = roomMemberRepository.findByRoom_RoomNoAndStatus(
                        room.getRoomNo(),
                        RoomMember.Status.ACTIVE
                )
                .stream()
                .map(member -> RoomMemberResponse.from(member, loginUserNo, true))
                .toList();

        return new RouletteResultResponse(
                room.getRoomNo(),
                result.getWinner().getUserNo(),
                result.getWinnerNickname(),
                result.getRemainingBudget(),
                allMembers
        );
    }

    private Room findRoom(Long roomNo) {
        return roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));
    }

    private void validateParticipant(Long roomNo, Long loginUserNo) {
        if (loginUserNo == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        roomMemberRepository.findByRoom_RoomNoAndUser_UserNo(roomNo, loginUserNo)
                .filter(member -> member.getStatus() == RoomMember.Status.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 참여자만 룰렛 결과를 볼 수 있습니다."));
    }
}
