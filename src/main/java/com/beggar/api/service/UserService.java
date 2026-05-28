package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    // TODO: getMyProfile(userNo) — 마이페이지 프로필 조회
}
