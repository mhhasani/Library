package com.library.dto;

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
@Schema(description = "Digital book version information")
public class DigitalBookDTO {

    @Schema(description = "Digital book ID")
    private Long id;

    @Schema(description = "Book ID")
    private Long bookId;

    @Schema(description = "File format (PDF, EPUB, MOBI, AZW3)")
    private String fileFormat;

    @Schema(description = "Version name")
    private String versionName;

    @Schema(description = "Original filename")
    private String originalFilename;

    @Schema(description = "File size in bytes")
    private Long fileSizeBytes;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;
}
