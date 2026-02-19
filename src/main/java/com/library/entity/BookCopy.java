package com.library.entity;

import com.library.entity.enums.BookCopyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_copies", indexes = {
    @Index(name = "idx_book_id", columnList = "book_id"),
    @Index(name = "idx_library_id", columnList = "library_id"),
    @Index(name = "idx_status", columnList = "status")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_book_copy_number", columnNames = {"book_id", "copy_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Column(nullable = false)
    private Integer copyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookCopyStatus status = BookCopyStatus.AVAILABLE;

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
