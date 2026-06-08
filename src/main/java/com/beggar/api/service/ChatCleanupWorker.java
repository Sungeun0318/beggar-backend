package com.beggar.api.service;

import com.beggar.api.repository.RoomFreeChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCleanupWorker {

    private final RoomFreeChatRepository chatRepository;

    // 매일 새벽 3시에 10일이 지난 채팅 내역 삭제
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldChats() {
        LocalDateTime targetDate = LocalDateTime.now().minusDays(10);
        log.info("{} 이전의 채팅 내역을 삭제합니다.", targetDate);
        chatRepository.deleteOldChats(targetDate);
        log.info("채팅 내역 정리 완료.");
    }
}
