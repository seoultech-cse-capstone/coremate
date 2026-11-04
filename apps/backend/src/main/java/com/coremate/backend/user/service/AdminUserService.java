package com.coremate.backend.user.service;

import com.coremate.backend.user.dto.response.AdminUserResponse;
import com.coremate.backend.user.entity.SocialProvider;
import com.coremate.backend.user.entity.UserSocialAccount;
import com.coremate.backend.user.repository.UserRepository;
import com.coremate.backend.user.repository.UserSocialAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 찾기. 앱을 쓸 수 없는 회원이 메일로 계정 삭제를 요청하면, 보낸 이메일이나 적어 준 닉네임으로
 * 계정을 찾아 탈퇴 처리한다(탈퇴는 {@code AuthService#withdrawByAdmin}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;

    /** 이메일과 닉네임은 각각 유일해서, 둘로 찾은 결과를 합쳐도 최대 두 명이다. */
    public List<AdminUserResponse> search(String keyword) {
        String trimmed = keyword.trim();
        return Stream.of(userRepository.findByEmail(trimmed), userRepository.findByNickname(trimmed))
                .flatMap(Optional::stream)
                .distinct()
                .map(user -> AdminUserResponse.of(user, providersOf(user.getUserId())))
                .toList();
    }

    private List<SocialProvider> providersOf(Long userId) {
        return socialAccountRepository.findByUserId(userId).stream()
                .map(UserSocialAccount::getProvider)
                .toList();
    }
}
