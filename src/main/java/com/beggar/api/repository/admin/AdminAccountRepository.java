package com.beggar.api.repository.admin;

import com.beggar.api.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {

    Optional<AdminAccount> findByUsername(String username);
}
