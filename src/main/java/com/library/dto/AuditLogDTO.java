package com.library.dto;

import com.library.entity.AuditLog;
import com.library.entity.enums.AuditOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Security audit record")
public record AuditLogDTO(
        Long id,
        LocalDateTime timestamp,
        Long actorId,
        String actorEmail,
        String action,
        AuditOutcome outcome,
        String entityType,
        Long entityId,
        String ipAddress,
        String userAgent,
        String details) {

    public static AuditLogDTO from(AuditLog log) {
        return new AuditLogDTO(log.getId(), log.getTimestamp(), log.getActorId(), log.getActorEmail(),
                log.getAction(), log.getOutcome(), log.getEntityType(), log.getEntityId(),
                log.getIpAddress(), log.getUserAgent(), log.getDetails());
    }
}
