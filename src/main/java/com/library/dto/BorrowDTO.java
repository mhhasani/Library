package com.library.dto;

import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.BorrowType;
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
@Schema(description = "Borrow record information")
public class BorrowDTO {

    @Schema(description = "Borrow ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User email")
    private String userEmail;

    @Schema(description = "Library ID", example = "1")
    private Long libraryId;

    @Schema(description = "Book ID", example = "1")
    private Long bookId;

    @Schema(description = "Book title")
    private String bookTitle;

    @Schema(description = "Borrow type", example = "PHYSICAL")
    private BorrowType borrowType;

    @Schema(description = "Book copy ID (for physical borrow)")
    private Long bookCopyId;

    @Schema(description = "Copy number (for physical borrow)")
    private Integer copyNumber;

    @Schema(description = "Borrow status", example = "REQUESTED")
    private BorrowStatus status;

    @Schema(description = "Approved by admin ID")
    private Long approvedById;

    @Schema(description = "Rejection reason if rejected")
    private String rejectionReason;

    @Schema(description = "Date when borrow was approved")
    private LocalDateTime borrowDate;

    @Schema(description = "Due date for return")
    private LocalDateTime dueDate;

    @Schema(description = "Date when book was returned")
    private LocalDateTime returnDate;

    @Schema(description = "Is overdue")
    private Boolean isOverdue;

    @Schema(description = "Is a reservation (physical borrow with no copy assigned yet)")
    private Boolean isReservation;

    @Schema(description = "Borrow creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
