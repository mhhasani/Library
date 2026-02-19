package com.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "digital_books", indexes = {
    @Index(name = "idx_book_id", columnList = "book_id"),
    @Index(name = "idx_file_resource_id", columnList = "file_resource_id")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_book_format", columnNames = {"book_id", "file_format"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_resource_id", nullable = false)
    private FileResource fileResource;

    @Column(nullable = false, length = 10)
    private String fileFormat;  // PDF, EPUB, MOBI, AZW3

    @Column(name = "version_name", length = 50)
    private String versionName;

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
