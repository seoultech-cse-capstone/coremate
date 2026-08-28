package com.coremate.backend.avatar.room;

import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자가 방 격자에 놓은 가구 한 개. FURNITURE 만 대상이고 벽지·바닥은 슬롯 장착이라 여기 오지 않는다.
// 여러 칸을 차지하는 가구는 왼쪽 윗칸만 저장하고, 실제 점유 면적은 avatar_items.metadata.footprint 로 계산한다.
@Entity
@Table(
        name = "user_room_placements",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_room_placements_cell",
                columnNames = {"user_id", "grid_x", "grid_y"}
        ),
        indexes = @Index(name = "idx_user_room_placements_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserRoomPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placement_id")
    private Long placementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 배치한 보유 가구. 같은 가구를 여러 개 놓을 수 있어 배치 행 수는 user_items.quantity 이하여야 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_item_id", nullable = false)
    private UserItem userItem;

    // 방 격자 좌표(0부터). 범위는 가로 10 x 세로 7 이며 서비스에서 검증한다(EX-ITEM-004).
    @Column(name = "grid_x", nullable = false, columnDefinition = "tinyint")
    private int gridX;

    @Column(name = "grid_y", nullable = false, columnDefinition = "tinyint")
    private int gridY;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserRoomPlacement(User user, UserItem userItem, int gridX, int gridY) {
        this.user = user;
        this.userItem = userItem;
        this.gridX = gridX;
        this.gridY = gridY;
    }
}
