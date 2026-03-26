package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create or update a book")
public class BookRequest {

    @NotBlank(message = "Book title cannot be blank")
    @Schema(description = "Book title", example = "Clean Code")
    private String title;

    @NotBlank(message = "Book author cannot be blank")
    @Schema(description = "Author name", example = "Robert C. Martin")
    private String author;

    @Schema(description = "Publisher name", example = "Prentice Hall")
    private String publisher;

    @Positive(message = "Publication year must be positive")
    @Schema(description = "Year of publication", example = "2008")
    private Integer publicationYear;

    @Schema(description = "Book description")
    private String description;

    @Schema(description = "Auto-approve digital borrow requests", example = "false")
    private Boolean autoDigitalBorrowEnabled = false;
}
