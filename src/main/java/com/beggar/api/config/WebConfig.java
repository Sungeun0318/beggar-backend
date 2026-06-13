package com.beggar.api.config;

import com.beggar.api.security.JwtInterceptor;
import com.beggar.api.security.AdminInterceptor;
import com.beggar.api.security.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 해당 인터셉터가 동작할 URL 패턴을 지정. "/**" = 모든 경로
                .excludePathPatterns(
                        "/",
                        "/admin/**",
                        "/auth/login",
                        "/auth/kakao",
                        "/auth/kakao/code",
                        "/auth/refresh",
                        "/users/signup",
                        "/locations/search",
                        "/error",
                        "/actuator/health"
                ); // 공개 엔드포인트와 상태 체크는 JWT 검사를 제외한다.

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 우리가 구현한 loginUserArgumentResolver를 스프링 설정에 추가합니다.
        // 이제 컨트롤러에서 @LoginUser가 붙은 파라미터를 만나면 이 리졸버가 동작합니다.
        resolvers.add(loginUserArgumentResolver);
    }
}
