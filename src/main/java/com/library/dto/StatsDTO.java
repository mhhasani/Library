package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "System statistics")
public class StatsDTO {

    @Schema(description = "Total number of active libraries")
    private long totalLibraries;

    @Schema(description = "Total number of books across all libraries")
    private long totalBooks;

    @Schema(description = "Total number of registered users")
    private long totalUsers;

    @Schema(description = "Total number of active borrows")
    private long activeBorrows;

    @Schema(description = "Total number of users with ACTIVE account status")
    private long activeUsers;
}
