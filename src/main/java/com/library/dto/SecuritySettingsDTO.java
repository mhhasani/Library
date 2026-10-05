package com.library.dto;

import com.library.entity.SecuritySettings;
import com.library.entity.enums.SensitiveOperation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "Administrator-tunable security settings")
public record SecuritySettingsDTO(
        int maxFailedLogins,
        int lockoutMinutes,
        int failureResetMinutes,
        int sessionIdleMinutes,
        int passwordHistory,
        int passwordMaxAgeDays,
        boolean mfaRequired,
        int reauthWindowMinutes,
        Set<SensitiveOperation> sensitiveOperations,
        LocalDateTime updatedAt) {

    public static SecuritySettingsDTO from(SecuritySettings s) {
        return new SecuritySettingsDTO(s.getMaxFailedLogins(), s.getLockoutMinutes(), s.getFailureResetMinutes(),
                s.getSessionIdleMinutes(), s.getPasswordHistory(), s.getPasswordMaxAgeDays(), s.isMfaRequired(),
                s.getReauthWindowMinutes(), s.sensitiveOperationSet(), s.getUpdatedAt());
    }
}
