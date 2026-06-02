package com.beggar.api.repository;

import com.beggar.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email); // 이메일로 사용자 찾기
    boolean existsByUserName(String userName); // 닉네임 중복검사
}
