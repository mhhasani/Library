package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BorrowApprovalRequest;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowRequest;
import com.library.dto.DeliveryDetailsRequest;
import com.library.dto.PhysicalApprovalRequest;
import com.library.dto.ReturnRequest;
import com.library.dto.ReturnScheduleRequest;
import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.BorrowType;
import com.library.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/libraries/{libraryId}/borrows")
@Tag(name = "Borrows", description = "Book borrowing and return management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @PostMapping("/{bookId}")
    @Operation(summary = "Create borrow request", description = "Request to borrow a book")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Borrow request created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BorrowDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or book not available"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "دسترسی غیرمجاز"
            )
    })
    public ResponseEntity<ApiResponse<BorrowDTO>> createBorrowRequest(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @Valid @RequestBody BorrowRequest request) {
        log.info("Creating borrow request for book {} in library {}", bookId, libraryId);
        BorrowDTO borrow = borrowService.createBorrowRequest(libraryId, bookId, request);
        return new ResponseEntity<>(
                ApiResponse.success("Borrow request created successfully", borrow),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{bookId}/reserve")
    @Operation(summary = "Reserve a book", description = "Reserve a book when no copies are available; placed in queue")
    public ResponseEntity<ApiResponse<BorrowDTO>> reserveBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId) {
        log.info("Reserving book {} in library {}", bookId, libraryId);
        BorrowDTO borrow = borrowService.reserveBook(libraryId, bookId);
        return new ResponseEntity<>(
                ApiResponse.success("Book reserved successfully", borrow),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{borrowId}/approve")
    @Operation(summary = "Approve borrow request", description = "Approve a pending borrow request (admin only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> approveBorrow(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        log.info("Approving borrow request {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.approveBorrowRequest(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Borrow request approved successfully", borrow));
    }

    @PutMapping("/{borrowId}/request")
    @Operation(summary = "Edit a pending physical borrow request",
            description = "Update delivery details while the request is still REQUESTED (owner only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> updatePhysicalRequest(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId,
            @Valid @RequestBody BorrowRequest request) {
        log.info("Editing physical borrow request {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.updatePhysicalRequest(libraryId, borrowId, request);
        return ResponseEntity.ok(ApiResponse.success("Borrow request updated successfully", borrow));
    }

    @PostMapping("/{borrowId}/approve-physical")
    @Operation(summary = "Approve physical borrow with delivery details",
            description = "Approve a pending PHYSICAL borrow request and record delivery details (admin only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> approvePhysicalBorrow(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId,
            @Valid @RequestBody PhysicalApprovalRequest request) {
        log.info("Approving physical borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.approvePhysicalBorrow(libraryId, borrowId, request);
        return ResponseEntity.ok(ApiResponse.success("Physical borrow approved successfully", borrow));
    }

    @PatchMapping("/{borrowId}/delivery")
    @Operation(summary = "Update delivery details",
            description = "Set or update courier / copy code / delivery date after approval (admin only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> updateDeliveryDetails(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId,
            @RequestBody DeliveryDetailsRequest request) {
        log.info("Updating delivery details for borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.updateDeliveryDetails(libraryId, borrowId, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery details updated successfully", borrow));
    }

    @PostMapping("/{borrowId}/confirm-receipt")
    @Operation(summary = "Confirm book receipt",
            description = "Recipient confirms receiving the physical book; starts the loan clock")
    public ResponseEntity<ApiResponse<BorrowDTO>> confirmReceipt(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        log.info("Confirming receipt for borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.confirmReceipt(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Receipt confirmed successfully", borrow));
    }

    @PostMapping("/{borrowId}/request-return")
    @Operation(summary = "Request a return pickup",
            description = "Recipient asks for the book to be collected (may be before the due date)")
    public ResponseEntity<ApiResponse<BorrowDTO>> requestReturn(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId,
            @RequestBody ReturnRequest request) {
        log.info("Return pickup requested for borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.requestReturn(libraryId, borrowId, request);
        return ResponseEntity.ok(ApiResponse.success("Return requested successfully", borrow));
    }

    @PatchMapping("/{borrowId}/return-schedule")
    @Operation(summary = "Schedule a return pickup",
            description = "Librarian assigns courier and pickup date for a requested return (admin only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> scheduleReturnPickup(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId,
            @RequestBody ReturnScheduleRequest request) {
        log.info("Scheduling return pickup for borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.scheduleReturnPickup(libraryId, borrowId, request);
        return ResponseEntity.ok(ApiResponse.success("Return pickup scheduled successfully", borrow));
    }

    @PostMapping("/{borrowId}/cancel-return-request")
    @Operation(summary = "Cancel a pending return request",
            description = "Recipient or librarian cancels the return request, reverting to an active loan")
    public ResponseEntity<ApiResponse<BorrowDTO>> cancelReturnRequest(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        BorrowDTO borrow = borrowService.cancelReturnRequest(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Return request cancelled", borrow));
    }

    @PostMapping("/{borrowId}/confirm-handover")
    @Operation(summary = "Confirm handover to courier",
            description = "Recipient confirms they handed the book to the courier (awaits librarian confirmation)")
    public ResponseEntity<ApiResponse<BorrowDTO>> confirmHandover(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        log.info("Confirming handover for borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.confirmHandover(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Handover confirmed successfully", borrow));
    }

    @PostMapping("/{borrowId}/confirm-return")
    @Operation(summary = "Confirm physical return",
            description = "Librarian records that the physical book has been returned (admin only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> confirmReturn(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        log.info("Confirming physical return for borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.confirmReturnByLibrarian(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Return confirmed successfully", borrow));
    }

    @PostMapping("/{borrowId}/cancel")
    @Operation(summary = "Cancel a borrow (recipient)",
            description = "Recipient cancels their request/loan before receiving the book")
    public ResponseEntity<ApiResponse<BorrowDTO>> cancelByUser(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        log.info("User cancelling borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.cancelByUser(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Borrow cancelled successfully", borrow));
    }

    @PostMapping("/{borrowId}/cancel-admin")
    @Operation(summary = "Cancel a borrow (librarian)",
            description = "Librarian cancels a request/loan before completion (admin only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> cancelByLibrarian(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        log.info("Librarian cancelling borrow {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.cancelByLibrarian(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Borrow cancelled successfully", borrow));
    }

    @GetMapping("/{borrowId}/events")
    @Operation(summary = "Get a borrow's event timeline",
            description = "Full chronological audit log of a borrow (admin only)")
    public ResponseEntity<ApiResponse<List<com.library.dto.BorrowEventDTO>>> getBorrowEvents(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        return ResponseEntity.ok(ApiResponse.success("Borrow events retrieved",
                borrowService.getBorrowEvents(libraryId, borrowId)));
    }

    @GetMapping("/user/{userId}/summary")
    @Operation(summary = "Get a borrower's summary",
            description = "Borrowing situation of a user in this library, to help the librarian decide (admin only)")
    public ResponseEntity<ApiResponse<com.library.dto.BorrowerSummaryDTO>> getBorrowerSummary(
            @PathVariable Long libraryId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Borrower summary retrieved",
                borrowService.getBorrowerSummary(libraryId, userId)));
    }

    @PostMapping("/{borrowId}/reject")
    @Operation(summary = "Reject borrow request", description = "Reject a pending borrow request (admin only)")
    public ResponseEntity<ApiResponse<BorrowDTO>> rejectBorrow(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId,
            @RequestParam(required = false) String reason) {
        log.info("Rejecting borrow request {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.rejectBorrowRequest(libraryId, borrowId, reason);
        return ResponseEntity.ok(ApiResponse.success("Borrow request rejected successfully", borrow));
    }

    @PostMapping("/{borrowId}/return")
    @Operation(summary = "Return book", description = "Return a borrowed book")
    public ResponseEntity<ApiResponse<BorrowDTO>> returnBook(
            @PathVariable Long libraryId,
            @PathVariable Long borrowId) {
        log.info("Returning book for borrow request {} in library {}", borrowId, libraryId);
        BorrowDTO borrow = borrowService.returnBook(libraryId, borrowId);
        return ResponseEntity.ok(ApiResponse.success("Book returned successfully", borrow));
    }

    @GetMapping
    @Operation(summary = "Get user's borrows", description = "Retrieve borrow records for current user, optionally filtered by status")
    public ResponseEntity<ApiResponse<List<BorrowDTO>>> getUserBorrows(
            @PathVariable Long libraryId,
            @RequestParam(required = false) BorrowStatus status,
            @RequestParam(required = false) BorrowType type) {
        log.info("Getting user borrows for library {} with status {} type {}", libraryId, status, type);
        List<BorrowDTO> borrows = borrowService.getUserBorrows(libraryId, status, type);
        return ResponseEntity.ok(ApiResponse.success("User borrows retrieved successfully", borrows));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending borrow requests", description = "Retrieve pending borrow requests for admin review (admin only)")
    public ResponseEntity<ApiResponse<List<BorrowDTO>>> getPendingBorrows(
            @PathVariable Long libraryId,
            @RequestParam(required = false) BorrowType type) {
        log.info("Getting pending borrow requests for library {} type {}", libraryId, type);
        List<BorrowDTO> borrows = borrowService.getPendingBorrows(libraryId, type);
        return ResponseEntity.ok(ApiResponse.success("Pending borrow requests retrieved successfully", borrows));
    }

    @GetMapping("/admin/search")
    @Operation(summary = "Search/paginate library borrows (admin)",
            description = "Paginated, searchable borrows for admin tables")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<BorrowDTO>>> searchLibraryBorrows(
            @PathVariable Long libraryId,
            @RequestParam(required = false) java.util.List<BorrowStatus> statuses,
            @RequestParam(required = false) BorrowType type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean needsAttention,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        var result = borrowService.getLibraryBorrowsPaged(
                libraryId, statuses, type, search,
                Boolean.TRUE.equals(needsAttention), Boolean.TRUE.equals(overdue), pageable);
        return ResponseEntity.ok(ApiResponse.success("Borrows retrieved", result));
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Get all library borrows", description = "Retrieve all borrows in a library (admin only), optionally filtered by status")
    public ResponseEntity<ApiResponse<List<BorrowDTO>>> getLibraryBorrows(
            @PathVariable Long libraryId,
            @RequestParam(required = false) BorrowStatus status,
            @RequestParam(required = false) BorrowType type) {
        log.info("Getting all borrows for library {} with status {} type {}", libraryId, status, type);
        List<BorrowDTO> borrows = borrowService.getLibraryBorrows(libraryId, status, type);
        return ResponseEntity.ok(ApiResponse.success("Library borrows retrieved successfully", borrows));
    }
}
