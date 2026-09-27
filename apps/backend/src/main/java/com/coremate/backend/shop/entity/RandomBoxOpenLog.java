package com.coremate.backend.shop.entity;

import com.coremate.backend.avatar.catalog.AvatarItem;
import com.coremate.backend.avatar.inventory.entity.UserItem;
import com.coremate.backend.user.entity.User;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 랜덤박스 오픈 결과를 추적하기 위한 운영 로그.
@Entity
@Table(name = "random_box_open_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RandomBoxOpenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "random_box_open_log_id")
    private Long randomBoxOpenLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_id", nullable = false)
    private RandomBox randomBox;

    @Column(name = "currency_log_id")
    private Long currencyLogId;

    @Column(name = "reward_type", nullable = false, length = 20)
    private String rewardType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_item_id")
    private AvatarItem rewardItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_user_item_id")
    private UserItem rewardUserItem;

    @Column(name = "reward_coin_amount")
    private Integer rewardCoinAmount;

    @CreatedDate
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    public RandomBoxOpenLog(User user, RandomBox randomBox, Long currencyLogId, String rewardType,
                            AvatarItem rewardItem, UserItem rewardUserItem, Integer rewardCoinAmount) {
        this.user = user;
        this.randomBox = randomBox;
        this.currencyLogId = currencyLogId;
        this.rewardType = rewardType;
        this.rewardItem = rewardItem;
        this.rewardUserItem = rewardUserItem;
        this.rewardCoinAmount = rewardCoinAmount;
    }
}
