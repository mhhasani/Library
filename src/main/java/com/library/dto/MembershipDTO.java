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
@Schema(description = "Library membership information")
public class MembershipDTO {

    @Schema(description = "Membership ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User email")
    private String userEmail;

    @Schema(description = "User full name")
    private String userName;

    @Schema(description = "Library ID", example = "1")
    private Long libraryId;

    @Schema(description = "Membership role", example = "MEMBER")
    private LibraryMembershipRole role;

    @Schema(description = "Membership status", example = "APPROVED")
    private MembershipStatus status;

    @Schema(description = "ID of admin who approved this membership")
    private Long approvedById;

    @Schema(description = "Rejection reason if rejected")
    private String rejectionReason;

    @Schema(description = "Membership creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
