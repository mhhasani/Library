package com.library.entity.enums;

/**
 * Operations an administrator can mark as "sensitive": performing one requires a recent
 * re-authentication (fresh password + second factor) within the configured window.
 */
public enum SensitiveOperation {
    USER_ROLE_CHANGE("تغییر نقش سیستمی کاربران"),
    USER_STATUS_CHANGE("تعلیق یا فعال‌سازی کاربران"),
    USER_CLEARANCE_CHANGE("تغییر سطح دسترسی طبقه‌بندی کاربران"),
    USER_PASSWORD_RESET("تنظیم رمز عبور موقت برای کاربران"),
    SECURITY_SETTINGS_CHANGE("تغییر تنظیمات امنیتی"),
    AUDIT_LOG_EXPORT("دریافت خروجی رویدادنگاری"),
    LIBRARY_DELETE("حذف کتابخانه"),
    LIBRARY_ROLE_CHANGE("تغییر نقش اعضای کتابخانه"),
    BOOK_DELETE("حذف کتاب");

    private final String persianLabel;

    SensitiveOperation(String persianLabel) {
        this.persianLabel = persianLabel;
    }

    public String getPersianLabel() {
        return persianLabel;
    }
}
