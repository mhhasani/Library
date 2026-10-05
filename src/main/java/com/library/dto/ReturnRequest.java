package com.library.dto;

import com.library.validation.SafeText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Recipient request to have a borrowed book picked up for return")
public class ReturnRequest {

    @Schema(description = "Address where the courier should collect the book")
    @Size(max = 2000, message = "نشانی بیش از حد طولانی است")
    @SafeText
    private String returnAddress;

    @Schema(description = "Recipient internal phone number (8 digits)")
    @Size(max = 50, message = "حداکثر ۵۰ نویسه مجاز است")
    @SafeText
    private String returnExtension;

    @Schema(description = "Preferred pickup date/time (may be earlier than the due date)")
    private LocalDateTime preferredDate;
}
