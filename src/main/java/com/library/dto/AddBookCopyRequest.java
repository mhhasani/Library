package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to add physical book copies")
public class AddBookCopyRequest {

    @Positive(message = "تعداد نسخه‌ها باید بزرگ‌تر از صفر باشد")
    @Schema(description = "Number of copies to add", example = "5")
    private Integer numberOfCopies;
}
