package com.library.entity;

import com.library.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic, domain-agnostic in-app notification.
 *
 * It is intentionally NOT tied to any single entity (e.g. Borrow). Instead it carries a
 * loose polymorphic reference ({@code relatedEntityType} + {@code relatedEntityId}) and an
 * optional frontend {@code link}, so any feature (borrows, memberships, books, ...) can emit
 * notifications without schema changes.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_recipient", columnList = "recipient_id"),
    @Index(name = "idx_notif_recipient_read", columnList = "recipient_id, is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** Loose reference to the related domain object, e.g. "BORROW", "MEMBERSHIP", "BOOK". */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /** Id of the related domain object (no FK constraint — kept generic on purpose). */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /** Optional frontend route the notification should navigate to when clicked. */
    @Column(length = 512)
    private String link;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
