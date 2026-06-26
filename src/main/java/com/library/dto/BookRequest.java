package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create or update a book")
public class BookRequest {

    @NotBlank(message = "عنوان کتاب را وارد کنید")
    @Schema(description = "Book title", example = "Clean Code")
    private String title;

    @NotBlank(message = "نام نویسنده را وارد کنید")
    @Schema(description = "Author name", example = "Robert C. Martin")
    private String author;

    @Schema(description = "Publisher name", example = "Prentice Hall")
    private String publisher;

    @Positive(message = "سال انتشار باید معتبر باشد")
    @Schema(description = "Year of publication", example = "2008")
    private Integer publicationYear;

    @Schema(description = "Subject IDs (from library's predefined subjects list)")
    private List<Long> subjectIds;

    @Schema(description = "Book description")
    private String description;

    @Schema(description = "Auto-approve digital borrow requests", example = "false")
    private Boolean autoDigitalBorrowEnabled = false;
}
