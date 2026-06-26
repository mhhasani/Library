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
@Schema(description = "Request to upload digital book version")
public class DigitalBookUploadRequest {

    @NotBlank(message = "فرمت فایل را مشخص کنید")
    @Schema(description = "File format (PDF, EPUB, MOBI, AZW3)", example = "PDF")
    private String fileFormat;

    @Schema(description = "Version name/description", example = "English PDF 1st Edition")
    private String versionName;
}
