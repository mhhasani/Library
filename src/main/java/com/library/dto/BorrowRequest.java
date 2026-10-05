package com.library.dto;

import com.library.entity.enums.BorrowType;
import com.library.validation.SafeText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to borrow a book")
public class BorrowRequest {

    @NotNull(message = "نوع امانت را مشخص کنید")
    @Schema(description = "Type of borrow (PHYSICAL or DIGITAL)", example = "PHYSICAL")
    private BorrowType borrowType;

    @Schema(description = "Book copy ID (optional for PHYSICAL borrow, auto-selected if omitted)", example = "1")
    private Long bookCopyId;

    // ---- Physical delivery fields (required for PHYSICAL) ----

    @Schema(description = "Delivery address where the recipient will receive the book",
            example = "ساختمان فناوری اطلاعات، طبقه دوم، اتاق ۱۱۲")
    @Size(max = 2000, message = "نشانی بیش از حد طولانی است")
    @SafeText
    private String deliveryAddress;

    @Schema(description = "Recipient internal phone number (8 digits)", example = "12345678")
    @Size(max = 50, message = "حداکثر ۵۰ نویسه مجاز است")
    @SafeText
    private String deliveryExtension;

    @Schema(description = "Number of loan days requested by the user", example = "7")
    @Positive(message = "مدت امانت باید بزرگ‌تر از صفر باشد")
    private Integer requestedDurationDays;

    @Schema(description = "Also save the delivery address/extension to the user profile as defaults")
    private Boolean saveToProfile;
}
