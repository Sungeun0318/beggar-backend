package com.beggar.api.entity;

public enum RoomStatus {
    DRAFT,          // 방 생성 중 (예산 입력 전)
    INVITING,       // 초대 대기 중
    BUDGET_INPUT,   // 예산 입력 중
    BUDGET_DONE,    // 예산 확정 완료
    ACTIVE,         // 거지방 진행 중
    ENDED,          // 방 종료
    DELETED         // 방 삭제
}
