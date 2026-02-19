package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create or update a library")
public class LibraryRequest {

    @NotBlank(message = "Library name cannot be blank")
    @Schema(description = "Library name", example = "City Public Library")
    private String name;

    @Schema(description = "Library description", example = "A comprehensive public library with digital and physical collections")
    private String description;

    @Schema(description = "Auto-approve membership requests", example = "false")
    private Boolean autoMembershipApproval = false;

    @Positive(message = "Borrow duration must be positive")
    @Schema(description = "Default borrow duration in days", example = "14")
    private Integer defaultBorrowDurationDays = 14;
}
