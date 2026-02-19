package com.library.entity.enums;

/**
 * Status of borrow requests
 */
public enum BorrowStatus {
    REQUESTED,  // Initial request by user
    APPROVED,   // Approved by admin
    REJECTED,   // Rejected by admin
    RETURNED,   // Successfully returned
    EXPIRED     // Not returned by due date
}
