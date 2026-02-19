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
    @Index(name = "idx_digital_book_id", columnList = "digital_book_id"),
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "digital_book_id")
    private DigitalBook digitalBook;

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
