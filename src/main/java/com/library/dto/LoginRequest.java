package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to login")
public class LoginRequest {

    @Email(message = "یک ایمیل معتبر وارد کنید")
    @NotBlank(message = "ایمیل را وارد کنید")
    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @NotBlank(message = "رمز عبور را وارد کنید")
    @Schema(description = "Password", example = "password123")
    private String password;
}
