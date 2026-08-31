package com.coremate.backend.avatar.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 상점, 랜덤박스, 보관함에서 공통으로 참조하는 아바타 아이템 마스터.
@Entity
@Table(
        name = "avatar_items",
        // 시드가 매 기동 실행되므로 ON DUPLICATE KEY UPDATE 로 멱등해야 한다.
        // 걸 키가 없으면 재시작마다 같은 아이템이 다시 쌓인다.
        uniqueConstraints = @UniqueConstraint(name = "uq_avatar_items_name", columnNames = "item_name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AvatarItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(length = 50)
    private String slot;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 기본 지급 8종(머리·눈·입·상의·하의·신발·벽지·바닥) 표시. 비매품이고 랜덤박스 후보에서 제외한다.
    @Column(name = "is_default", nullable = false)
    private boolean defaultItem;

    // 렌더링 부가 정보(JSON). 백엔드는 저장·전달만 하고 해석은 앱이 한다.
    // footprint {"w":2,"h":1} 가구가 차지하는 칸 수(FURNITURE 필수)
    // interaction "desk"|"bed"  아바타가 포즈를 취하게 하는 가구 표시
    // poseOffsets {"sit":[0,-8],"sleep":null}  포즈별 스프라이트 오프셋(null 이면 그 포즈에서 안 그림)
    // dualPart true  가방처럼 앞뒤 두 조각으로 렌더
    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isEquippable() {
        return slot != null && !slot.isBlank();
    }
}
