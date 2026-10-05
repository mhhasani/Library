package com.library.dto;

import com.library.entity.enums.SystemRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change a user's system role")
public record UpdateRoleRequest(
        @NotNull(message = "نقش را انتخاب کنید")
        @Schema(description = "New system role", example = "SYSTEM_ADMIN")
        SystemRole role) {
}
