package com.library.audit;

import com.library.entity.enums.AuditAction;
import com.library.entity.enums.AuditOutcome;
import lombok.Builder;

/**
 * What to record. Actor and request fields left null are filled from the current
 * security context and HTTP request by {@link AuditService}.
 */
@Builder
public record AuditEntry(
        AuditAction action,
        AuditOutcome outcome,
        String entityType,
        Long entityId,
        String details,
        Long actorId,
        String actorEmail) {

    public static AuditEntryBuilder success(AuditAction action) {
        return builder().action(action).outcome(AuditOutcome.SUCCESS);
    }

    public static AuditEntryBuilder failure(AuditAction action) {
        return builder().action(action).outcome(AuditOutcome.FAILURE);
    }
}
