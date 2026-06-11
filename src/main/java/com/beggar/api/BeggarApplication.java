package com.beggar.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(exclude = {
        // 서버 확장 시 Redis 전환이 가능하도록 의존성은 유지하되, 구동 크래시 방지를 위해 단일 환경에서의 자동 연결 시도 제외
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
public class BeggarApplication {

    public static void main(String[] args){
        SpringApplication.run(BeggarApplication.class, args);
    }
}
