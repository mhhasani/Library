package com.library.dto;

import com.library.validation.SafeText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create or update a library")
public class LibraryRequest {

    @NotBlank(message = "نام کتابخانه را وارد کنید")
    @Schema(description = "Library name", example = "City Public Library")
    @Size(max = 255, message = "حداکثر ۲۵۵ نویسه مجاز است")
    @SafeText
    private String name;

    @Schema(description = "Library description", example = "A comprehensive public library with digital and physical collections")
    @Size(max = 10000, message = "متن واردشده بیش از حد طولانی است")
    @SafeText
    private String description;

    @Schema(description = "Auto-approve membership requests", example = "false")
    private Boolean autoMembershipApproval = false;

    @Positive(message = "مدت امانت باید بزرگ‌تر از صفر باشد")
    @Schema(description = "Default borrow duration in days", example = "14")
    private Integer defaultBorrowDurationDays = 14;

    @Schema(description = "Active status (used to (re)activate a library)", example = "true")
    private Boolean isActive;

    @Schema(description = "Owner user id — system admin may assign the library owner/admin on direct creation")
    private Long ownerUserId;
}
