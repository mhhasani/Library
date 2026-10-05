package com.library.dto;

import com.library.validation.SafeText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a library subject")
public record SubjectRequest(
        @NotBlank(message = "نام موضوع را وارد کنید")
        @Size(max = 255, message = "حداکثر ۲۵۵ نویسه مجاز است")
        @SafeText
        @Schema(description = "Subject name", example = "ادبیات")
        String name) {
}
