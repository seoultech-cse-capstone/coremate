package com.coremate.backend.avatar.profile;

import com.coremate.backend.avatar.dto.type.AvatarGenderType;
import com.coremate.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자별 기본 아바타 타입과 성별/타입 상태.
@Entity
@Table(
        name = "user_avatar_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_avatar_profiles_user", columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserAvatarProfile {

    public static final AvatarGenderType DEFAULT_GENDER = AvatarGenderType.OTHER;
    public static final String DEFAULT_BASE_AVATAR_TYPE = "BASIC_01";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_avatar_profile_id")
    private Long userAvatarProfileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "avatar_gender", length = 20)
    private String avatarGender;

    @Column(name = "base_avatar_type", length = 50)
    private String baseAvatarType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserAvatarProfile(User user) {
        this.user = user;
        this.avatarGender = DEFAULT_GENDER.name();
        this.baseAvatarType = DEFAULT_BASE_AVATAR_TYPE;
    }

    public void update(AvatarGenderType avatarGender, String baseAvatarType) {
        if (avatarGender != null) {
            this.avatarGender = avatarGender.name();
        }
        if (baseAvatarType != null && !baseAvatarType.isBlank()) {
            this.baseAvatarType = baseAvatarType;
        }
    }
}
