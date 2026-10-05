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
@Schema(description = "Update courier / dispatch details after a physical borrow is approved")
public class DeliveryDetailsRequest {

    @Schema(description = "Courier name", example = "علی رضایی")
    @Size(max = 255, message = "حداکثر ۲۵۵ نویسه مجاز است")
    @SafeText
    private String courierName;

    @Schema(description = "Unique code of the physical copy being dispatched", example = "LIB-A-00123")
    @Size(max = 100, message = "حداکثر ۱۰۰ نویسه مجاز است")
    @SafeText
    private String copyUniqueCode;

    @Schema(description = "Planned delivery date", example = "2026-06-28T10:00:00")
    private LocalDateTime plannedDeliveryDate;
}
