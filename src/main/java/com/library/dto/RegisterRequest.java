package com.library.dto;

import com.library.validation.SafeText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to register a new user")
public class RegisterRequest {

    @Email(message = "یک ایمیل معتبر وارد کنید")
    @NotBlank(message = "ایمیل را وارد کنید")
    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @NotBlank(message = "رمز عبور را وارد کنید")
    @Size(min = 6, message = "رمز عبور باید حداقل ۶ کاراکتر باشد")
    @Schema(description = "Password", example = "password123")
    private String password;

    @NotBlank(message = "نام را وارد کنید")
    @Size(min = 2, max = 100, message = "نام باید بین ۲ تا ۱۰۰ کاراکتر باشد")
    @Schema(description = "First name", example = "John")
    @SafeText
    private String firstName;

    @NotBlank(message = "نام خانوادگی را وارد کنید")
    @Size(min = 2, max = 100, message = "نام خانوادگی باید بین ۲ تا ۱۰۰ کاراکتر باشد")
    @Schema(description = "Last name", example = "Doe")
    @SafeText
    private String lastName;

    @Schema(description = "Phone number (optional)", example = "+1234567890")
    @Size(max = 20, message = "حداکثر ۲۰ نویسه مجاز است")
    @SafeText
    private String phoneNumber;
}
