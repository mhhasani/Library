package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to approve or reject a borrow")
public class BorrowApprovalRequest {

    @NotBlank(message = "Action cannot be blank")
    @Schema(description = "Action: APPROVE or REJECT", example = "APPROVE")
    private String action;

    @Schema(description = "Rejection reason (required if action is REJECT)")
    private String rejectionReason;
}
