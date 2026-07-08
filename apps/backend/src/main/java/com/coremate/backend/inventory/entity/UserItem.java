package com.coremate.backend.inventory.entity;

import com.coremate.backend.avatar.entity.AvatarItem;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자가 획득한 아이템과 수량을 관리하는 보관함 행.
@Entity
@Table(
        name = "user_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_items_user_item", columnNames = {"user_id", "item_id"}),
        indexes = {
                @Index(name = "idx_user_items_user_id", columnList = "user_id"),
                @Index(name = "idx_user_items_item_id", columnList = "item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_item_id")
    private Long userItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private AvatarItem item;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "acquired_source", nullable = false, length = 50)
    private String acquiredSource;

    @Column(name = "source_id")
    private Long sourceId;

    @CreatedDate
    @Column(name = "acquired_at", nullable = false, updatable = false)
    private LocalDateTime acquiredAt;

    public UserItem(User user, AvatarItem item, int quantity, String acquiredSource, Long sourceId) {
        this.user = user;
        this.item = item;
        this.quantity = quantity;
        this.acquiredSource = acquiredSource;
        this.sourceId = sourceId;
    }

    // 중복 획득 정책은 같은 아이템 수량 증가로 처리한다.
    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    public boolean isOwned() {
        return quantity > 0;
    }
}
