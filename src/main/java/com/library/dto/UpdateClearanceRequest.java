package com.library.dto;

import com.library.entity.enums.ClassificationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change a user's classification clearance")
public record UpdateClearanceRequest(
        @NotNull(message = "سطح دسترسی را انتخاب کنید")
        @Schema(description = "New clearance", example = "CONFIDENTIAL")
        ClassificationLevel clearance) {
}
