package com.beggar.api.service.admin;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.admin.AdminLoginRequest;
import com.beggar.api.dto.admin.AdminTokenResponse;
import com.beggar.api.entity.AdminAccount;
import com.beggar.api.repository.admin.AdminAccountRepository;
import com.beggar.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminTokenResponse login(AdminLoginRequest request) {
        AdminAccount account = adminAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            throw new CustomException(ErrorCode.ADMIN_UNAUTHORIZED);
        }

        String accessToken = jwtTokenProvider.createAdminToken(account.getId(), account.getRole());
        return new AdminTokenResponse(accessToken, account.getUsername(), account.getRole());
    }
}
