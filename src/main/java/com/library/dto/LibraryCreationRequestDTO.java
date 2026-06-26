package com.library.dto;

import com.library.entity.enums.LibraryRequestStatus;
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
@Schema(description = "A library creation request")
public class LibraryCreationRequestDTO {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private String requesterEmail;
    private String name;
    private String description;
    private Boolean autoMembershipApproval;
    private Integer defaultBorrowDurationDays;
    private LibraryRequestStatus status;
    private String rejectionReason;
    private Long createdLibraryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
