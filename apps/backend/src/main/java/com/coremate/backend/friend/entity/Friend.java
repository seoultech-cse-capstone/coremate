package com.coremate.backend.friend.entity;

import com.coremate.backend.friend.dto.type.FriendStatus;
import com.coremate.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// 두 사용자 사이의 친구 요청과 수락 상태를 한 행으로 관리한다.
@Entity
@Table(
        name = "friends",
        uniqueConstraints = @UniqueConstraint(name = "uq_friends_user_pair", columnNames = {"user_low_id", "user_high_id"}),
        indexes = {
                @Index(name = "idx_friends_requester_id", columnList = "requester_id"),
                @Index(name = "idx_friends_receiver_id", columnList = "receiver_id"),
                @Index(name = "idx_friends_user_pair_status", columnList = "user_low_id,user_high_id,status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_id")
    private Long friendId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "user_low_id", nullable = false)
    private Long userLowId;

    @Column(name = "user_high_id", nullable = false)
    private Long userHighId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public Friend(User requester, User receiver) {
        this.requester = requester;
        this.receiver = receiver;
        this.userLowId = Math.min(requester.getUserId(), receiver.getUserId());
        this.userHighId = Math.max(requester.getUserId(), receiver.getUserId());
        this.status = FriendStatus.REQUESTED;
    }

    public void accept(LocalDateTime acceptedAt) {
        this.status = FriendStatus.ACCEPTED;
        this.acceptedAt = acceptedAt;
    }

    public void reject() {
        this.status = FriendStatus.REJECTED;
        this.acceptedAt = null;
    }

    public void block() {
        this.status = FriendStatus.BLOCKED;
        this.acceptedAt = null;
    }

    public void reopen(User requester, User receiver) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = FriendStatus.REQUESTED;
        this.acceptedAt = null;
    }

    public Long getOtherUserId(Long userId) {
        if (requester.getUserId().equals(userId)) {
            return receiver.getUserId();
        }
        return requester.getUserId();
    }

    public User getOtherUser(Long userId) {
        if (requester.getUserId().equals(userId)) {
            return receiver;
        }
        return requester;
    }

    public boolean involves(Long userId) {
        return requester.getUserId().equals(userId) || receiver.getUserId().equals(userId);
    }
}
