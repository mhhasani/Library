package com.library.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_resources", indexes = {
    @Index(name = "idx_checksum", columnList = "checksum_sha256"),
    @Index(name = "idx_uploaded_by", columnList = "uploaded_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 255)
    private String storedFilename;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
