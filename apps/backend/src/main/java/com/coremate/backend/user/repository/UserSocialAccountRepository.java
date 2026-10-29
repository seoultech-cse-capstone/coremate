package com.coremate.backend.user.repository;

import com.coremate.backend.user.entity.SocialProvider;
import com.coremate.backend.user.entity.UserSocialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);

    List<UserSocialAccount> findByUserId(Long userId);
}
