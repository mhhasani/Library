package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.dto.MembershipDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
        @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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

    @GetMapping("/{libraryId}/members")
    @Operation(summary = "Get library members", description = "Retrieve all members of a library (admin only)")
    public ResponseEntity<ApiResponse<List<MembershipDTO>>> getLibraryMembers(@PathVariable Long libraryId) {
        log.info("Getting members for library: {}", libraryId);
        List<MembershipDTO> members = libraryService.getLibraryMembers(libraryId);
        return ResponseEntity.ok(ApiResponse.success("Members retrieved successfully", members));
    }

    @GetMapping("/{libraryId}/members/pending")
    @Operation(summary = "Get pending membership requests", description = "Pending requests of a library (admin only)")
    public ResponseEntity<ApiResponse<List<MembershipDTO>>> getPendingMembers(@PathVariable Long libraryId) {
        return ResponseEntity.ok(ApiResponse.success("Pending members retrieved",
                libraryService.getPendingMembers(libraryId)));
    }

    @GetMapping("/{libraryId}/members/search")
    @Operation(summary = "Search/paginate library members", description = "Paginated, searchable non-pending members (admin only)")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<MembershipDTO>>> searchMembers(
            @PathVariable Long libraryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Members retrieved",
                libraryService.getMembersPaged(libraryId, search, pageable)));
    }

    @PatchMapping("/{libraryId}/members/{userId}/role")
    @Operation(summary = "Set a member's role",
            description = "Promote to ADMIN or demote to MEMBER (library owner or system admin only)")
    public ResponseEntity<ApiResponse<com.library.dto.LibraryDTO>> setMemberRole(
            @PathVariable Long libraryId,
            @PathVariable Long userId,
            @RequestParam com.library.entity.enums.LibraryMembershipRole role) {
        log.info("Setting role {} for user {} in library {}", role, userId, libraryId);
        return ResponseEntity.ok(ApiResponse.success("نقش عضو به‌روزرسانی شد",
                libraryService.setMemberRole(libraryId, userId, role)));
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
