package com.beggar.api.repository;

import com.beggar.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);

    List<User> findTop5ByOrderByCreatedAtDesc();

    Page<User> findByUserNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String userName,
            String email,
            Pageable pageable
    );
}
