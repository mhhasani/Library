package com.library.dto;

import com.library.entity.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change a user's account status")
public record UpdateStatusRequest(
        @NotNull(message = "وضعیت را انتخاب کنید")
        @Schema(description = "New account status", example = "SUSPENDED")
        AccountStatus status) {
}
