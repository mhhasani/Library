package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.LibrarySubjectDTO;
import com.library.dto.SubjectRequest;
import com.library.service.LibrarySubjectService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/v1/libraries/{libraryId}/subjects")
@Tag(name = "Subjects", description = "Library subject/category management")
@SecurityRequirement(name = "Bearer Authentication")
public class LibrarySubjectController {

    @Autowired
    private LibrarySubjectService subjectService;

    @GetMapping
    @Operation(summary = "Get subjects", description = "Get all subjects defined for this library")
    public ResponseEntity<ApiResponse<List<LibrarySubjectDTO>>> getSubjects(@PathVariable Long libraryId) {
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved", subjectService.getSubjects(libraryId)));
    }

    @PostMapping
    @Operation(summary = "Create subject", description = "Create a new subject/category (admin only)")
    public ResponseEntity<ApiResponse<LibrarySubjectDTO>> createSubject(
            @PathVariable Long libraryId,
            @Valid @RequestBody SubjectRequest request) {
        LibrarySubjectDTO dto = subjectService.createSubject(libraryId, request.name());
        return new ResponseEntity<>(ApiResponse.success("Subject created", dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{subjectId}")
    @Operation(summary = "Delete subject", description = "Delete a subject (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(
            @PathVariable Long libraryId,
            @PathVariable Long subjectId) {
        subjectService.deleteSubject(libraryId, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Subject deleted"));
    }
}
