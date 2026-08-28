package com.coremate.backend.avatar.profile;

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

// 사용자별 기본 아바타 상태. 성별 구분은 두지 않는다.
@Entity
@Table(
        name = "user_avatar_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_avatar_profiles_user", columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserAvatarProfile {

    // 아바타에 성별 구분을 두지 않으므로 베이스 마네킹은 1종뿐이고 사용자가 바꿀 수 없다.
    public static final String DEFAULT_BASE_AVATAR_TYPE = "BASIC_01";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_avatar_profile_id")
    private Long userAvatarProfileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 베이스 마네킹(렌더 레이어 1). BASIC_01 고정이며 변경 수단이 없다.
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
        this.baseAvatarType = DEFAULT_BASE_AVATAR_TYPE;
    }
}
