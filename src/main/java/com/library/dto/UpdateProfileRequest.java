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
@Schema(description = "Request to update user profile")
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Schema(description = "First name", example = "علی")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Schema(description = "Last name", example = "احمدی")
    private String lastName;

    @Size(max = 20)
    @Schema(description = "Phone number", example = "09123456789")
    private String phoneNumber;
}
