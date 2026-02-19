package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Request to register a new user")
public class RegisterRequest {

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password should have at least 6 characters")
    @Schema(description = "Password", example = "password123")
    private String password;

    @NotBlank(message = "First name cannot be blank")
    @Size(min = 2, max = 100, message = "First name should be between 2 and 100 characters")
    @Schema(description = "First name", example = "John")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(min = 2, max = 100, message = "Last name should be between 2 and 100 characters")
    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Phone number (optional)", example = "+1234567890")
    private String phoneNumber;
}
