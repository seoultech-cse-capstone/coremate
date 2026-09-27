package com.coremate.backend.global.security;

import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserStatus;
import com.coremate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        return new CustomUserDetails(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        return loadUserById(userId, null);
    }

    /**
     * 인증 필터 전용. 토큰의 sid 클레임을 principal 에 실어 컨트롤러까지 전달한다.
     * 정지·탈퇴 계정은 인증 대상이 아니므로 여기서 걸러 낸다.
     */
    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(Long userId, String sessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("사용할 수 없는 계정입니다: " + userId);
        }
        return new CustomUserDetails(user, sessionId);
    }
}
