package com.library.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_actor_id", columnList = "actor_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_entity_type", columnList = "entity_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
