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

    @Schema(description = "Human-friendly unique tracking code", example = "BR-000001")
    private String trackingCode;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User email")
    private String userEmail;

    @Schema(description = "User full name (for delivery)")
    private String userFullName;

    @Schema(description = "User phone number (for delivery)")
    private String userPhone;

    @Schema(description = "Library ID", example = "1")
    private Long libraryId;

    @Schema(description = "Library name (for cross-library views)")
    private String libraryName;

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

    // ---- Physical delivery fields ----

    @Schema(description = "Delivery address provided by the recipient")
    private String deliveryAddress;

    @Schema(description = "Recipient internal phone number")
    private String deliveryExtension;

    @Schema(description = "Loan days requested by the user")
    private Integer requestedDurationDays;

    @Schema(description = "Loan days finalized by the librarian")
    private Integer approvedDurationDays;

    @Schema(description = "Planned delivery date set by the librarian")
    private LocalDateTime plannedDeliveryDate;

    @Schema(description = "Courier name (may be null until assigned)")
    private String courierName;

    @Schema(description = "Unique code of the dispatched physical copy")
    private String copyUniqueCode;

    @Schema(description = "When the recipient confirmed receipt")
    private LocalDateTime receivedAt;

    // ---- Return-pickup workflow ----
    @Schema(description = "When the recipient requested a return pickup")
    private LocalDateTime returnRequestedAt;
    @Schema(description = "Address for the return pickup")
    private String returnAddress;
    @Schema(description = "Recipient internal phone for the return pickup")
    private String returnExtension;
    @Schema(description = "Recipient's preferred pickup date")
    private LocalDateTime returnPreferredDate;
    @Schema(description = "Courier assigned for the return pickup")
    private String returnCourierName;
    @Schema(description = "Scheduled return pickup date")
    private LocalDateTime returnPlannedDate;
    @Schema(description = "When the recipient handed the book to the courier")
    private LocalDateTime handedOverByUserAt;

    @Schema(description = "Borrow creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
