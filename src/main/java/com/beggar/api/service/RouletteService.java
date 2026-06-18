package com.beggar.api.service;

import com.beggar.api.dto.room.RouletteResultResponse;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomBudgetResult;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomBudgetResultRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RouletteService {
    private final RoomRepository roomRepository;
    private final RoomBudgetResultRepository roomBudgetResultRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ReceiptRepository receiptRepository;

    public RouletteResultResponse runRoulette(Long roomId, Long loginUserNo){
        // 방 찾기
        Room room = roomRepository.findById(roomId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 거지방입니다."));

        // 방장 권한 확인
        if (!room.getOwnerUserNo().equals(loginUserNo)){
                throw new IllegalArgumentException("방장만 운명의 거지룰렛을 돌릴 수 있습니다.");
            }

        // 방이 정말 종료된 상태인지 확인
        if (!room.getStatus().equals(RoomStatus.ENDED)){
            throw new IllegalArgumentException("종료된 방만 거지룰렛을 돌릴 수 있습니다.");
        }


        // 정산 데이터 가져오기
        RoomBudgetResult result = roomBudgetResultRepository.findByRoom_RoomNo(roomId)
                .orElseThrow(()->new IllegalArgumentException("정산 데이터가 없습니다."));

        // 목표로 정했던 총 예산
        Integer totalBudget = result.getTotalBudget();

        // 쓴 돈
        Long spentAmount = receiptRepository.sumAmountByRoomNo(roomId);

        // 룰렛에 걸 최종 잔액 예산
        Long remainingBudget = totalBudget - spentAmount;
        if (remainingBudget <= 0){ // 기본형은 null이 될 수 없어서 remainingBudget == null 은 안 해도 된다~ ㅎㅎ
            throw new IllegalArgumentException("룰렛 상금액이 부족합니다.");
        }

        return null;
    }
}
