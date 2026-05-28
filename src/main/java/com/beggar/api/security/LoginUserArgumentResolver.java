package com.beggar.api.security;

import org.springframework.stereotype.Component;

@Component
public class LoginUserArgumentResolver {

    // TODO: implements HandlerMethodArgumentResolver
    //
    // supportsParameter(parameter):
    //   parameter 에 @LoginUser 가 붙어 있고 Long 타입이면 true
    //
    // resolveArgument(parameter, ...):
    //   webRequest.getNativeRequest(HttpServletRequest.class)
    //     .getAttribute("userNo") 반환 (JwtInterceptor 가 저장한 값)
    //
    // WebConfig.addArgumentResolvers() 에서 등록.
}
