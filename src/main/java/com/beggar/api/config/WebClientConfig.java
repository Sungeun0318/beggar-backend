package com.beggar.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebFlux의 WebClient 빈 정의.
 * - kakaoWebClient: 카카오 OAuth/Profile API 호출
 * - aiServerWebClient: Python AI 추천 서버 호출
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .defaultHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .build();
    }

    @Bean
    public WebClient aiServerWebClient(@Value("${ai-server.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
