package com.library.controller;

import com.library.entity.FileResource;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.FileResourceRepository;
import com.library.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/files")
@Tag(name = "Files", description = "File serving endpoints")
public class FileController {

    @Autowired private FileResourceRepository fileResourceRepository;
    @Autowired private StorageService storageService;

    @GetMapping("/{fileId}")
    @Operation(summary = "Serve a file (cover images are public)")
    public ResponseEntity<Resource> serveFile(@PathVariable Long fileId) {
        FileResource fileResource = fileResourceRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        Resource resource = storageService.load(fileResource.getFilePath());

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(fileResource.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
