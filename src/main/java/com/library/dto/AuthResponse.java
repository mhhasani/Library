package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication response with tokens")
public class AuthResponse {

    @Schema(description = "Access token for API requests", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh token to get a new access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "User system role", example = "USER")
    private String systemRole;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";
}
