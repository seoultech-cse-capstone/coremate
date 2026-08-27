package com.coremate.backend.shop.entity;

import com.coremate.backend.avatar.catalog.AvatarItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 랜덤박스에서 등장 가능한 보상 후보와 확률.
@Entity
@Table(name = "random_box_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RandomBoxItem {

    public static final String REWARD_TYPE_ITEM = "ITEM";
    public static final String REWARD_TYPE_COIN = "COIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "random_box_item_id")
    private Long randomBoxItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_id", nullable = false)
    private RandomBox randomBox;

    @Column(name = "reward_type", nullable = false, length = 20)
    private String rewardType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private AvatarItem item;

    @Column(name = "coin_amount")
    private Integer coinAmount;

    @Column(nullable = false, precision = 8, scale = 6)
    private BigDecimal probability;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isItemReward() {
        return REWARD_TYPE_ITEM.equalsIgnoreCase(rewardType);
    }

    public boolean isCoinReward() {
        return REWARD_TYPE_COIN.equalsIgnoreCase(rewardType);
    }
}
