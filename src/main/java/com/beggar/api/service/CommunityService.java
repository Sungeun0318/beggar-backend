package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunityService {

    // TODO: findPosts(category)       — 커뮤니티 게시글 최신순 조회
    // TODO: createPost(userNo, req)   — 게시글 작성
    // TODO: findGlobalChatRoom()      — 전체 사용자가 참여하는 채팅방 조회
    // TODO: sendChatMessage(userNo, req) — 전체 채팅 메시지 저장/전송
}
