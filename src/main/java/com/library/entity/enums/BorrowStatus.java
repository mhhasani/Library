package com.library.entity.enums;

/**
 * Status of borrow requests
 */
public enum BorrowStatus {
    REQUESTED,  // Initial request by user
    APPROVED,   // Approved by admin (physical: dispatched/awaiting receipt; digital: active)
    RECEIVED,   // Physical only: recipient confirmed receipt, loan clock started
    REJECTED,   // Rejected by admin
    CANCELLED,  // Cancelled by the recipient or the librarian before completion
    RETURNED,   // Successfully returned
    EXPIRED     // Not returned by due date
}
