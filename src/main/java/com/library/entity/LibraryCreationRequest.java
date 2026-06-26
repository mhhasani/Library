package com.library.entity;

import com.library.entity.enums.LibraryRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A user's request to have a new library created. Reviewed by a system admin who may
 * approve (which creates the library with the requester as owner), edit, or reject it.
 */
@Entity
@Table(name = "library_requests", indexes = {
    @Index(name = "idx_libreq_requester", columnList = "requester_id"),
    @Index(name = "idx_libreq_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryCreationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "auto_membership_approval", nullable = false)
    @Builder.Default
    private Boolean autoMembershipApproval = false;

    @Column(name = "default_borrow_duration_days", nullable = false)
    @Builder.Default
    private Integer defaultBorrowDurationDays = 14;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LibraryRequestStatus status = LibraryRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** The library that was created once this request is approved. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_library_id")
    private Library createdLibrary;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
