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
@Schema(description = "A single event in a borrow's timeline")
public class BorrowEventDTO {
    private Long id;
    private String title;
    private String detail;
    private String actorName;
    private LocalDateTime createdAt;
}
