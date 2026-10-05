package com.library.entity;

import com.library.entity.enums.AuditOutcome;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One immutable security-audit record. Records are append-only (enforced by a DB trigger)
 * and hash-chained: {@code recordHash = SHA-256(prevHash + canonical fields)}.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_actor_id", columnList = "actor_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_entity_type", columnList = "entity_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_outcome", columnList = "outcome")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plain id (not a relation) so the record survives independently of the user row. */
    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    /** Identity as presented — also recorded for failed attempts with unknown users. */
    @Column(name = "actor_email", length = 255, updatable = false)
    private String actorEmail;

    @Column(nullable = false, length = 100, updatable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AuditOutcome outcome;

    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 512, updatable = false)
    private String userAgent;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "prev_hash", length = 64, updatable = false)
    private String prevHash;

    @Column(name = "record_hash", length = 64, updatable = false)
    private String recordHash;
}
