package com.library.controller;

import com.library.entity.FileResource;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.FileResourceRepository;
import com.library.service.DigitalBookService;
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
    @Autowired private DigitalBookService digitalBookService;

    @GetMapping("/{fileId}")
    @Operation(summary = "Serve a file (cover images are public; digital book files are gated)")
    public ResponseEntity<Resource> serveFile(@PathVariable Long fileId) {
        FileResource fileResource = fileResourceRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("فایل پیدا نشد: " + fileId));

        // /v1/files/** is permitAll for public cover images, so digital book PDFs
        // stored in the same FileResource table must re-check the borrow-approval gate here.
        digitalBookService.assertFileAccess(fileId);

        Resource resource = storageService.load(fileResource.getFilePath());

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(fileResource.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        // Only images and PDFs are ever served inline; anything else is forced to download.
        boolean image = "image".equals(mediaType.getType()) && !mediaType.getSubtype().contains("svg");
        boolean pdf = MediaType.APPLICATION_PDF.equalsTypeAndSubtype(mediaType);
        if (!image && !pdf) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                // Public cover images may be cached; gated files (digital books) must never be
                // stored by the browser or intermediate proxies.
                .header(HttpHeaders.CACHE_CONTROL, image ? "public, max-age=86400" : "no-store, private")
                .header(HttpHeaders.CONTENT_DISPOSITION, (image || pdf) ? "inline" : "attachment")
                // Served bytes can never run script in the app's origin
                .header("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; sandbox")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
