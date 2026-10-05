package com.library.entity;

import com.library.entity.enums.SensitiveOperation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The single row of administrator-tunable security settings. Ranges are enforced by the
 * service, by Bean Validation on the request and by CHECK constraints in the database.
 */
@Entity
@Table(name = "security_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** Failed logins before a temporary lockout (1..6). */
    @Column(name = "max_failed_logins", nullable = false)
    private int maxFailedLogins;

    @Column(name = "lockout_minutes", nullable = false)
    private int lockoutMinutes;

    /** Failure counter resets after this long without failures. */
    @Column(name = "failure_reset_minutes", nullable = false)
    private int failureResetMinutes;

    /** Inactivity after which the session ends (15..30). */
    @Column(name = "session_idle_minutes", nullable = false)
    private int sessionIdleMinutes;

    /** Previous passwords that cannot be reused (1..3 for this classification level). */
    @Column(name = "password_history", nullable = false)
    private int passwordHistory;

    @Column(name = "password_max_age_days", nullable = false)
    private int passwordMaxAgeDays;

    @Column(name = "mfa_required", nullable = false)
    private boolean mfaRequired;

    /** How recent an authentication must be for a sensitive operation. */
    @Column(name = "reauth_window_minutes", nullable = false)
    private int reauthWindowMinutes;

    /** Comma-separated {@link SensitiveOperation} names. */
    @Column(name = "sensitive_operations", length = 1000)
    private String sensitiveOperations;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Set<SensitiveOperation> sensitiveOperationSet() {
        if (sensitiveOperations == null || sensitiveOperations.isBlank()) {
            return EnumSet.noneOf(SensitiveOperation.class);
        }
        return Arrays.stream(sensitiveOperations.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(SensitiveOperation::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SensitiveOperation.class)));
    }

    public void setSensitiveOperationSet(Set<SensitiveOperation> operations) {
        this.sensitiveOperations = operations.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
