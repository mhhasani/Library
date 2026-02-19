package com.library.entity.enums;

/**
 * Physical book copy status
 */
public enum BookCopyStatus {
    AVAILABLE,    // Available for borrowing
    BORROWED,     // Currently borrowed
    LOST,         // Lost or missing
    MAINTENANCE   // Under maintenance
}
