package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.SecuritySettingsDTO;
import com.library.dto.UpdateSecuritySettingsRequest;
import com.library.entity.enums.SensitiveOperation;
import com.library.security.RequiresRecentAuthentication;
import com.library.settings.SecuritySettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Security settings: viewable by system admins, changeable by the super admin only. */
@RestController
@RequestMapping("/v1/admin/security-settings")
@Tag(name = "Security settings", description = "Lockout, session, password and MFA settings")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SecuritySettingsController {

    private final SecuritySettingsService settingsService;

    public SecuritySettingsController(SecuritySettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @Operation(summary = "Current security settings")
    public ResponseEntity<ApiResponse<SecuritySettingsDTO>> get() {
        return ResponseEntity.ok(ApiResponse.success("Security settings",
                SecuritySettingsDTO.from(settingsService.get())));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @RequiresRecentAuthentication(SensitiveOperation.SECURITY_SETTINGS_CHANGE)
    @Operation(summary = "Change security settings", description = "Applied to the identity provider in the same step")
    public ResponseEntity<ApiResponse<SecuritySettingsDTO>> update(
            @Valid @RequestBody UpdateSecuritySettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Security settings updated",
                SecuritySettingsDTO.from(settingsService.update(request))));
    }

    @GetMapping("/sensitive-operations")
    @Operation(summary = "Operations that can be marked as sensitive")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> sensitiveOperations() {
        List<Map<String, String>> operations = Arrays.stream(SensitiveOperation.values())
                .map(op -> Map.of("value", op.name(), "label", op.getPersianLabel()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Sensitive operations", operations));
    }
}
