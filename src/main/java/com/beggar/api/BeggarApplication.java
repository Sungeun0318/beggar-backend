package com.beggar.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BeggarApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeggarApplication.class, args);
    }
}
