package com.library.dto;

import com.library.validation.SafeText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update user profile")
public class UpdateProfileRequest {

    @NotBlank(message = "نام را وارد کنید")
    @Size(max = 100)
    @Schema(description = "First name", example = "علی")
    @SafeText
    private String firstName;

    @NotBlank(message = "نام خانوادگی را وارد کنید")
    @Size(max = 100)
    @Schema(description = "Last name", example = "احمدی")
    @SafeText
    private String lastName;

    @Size(max = 20)
    @Schema(description = "Phone number", example = "09123456789")
    @SafeText
    private String phoneNumber;

    @Schema(description = "Default delivery address", example = "ساختمان فناوری اطلاعات، طبقه دوم، اتاق ۱۱۲")
    @Size(max = 2000, message = "نشانی بیش از حد طولانی است")
    @SafeText
    private String deliveryAddress;

    @Pattern(regexp = "^(\\d{8})?$", message = "شماره تلفن داخلی باید ۸ رقم باشد")
    @Schema(description = "Internal phone number (8 digits)", example = "12345678")
    private String internalExtension;
}
