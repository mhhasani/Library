package com.library.dto;

import com.library.entity.enums.SensitiveOperation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** Ranges mirror the CHECK constraints of the security_settings table. */
@Schema(description = "New security settings")
public record UpdateSecuritySettingsRequest(
        @Min(value = 1, message = "تعداد تلاش ناموفق باید بین ۱ تا ۶ باشد")
        @Max(value = 6, message = "تعداد تلاش ناموفق باید بین ۱ تا ۶ باشد")
        int maxFailedLogins,

        @Min(value = 1, message = "مدت قفل باید بین ۱ تا ۱۴۴۰ دقیقه باشد")
        @Max(value = 1440, message = "مدت قفل باید بین ۱ تا ۱۴۴۰ دقیقه باشد")
        int lockoutMinutes,

        @Min(value = 1, message = "بازه‌ی شمارش تلاش‌ها باید بین ۱ تا ۱۴۴۰ دقیقه باشد")
        @Max(value = 1440, message = "بازه‌ی شمارش تلاش‌ها باید بین ۱ تا ۱۴۴۰ دقیقه باشد")
        int failureResetMinutes,

        @Min(value = 15, message = "زمان انقضای نشست باید بین ۱۵ تا ۳۰ دقیقه باشد")
        @Max(value = 30, message = "زمان انقضای نشست باید بین ۱۵ تا ۳۰ دقیقه باشد")
        int sessionIdleMinutes,

        @Min(value = 1, message = "تعداد رمزهای قبلی غیرقابل‌تکرار باید بین ۱ تا ۳ باشد")
        @Max(value = 3, message = "تعداد رمزهای قبلی غیرقابل‌تکرار باید بین ۱ تا ۳ باشد")
        int passwordHistory,

        @Min(value = 1, message = "اعتبار رمز عبور باید بین ۱ تا ۳۶۵ روز باشد")
        @Max(value = 365, message = "اعتبار رمز عبور باید بین ۱ تا ۳۶۵ روز باشد")
        int passwordMaxAgeDays,

        boolean mfaRequired,

        @Min(value = 1, message = "مهلت احراز هویت مجدد باید بین ۱ تا ۶۰ دقیقه باشد")
        @Max(value = 60, message = "مهلت احراز هویت مجدد باید بین ۱ تا ۶۰ دقیقه باشد")
        int reauthWindowMinutes,

        @NotNull(message = "فهرست عملیات حساس را مشخص کنید")
        Set<SensitiveOperation> sensitiveOperations) {
}
