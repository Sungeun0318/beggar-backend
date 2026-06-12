package com.beggar.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${app.cors.allowed-origin-patterns:https://dgh1r60fiahrz.cloudfront.net,https://beggar-webfront.vercel.app}")
    private String allowedOriginPatterns;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 소켓 연결을 위한 엔드포인트 설정 (/ws-stomp)
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns(parseAllowedOriginPatterns())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 받을 때 사용하는 경로 설정 (구독)
        registry.enableSimpleBroker("/sub", "/topic");
        
        // 메시지를 보낼 때 사용하는 경로 설정 (발행)
        registry.setApplicationDestinationPrefixes("/pub");
    }

    private String[] parseAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }
}
