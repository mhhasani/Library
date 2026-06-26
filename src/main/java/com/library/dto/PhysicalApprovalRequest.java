package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Librarian approval details for a physical borrow request")
public class PhysicalApprovalRequest {

    @NotNull(message = "تاریخ تحویل الزامی است")
    @Schema(description = "Planned delivery date", example = "2026-06-28T10:00:00")
    private LocalDateTime plannedDeliveryDate;

    @NotNull(message = "تعداد روز امانت الزامی است")
    @Min(value = 1, message = "تعداد روز امانت باید حداقل ۱ باشد")
    @Schema(description = "Final number of loan days decided by the librarian", example = "5")
    private Integer approvedDurationDays;

    @Schema(description = "Unique code of the physical copy being dispatched", example = "LIB-A-00123")
    private String copyUniqueCode;

    @Schema(description = "Courier name (optional, can be set later)", example = "علی رضایی")
    private String courierName;

    @Schema(description = "Specific book copy id to assign (optional, auto-selected if omitted)")
    private Long bookCopyId;
}
