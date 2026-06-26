package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.LibraryCreationRequestDTO;
import com.library.dto.LibraryRequest;
import com.library.entity.enums.LibraryRequestStatus;
import com.library.service.LibraryRequestService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/v1/library-requests")
@Tag(name = "Library Requests", description = "Requesting and reviewing new library creation")
@SecurityRequirement(name = "Bearer Authentication")
public class LibraryRequestController {

    @Autowired
    private LibraryRequestService service;

    @PostMapping
    @Operation(summary = "Submit a library creation request")
    public ResponseEntity<ApiResponse<LibraryCreationRequestDTO>> submit(@Valid @RequestBody LibraryRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success("درخواست ایجاد کتابخانه ثبت شد", service.createRequest(request)),
                HttpStatus.CREATED);
    }

    @GetMapping("/mine")
    @Operation(summary = "List my library creation requests")
    public ResponseEntity<ApiResponse<List<LibraryCreationRequestDTO>>> mine() {
        return ResponseEntity.ok(ApiResponse.success("درخواست‌های شما", service.getMyRequests()));
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "List all library creation requests (system admin)")
    public ResponseEntity<ApiResponse<List<LibraryCreationRequestDTO>>> all(
            @RequestParam(required = false) LibraryRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.success("درخواست‌ها", service.getAllRequests(status)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Approve a library creation request (system admin)")
    public ResponseEntity<ApiResponse<LibraryCreationRequestDTO>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) LibraryRequest override) {
        return ResponseEntity.ok(ApiResponse.success("درخواست تأیید و کتابخانه ایجاد شد", service.approveRequest(id, override)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Reject a library creation request (system admin)")
    public ResponseEntity<ApiResponse<LibraryCreationRequestDTO>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("درخواست رد شد", service.rejectRequest(id, reason)));
    }
}
