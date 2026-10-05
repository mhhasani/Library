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
@Schema(description = "Librarian scheduling of a return pickup")
public class ReturnScheduleRequest {

    @Schema(description = "Courier assigned to collect the book")
    @Size(max = 255, message = "حداکثر ۲۵۵ نویسه مجاز است")
    @SafeText
    private String returnCourierName;

    @Schema(description = "Scheduled pickup date/time")
    private LocalDateTime returnPlannedDate;
}
