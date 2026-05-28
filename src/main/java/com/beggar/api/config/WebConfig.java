package com.beggar.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // TODO: addInterceptors(InterceptorRegistry registry)
    //   registry.addInterceptor(jwtInterceptor)
    //           .addPathPatterns("/**")
    //           .excludePathPatterns("/auth/kakao", "/auth/refresh", "/error", "/actuator/health");
    //
    // TODO: addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers)
    //   resolvers.add(loginUserArgumentResolver);
    //
    // JwtInterceptor / LoginUserArgumentResolver 를 구현하고 나면
    // 생성자로 주입받아(@RequiredArgsConstructor) 위 두 메서드에 등록.
}
