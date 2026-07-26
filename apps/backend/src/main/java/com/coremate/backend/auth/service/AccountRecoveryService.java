package com.coremate.backend.auth.service;

import com.coremate.backend.auth.dto.request.FindEmailRequest;
import com.coremate.backend.auth.dto.response.FindEmailResponse;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountRecoveryService {

    private final UserRepository userRepository;

    public FindEmailResponse findEmail(FindEmailRequest request) {
        List<User> users = userRepository.findAllByNameAndPhoneNumber(
                request.getName(), request.getPhoneNumber());

        if (users.isEmpty()) {
            throw new CoreMateException(ErrorCode.USER_NOT_FOUND);
        }

        List<String> maskedEmails = users.stream()
                .map(User::getEmail)
                .map(AccountRecoveryService::maskEmail)
                .toList();

        return FindEmailResponse.builder()
                .emails(maskedEmails)
                .build();
    }

    /**
     * 이메일 로컬 파트 일부만 노출하고 나머지는 마스킹한다. (예: lange50300@gmail.com -> lan****@gmail.com)
     */
    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }

        String local = email.substring(0, at);
        String domain = email.substring(at);
        int visible = local.length() <= 3 ? 1 : 3;

        return local.substring(0, visible) + "****" + domain;
    }
}
