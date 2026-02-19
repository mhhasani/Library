package com.library.dto;

import com.library.entity.enums.BorrowType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to borrow a book")
public class BorrowRequest {

    @NotNull(message = "Borrow type cannot be null")
    @Schema(description = "Type of borrow (PHYSICAL or DIGITAL)", example = "PHYSICAL")
    private BorrowType borrowType;

    @Schema(description = "Book copy ID (required for PHYSICAL borrow)", example = "1")
    private Long bookCopyId;

    @Schema(description = "Digital book ID (required for DIGITAL borrow)", example = "1")
    private Long digitalBookId;
}
