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
@Schema(description = "Librarian scheduling of a return pickup")
public class ReturnScheduleRequest {

    @Schema(description = "Courier assigned to collect the book")
    private String returnCourierName;

    @Schema(description = "Scheduled pickup date/time")
    private LocalDateTime returnPlannedDate;
}
