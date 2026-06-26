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
@Schema(description = "Update courier / dispatch details after a physical borrow is approved")
public class DeliveryDetailsRequest {

    @Schema(description = "Courier name", example = "علی رضایی")
    private String courierName;

    @Schema(description = "Unique code of the physical copy being dispatched", example = "LIB-A-00123")
    private String copyUniqueCode;

    @Schema(description = "Planned delivery date", example = "2026-06-28T10:00:00")
    private LocalDateTime plannedDeliveryDate;
}
