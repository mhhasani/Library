package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.service.LibraryService;
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
@RequestMapping("/v1/libraries")
@Tag(name = "Libraries", description = "Library management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class LibraryController {

    @Autowired
    private LibraryService libraryService;

    @PostMapping
    @Operation(summary = "Create a new library", description = "Create a new library")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Library created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LibraryDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input"
            )
    })
    public ResponseEntity<ApiResponse<LibraryDTO>> createLibrary(@Valid @RequestBody LibraryRequest request) {
        log.info("Creating library: {}", request.getName());
        LibraryDTO library = libraryService.createLibrary(request);
        return new ResponseEntity<>(
                ApiResponse.success("Library created successfully", library),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{libraryId}")
    @Operation(summary = "Get library details", description = "Retrieve library information")
    public ResponseEntity<ApiResponse<LibraryDTO>> getLibrary(@PathVariable Long libraryId) {
        log.info("Getting library: {}", libraryId);
        LibraryDTO library = libraryService.getLibraryById(libraryId);
        return ResponseEntity.ok(ApiResponse.success("Library retrieved successfully", library));
    }

    @GetMapping
    @Operation(summary = "Get user's libraries", description = "Retrieve all libraries the user is a member of")
    public ResponseEntity<ApiResponse<List<LibraryDTO>>> getUserLibraries() {
        log.info("Getting user libraries");
        List<LibraryDTO> libraries = libraryService.getUserLibraries();
        return ResponseEntity.ok(ApiResponse.success("Libraries retrieved successfully", libraries));
    }

    @GetMapping("/public/active")
    @Operation(summary = "Get all active libraries", description = "Retrieve all active libraries (public endpoint)")
    public ResponseEntity<ApiResponse<List<LibraryDTO>>> getActiveLibraries() {
        log.info("Getting active libraries");
        List<LibraryDTO> libraries = libraryService.getAllActiveLibraries();
        return ResponseEntity.ok(ApiResponse.success("Active libraries retrieved successfully", libraries));
    }

    @PutMapping("/{libraryId}")
    @Operation(summary = "Update library", description = "Update library details (admin only)")
    public ResponseEntity<ApiResponse<LibraryDTO>> updateLibrary(
            @PathVariable Long libraryId,
            @Valid @RequestBody LibraryRequest request) {
        log.info("Updating library: {}", libraryId);
        LibraryDTO library = libraryService.updateLibrary(libraryId, request);
        return ResponseEntity.ok(ApiResponse.success("Library updated successfully", library));
    }

    @DeleteMapping("/{libraryId}")
    @Operation(summary = "Delete library", description = "Delete library (owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteLibrary(@PathVariable Long libraryId) {
        log.info("Deleting library: {}", libraryId);
        libraryService.deleteLibrary(libraryId);
        return ResponseEntity.ok(ApiResponse.success("Library deleted successfully"));
    }

    @PostMapping("/{libraryId}/membership/request")
    @Operation(summary = "Request library membership", description = "Request to join a library")
    public ResponseEntity<ApiResponse<Void>> requestMembership(@PathVariable Long libraryId) {
        log.info("Requesting membership for library: {}", libraryId);
        libraryService.requestMembership(libraryId);
        return new ResponseEntity<>(
                ApiResponse.success("Membership request submitted successfully"),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{libraryId}/membership/{userId}/approve")
    @Operation(summary = "Approve membership", description = "Approve user membership (admin only)")
    public ResponseEntity<ApiResponse<Void>> approveMembership(
            @PathVariable Long libraryId,
            @PathVariable Long userId) {
        log.info("Approving membership for user {} in library {}", userId, libraryId);
        libraryService.approveMembership(libraryId, userId);
        return ResponseEntity.ok(ApiResponse.success("Membership approved successfully"));
    }

    @PostMapping("/{libraryId}/membership/{userId}/reject")
    @Operation(summary = "Reject membership", description = "Reject user membership (admin only)")
    public ResponseEntity<ApiResponse<Void>> rejectMembership(
            @PathVariable Long libraryId,
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        log.info("Rejecting membership for user {} in library {}", userId, libraryId);
        libraryService.rejectMembership(libraryId, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("Membership rejected successfully"));
    }
}
