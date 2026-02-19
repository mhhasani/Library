package com.library.dto;

import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Library information response")
public class LibraryDTO {

    @Schema(description = "Library ID", example = "1")
    private Long id;

    @Schema(description = "Library name", example = "City Public Library")
    private String name;

    @Schema(description = "Library description")
    private String description;

    @Schema(description = "Owner user ID", example = "1")
    private Long ownerId;

    @Schema(description = "Owner name")
    private String ownerName;

    @Schema(description = "Auto-approve membership requests")
    private Boolean autoMembershipApproval;

    @Schema(description = "Default borrow duration in days", example = "14")
    private Integer defaultBorrowDurationDays;

    @Schema(description = "Library is active")
    private Boolean isActive;

    @Schema(description = "Current user's membership role in this library")
    private LibraryMembershipRole userRole;

    @Schema(description = "Current user's membership status in this library")
    private MembershipStatus userStatus;

    @Schema(description = "Library creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
