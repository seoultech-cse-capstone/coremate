package com.coremate.backend.avatar.profile;

import com.coremate.backend.avatar.room.UserRoomPlacement;
import com.coremate.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    // 방에서 마지막에 취한 자세. NULL 은 STAND 와 같게 취급한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "room_pose", length = 20)
    private RoomPose roomPose;

    // 그 포즈를 취한 가구. 같은 침대를 여러 개 놓을 수 있어 "어느 침대"인지가 필요하다.
    // ⚠️ ddl-auto 는 ON DELETE SET NULL 을 만들어 주지 않는다. 가구를 뺄 때 서비스가 직접 끊어야 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_pose_placement_id")
    private UserRoomPlacement roomPosePlacement;

    // 포즈가 마지막으로 "바뀐" 시각. 친구 방의 "N일 전부터 자는 중" 계산 기준이다.
    // updated_at 을 재사용하지 않는 이유는 포즈와 무관한 수정으로도 그 값이 바뀌어 시각이 오염되기 때문이다.
    @Column(name = "room_pose_updated_at")
    private LocalDateTime roomPoseUpdatedAt;

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

    // 포즈가 실제로 바뀔 때만 시각을 갱신한다.
    // 같은 포즈를 다시 저장해도 친구 방의 "3일 전부터 자는 중"이 "방금 전부터"로 초기화되지 않아야 한다.
    public void updateRoomPose(RoomPose roomPose, UserRoomPlacement placement, LocalDateTime now) {
        boolean poseChanged = this.roomPose != roomPose;
        this.roomPose = roomPose;
        this.roomPosePlacement = placement;
        if (poseChanged) {
            this.roomPoseUpdatedAt = now;
        }
    }

    // 포즈가 가리키던 가구가 방에서 빠졌을 때 호출한다. 배치가 사라지면 기본 자리에 서서 시작한다.
    // 시각까지 비우는 이유는 STAND 로 돌아간 뒤 남은 시각이 "N일 전부터"로 잘못 읽힐 수 있기 때문이다.
    public void clearRoomPose() {
        this.roomPose = null;
        this.roomPosePlacement = null;
        this.roomPoseUpdatedAt = null;
    }
}
