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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String name;

    @Column(unique = true, nullable = false, length = 50)
    private String nickname;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private int level;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(name = "school_name", length = 100)
    private String schoolName;

    @Column(name = "school_email", length = 255)
    private String schoolEmail;

    @Column(name = "school_verified", nullable = false)
    private boolean schoolVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    public User(String email, String passwordHash, String name, String nickname, String phoneNumber, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.level = 1;
        this.totalXp = 0;
        this.schoolVerified = false;
        this.status = UserStatus.ACTIVE;
    }

    public void updateLastLoginAt(LocalDateTime at) {
        this.lastLoginAt = at;
    }

    public void updateProfile(String nickname, String schoolName, String schoolEmail) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        this.schoolName = schoolName;
        this.schoolEmail = schoolEmail;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        anonymizePersonalData();
    }

    /**
     * 탈퇴 시 개인정보를 파기(익명화)한다. unique 제약이 있는 email·nickname은
     * userId 기반 값으로 치환해 충돌을 피하고, 원본 값을 재가입에 다시 사용할 수 있게 한다.
     * 참조 무결성·법정 보존이 필요한 기록(결제 등)은 행 자체를 유지한다.
     */
    private void anonymizePersonalData() {
        this.email = "withdrawn_" + this.userId + "@removed.local";
        this.nickname = "탈퇴회원_" + this.userId;
        this.passwordHash = null;
        this.name = null;
        this.phoneNumber = null;
        this.schoolName = null;
        this.schoolEmail = null;
    }
}
