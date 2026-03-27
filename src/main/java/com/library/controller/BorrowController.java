package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BorrowApprovalRequest;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowRequest;
import com.library.entity.enums.BorrowStatus;
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
                    description = "Unauthorized"
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
            @RequestParam(required = false) BorrowStatus status) {
        log.info("Getting user borrows for library {} with status {}", libraryId, status);
        List<BorrowDTO> borrows = borrowService.getUserBorrows(libraryId, status);
        return ResponseEntity.ok(ApiResponse.success("User borrows retrieved successfully", borrows));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending borrow requests", description = "Retrieve pending borrow requests for admin review (admin only)")
    public ResponseEntity<ApiResponse<List<BorrowDTO>>> getPendingBorrows(@PathVariable Long libraryId) {
        log.info("Getting pending borrow requests for library {}", libraryId);
        List<BorrowDTO> borrows = borrowService.getPendingBorrows(libraryId);
        return ResponseEntity.ok(ApiResponse.success("Pending borrow requests retrieved successfully", borrows));
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Get all library borrows", description = "Retrieve all borrows in a library (admin only), optionally filtered by status")
    public ResponseEntity<ApiResponse<List<BorrowDTO>>> getLibraryBorrows(
            @PathVariable Long libraryId,
            @RequestParam(required = false) BorrowStatus status) {
        log.info("Getting all borrows for library {} with status {}", libraryId, status);
        List<BorrowDTO> borrows = borrowService.getLibraryBorrows(libraryId, status);
        return ResponseEntity.ok(ApiResponse.success("Library borrows retrieved successfully", borrows));
    }
}
