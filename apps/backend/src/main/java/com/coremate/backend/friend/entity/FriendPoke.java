package com.coremate.backend.friend.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 친구에게 보낸 가벼운 상호작용 알림 로그.
@Entity
@Table(
        name = "friend_pokes",
        indexes = {
                @Index(name = "idx_friend_pokes_from_user_id", columnList = "from_user_id"),
                @Index(name = "idx_friend_pokes_to_user_id", columnList = "to_user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FriendPoke {

    public static final String DEFAULT_MESSAGE_TYPE = "POKE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poke_id")
    private Long pokeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 읽음 상태를 두지 않는다. 수신자에게 보이는 목록과 읽음 처리는 인앱 알림함(FR-NOTI-05)이 담당한다.
    public FriendPoke(User fromUser, User toUser, String messageType) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.messageType = normalizeMessageType(messageType);
    }

    private String normalizeMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return DEFAULT_MESSAGE_TYPE;
        }
        return messageType.trim().toUpperCase();
    }
}
