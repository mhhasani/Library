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
@Schema(description = "Recipient request to have a borrowed book picked up for return")
public class ReturnRequest {

    @Schema(description = "Address where the courier should collect the book")
    private String returnAddress;

    @Schema(description = "Recipient internal phone number (8 digits)")
    private String returnExtension;

    @Schema(description = "Preferred pickup date/time (may be earlier than the due date)")
    private LocalDateTime preferredDate;
}
