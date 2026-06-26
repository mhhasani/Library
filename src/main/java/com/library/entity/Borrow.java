package com.library.entity;

import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.BorrowType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "borrows", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_book_id", columnList = "book_id"),
    @Index(name = "idx_book_copy_id", columnList = "book_copy_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_due_date", columnList = "due_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BorrowType borrowType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BorrowStatus status = BorrowStatus.REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "borrow_date")
    private LocalDateTime borrowDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    // ---- Physical delivery workflow fields ----

    /** Snapshot of the delivery address provided at request time. */
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    /** Snapshot of the recipient's internal phone extension. */
    @Column(name = "delivery_extension", length = 50)
    private String deliveryExtension;

    /** Number of loan days requested by the user. */
    @Column(name = "requested_duration_days")
    private Integer requestedDurationDays;

    /** Final number of loan days decided by the librarian. */
    @Column(name = "approved_duration_days")
    private Integer approvedDurationDays;

    /** Date the librarian plans to deliver the book to the recipient. */
    @Column(name = "planned_delivery_date")
    private LocalDateTime plannedDeliveryDate;

    /** Name of the courier delivering the book (may be filled later). */
    @Column(name = "courier_name", length = 255)
    private String courierName;

    /** Unique code of the physical copy dispatched to the recipient. */
    @Column(name = "copy_unique_code", length = 100)
    private String copyUniqueCode;

    /** Moment the recipient confirmed receiving the book (starts the loan clock). */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    // ---- Return-pickup workflow (recipient requests return → librarian schedules pickup) ----

    /** When the recipient asked for the book to be picked up (may be before the due date). */
    @Column(name = "return_requested_at")
    private LocalDateTime returnRequestedAt;

    /** Address where the courier should collect the book from. */
    @Column(name = "return_address", columnDefinition = "TEXT")
    private String returnAddress;

    /** Recipient internal phone number for the return pickup. */
    @Column(name = "return_extension", length = 50)
    private String returnExtension;

    /** Recipient's preferred pickup date/time. */
    @Column(name = "return_preferred_date")
    private LocalDateTime returnPreferredDate;

    /** Courier the librarian assigns to collect the book. */
    @Column(name = "return_courier_name", length = 255)
    private String returnCourierName;

    /** Pickup date/time scheduled by the librarian. */
    @Column(name = "return_planned_date")
    private LocalDateTime returnPlannedDate;

    /** When the recipient handed the book to the courier (in transit back to the library). */
    @Column(name = "handed_over_by_user_at")
    private LocalDateTime handedOverByUserAt;

    /** Librarian who recorded the physical return. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_confirmed_by")
    private User returnConfirmedBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
