package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.DigitalBookDTO;
import com.library.service.CoverImageService;
import com.library.service.DigitalBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/libraries/{libraryId}/books/{bookId}")
@Tag(name = "Digital Books", description = "Digital book upload, download, and management")
@SecurityRequirement(name = "Bearer Authentication")
public class DigitalBookController {

    @Autowired private DigitalBookService digitalBookService;
    @Autowired private CoverImageService coverImageService;

    // ── Cover Image ──────────────────────────────────────────────────────────

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload cover image (admin only)")
    public ResponseEntity<ApiResponse<Void>> uploadCoverImage(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading cover image for book: {}", bookId);
        coverImageService.uploadCoverImage(libraryId, bookId, file);
        return ResponseEntity.ok(ApiResponse.success("Cover image uploaded successfully"));
    }

    // ── Digital Books ────────────────────────────────────────────────────────

    @PostMapping(value = "/digital", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a digital book PDF (admin only)")
    public ResponseEntity<ApiResponse<DigitalBookDTO>> uploadDigitalBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "versionName", required = false) String versionName) {
        log.info("Uploading digital book PDF: bookId={}", bookId);
        DigitalBookDTO result = digitalBookService.uploadDigitalBook(libraryId, bookId, file, versionName);
        return new ResponseEntity<>(ApiResponse.success("Digital book uploaded successfully", result), HttpStatus.CREATED);
    }

    @GetMapping("/digital")
    @Operation(summary = "List available digital formats for a book")
    public ResponseEntity<ApiResponse<List<DigitalBookDTO>>> listDigitalBooks(
            @PathVariable Long libraryId,
            @PathVariable Long bookId) {
        List<DigitalBookDTO> list = digitalBookService.listDigitalBooks(libraryId, bookId);
        return ResponseEntity.ok(ApiResponse.success("Digital books retrieved successfully", list));
    }

    @GetMapping("/digital/{digitalBookId}/download")
    @Operation(summary = "Download a digital book (requires active approved borrow)")
    public ResponseEntity<Resource> downloadDigitalBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @PathVariable Long digitalBookId) {
        log.info("Downloading digital book: id={}", digitalBookId);
        Resource resource = digitalBookService.downloadDigitalBook(digitalBookId);
        String contentType = digitalBookService.getContentType(digitalBookId);
        String filename = digitalBookService.getOriginalFilename(digitalBookId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/digital/{digitalBookId}")
    @Operation(summary = "Delete a digital book version (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteDigitalBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @PathVariable Long digitalBookId) {
        log.info("Deleting digital book: id={}", digitalBookId);
        digitalBookService.deleteDigitalBook(libraryId, bookId, digitalBookId);
        return ResponseEntity.ok(ApiResponse.success("Digital book deleted successfully"));
    }
}
