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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 상점에서 코인으로 구매 가능한 상품. 아이템 또는 랜덤박스와 연결될 수 있다.
@Entity
@Table(name = "shop_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ShopProduct {

    public static final String PRODUCT_TYPE_RANDOM_BOX = "RANDOM_BOX";
    public static final String PRODUCT_TYPE_ITEM = "ITEM";
    public static final String PRODUCT_TYPE_ROOM_THEME = "ROOM_THEME";
    public static final String PRODUCT_TYPE_ACTION_CARD = "ACTION_CARD";
    public static final String PRODUCT_TYPE_GENDER_CHANGE = "GENDER_CHANGE";
    public static final String PRODUCT_TYPE_NICKNAME_CHANGE = "NICKNAME_CHANGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_product_id")
    private Long shopProductId;

    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private AvatarItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "random_box_id")
    private RandomBox randomBox;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "price_coin", nullable = false)
    private int priceCoin;

    @Column(name = "required_level", nullable = false)
    private int requiredLevel;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isRandomBoxProduct() {
        return PRODUCT_TYPE_RANDOM_BOX.equalsIgnoreCase(productType);
    }

    public boolean grantsItem() {
        return item != null && (
                PRODUCT_TYPE_ITEM.equalsIgnoreCase(productType)
                        || PRODUCT_TYPE_ROOM_THEME.equalsIgnoreCase(productType)
                        || PRODUCT_TYPE_ACTION_CARD.equalsIgnoreCase(productType)
        );
    }
}
