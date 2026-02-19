package com.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "books", indexes = {
    @Index(name = "idx_library_id", columnList = "library_id"),
    @Index(name = "idx_title", columnList = "title"),
    @Index(name = "idx_author", columnList = "author")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(length = 255)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean autoDigitalBorrowEnabled = false;

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
