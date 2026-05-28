package com.beggar.api.security;

public @interface LoginUser {

    // TODO: 컨트롤러 메서드 파라미터에 붙여 userNo 자동 주입용 어노테이션.
    //   @Target(ElementType.PARAMETER)
    //   @Retention(RetentionPolicy.RUNTIME)
    //   를 추가하고, LoginUserArgumentResolver 에서 처리.
    //
    // 사용 예: public ApiResponse<?> foo(@LoginUser Long userNo) { ... }
}
