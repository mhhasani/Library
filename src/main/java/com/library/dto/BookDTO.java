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
@Schema(description = "Book information response")
public class BookDTO {

    @Schema(description = "Book ID", example = "1")
    private Long id;

    @Schema(description = "Library ID", example = "1")
    private Long libraryId;

    @Schema(description = "Book title", example = "Clean Code")
    private String title;

    @Schema(description = "Author name", example = "Robert C. Martin")
    private String author;

    @Schema(description = "Publisher name")
    private String publisher;

    @Schema(description = "Year of publication", example = "2008")
    private Integer publicationYear;

    @Schema(description = "Book description")
    private String description;

    @Schema(description = "Cover image URL (served by the API)")
    private String coverImageUrl;

    @Schema(description = "Cover image file resource ID")
    private Long coverImageFileResourceId;

    @Schema(description = "Auto-approve digital borrow requests")
    private Boolean autoDigitalBorrowEnabled;

    @Schema(description = "Number of available physical copies")
    private Long availableCopiesCount;

    @Schema(description = "Total number of physical copies")
    private Long totalCopiesCount;

    @Schema(description = "Has digital versions available")
    private Boolean hasDigitalVersions;

    @Schema(description = "Book creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
