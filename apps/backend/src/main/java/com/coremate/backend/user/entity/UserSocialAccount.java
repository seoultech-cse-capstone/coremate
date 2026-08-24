package com.coremate.backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 사용자와 소셜 계정의 연결. 한 사용자가 여러 provider 를 연결할 수 있어 users 와 1:N 이다.
 * (provider, provider_id) 유니크로 같은 소셜 계정이 두 사용자에 붙는 것을 막는다.
 */
@Entity
@Table(name = "user_social_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_provider_provider_id",
                columnNames = {"provider", "provider_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserSocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    // provider 가 발급하는 고유 subject id(이메일 아님).
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserSocialAccount(Long userId, SocialProvider provider, String providerId) {
        this.userId = userId;
        this.provider = provider;
        this.providerId = providerId;
    }
}
