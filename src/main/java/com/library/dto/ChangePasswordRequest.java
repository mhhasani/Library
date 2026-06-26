package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to change user password")
public class ChangePasswordRequest {

    @NotBlank(message = "رمز عبور فعلی را وارد کنید")
    @Schema(description = "Current password")
    private String currentPassword;

    @NotBlank(message = "رمز عبور جدید را وارد کنید")
    @Size(min = 8, message = "رمز عبور جدید باید حداقل ۸ کاراکتر باشد")
    @Schema(description = "New password (minimum 8 characters)")
    private String newPassword;
}
