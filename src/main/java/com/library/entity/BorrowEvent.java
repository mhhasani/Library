package com.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * An immutable timeline entry recording a single event in a borrow's lifecycle
 * (who did what and when), shown to librarians as a per-borrow audit log.
 */
@Entity
@Table(name = "borrow_events", indexes = {
    @Index(name = "idx_borrow_event_borrow", columnList = "borrow_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrow_id", nullable = false)
    private Borrow borrow;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String detail;

    /** Display name of the person who performed the action (or "سیستم"). */
    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
