package com.coremate.backend.support.entity;

import com.coremate.backend.support.entity.type.InquiryCategory;
import com.coremate.backend.support.entity.type.InquiryStatus;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 사용자가 보낸 문의. 관리자 웹에서 조회·답변한다.
@Entity
@Table(
        name = "inquiry",
        indexes = {
                @Index(name = "idx_inquiry_user_id", columnList = "user_id"),
                @Index(name = "idx_inquiry_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 300)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Column(length = 1000)
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Inquiry(User user, InquiryCategory category, String title, String body) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.body = body;
        this.status = InquiryStatus.PENDING;
    }

    // 관리자가 답변을 등록하면 상태를 답변 완료로 바꾸고 답변 시각을 남긴다.
    public void answer(String answer) {
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }
}
