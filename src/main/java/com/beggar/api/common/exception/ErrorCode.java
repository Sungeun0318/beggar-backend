package com.beggar.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 토큰입니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_003", "카카오 로그인에 실패했습니다."),

    // Admin
    ADMIN_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ADMIN_001", "관리자 인증이 필요합니다."),
    ADMIN_FORBIDDEN(HttpStatus.FORBIDDEN, "ADMIN_002", "관리자 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USER_NAME(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 닉네임입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_003", "이미 사용 중인 이메일입니다."),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_001", "방을 찾을 수 없습니다."),
    DUPLICATE_ROOM_NAME(HttpStatus.CONFLICT, "ROOM_002", "이미 존재하는 방 이름입니다. 다른 이름을 사용해주세요."),
    INVALID_ROOM_CODE(HttpStatus.NOT_FOUND, "ROOM_003", "유효하지 않은 초대 코드입니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "ROOM_004", "이미 참여 중인 방입니다."),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "ROOM_005", "방 멤버가 아닙니다."),
    ROOM_NOT_OPEN(HttpStatus.FORBIDDEN, "ROOM_006", "친구 전용 방은 초대 코드로만 입장할 수 있습니다."),
    ROOM_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "ROOM_007", "이미 종료된 방입니다."),

    // Budget
    BUDGET_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "BUDGET_001", "이미 확정된 예산입니다."),
    BUDGET_NOT_READY(HttpStatus.CONFLICT, "BUDGET_002", "모든 멤버가 제출하지 않았습니다."),

    // Receipt
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "RECEIPT_001", "영수증을 찾을 수 없습니다."),

    // External API
    EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "EXTERNAL_001", "외부 API 호출에 실패했습니다."),

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
