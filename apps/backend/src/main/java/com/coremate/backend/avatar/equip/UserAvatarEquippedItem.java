package com.coremate.backend.avatar.equip;

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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 슬롯별 현재 장착 중인 보유 아이템.
@Entity
@Table(
        name = "user_avatar_equipped_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_avatar_equipped_slot", columnNames = {"user_id", "slot"}),
        indexes = {
                @Index(name = "idx_user_avatar_equipped_user_id", columnList = "user_id"),
                @Index(name = "idx_user_avatar_equipped_user_item_id", columnList = "user_item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserAvatarEquippedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipped_item_id")
    private Long equippedItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_item_id", nullable = false)
    private UserItem userItem;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserAvatarEquippedItem(User user, String slot, UserItem userItem) {
        this.user = user;
        this.slot = slot;
        this.userItem = userItem;
    }

    public void changeUserItem(UserItem userItem) {
        this.userItem = userItem;
    }
}
