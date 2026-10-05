package com.library.entity.enums;

/**
 * Security classification levels, in ascending order of sensitivity. Used both as a data
 * label (what a record is) and as a user clearance (what a user may see).
 */
public enum ClassificationLevel {
    UNCLASSIFIED("فاقد طبقه‌بندی"),
    CONFIDENTIAL("محرمانه"),
    HIGHLY_CONFIDENTIAL("خیلی محرمانه"),
    SECRET("سری"),
    TOP_SECRET("به‌کلی سری");

    private final String persianLabel;

    ClassificationLevel(String persianLabel) {
        this.persianLabel = persianLabel;
    }

    public String getPersianLabel() {
        return persianLabel;
    }

    /** True if a holder of this clearance may access data labeled {@code label}. */
    public boolean dominates(ClassificationLevel label) {
        return compareTo(label) >= 0;
    }
}
