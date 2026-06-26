package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Summary of a user's borrowing situation in a library (for librarian review)")
public class BorrowerSummaryDTO {

    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhone;

    private long activePhysical;
    private long activeDigital;
    private long pending;
    private long overdue;
    private long returned;
    private long rejected;
    private long cancelled;
    private long totalBorrows;

    /** Currently dispatched / in-hand loans, to show what the user already holds. */
    private List<BorrowDTO> activeLoans;
}
