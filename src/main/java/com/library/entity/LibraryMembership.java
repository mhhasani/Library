package com.library.entity;

import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "library_memberships", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_library_id", columnList = "library_id"),
    @Index(name = "idx_status", columnList = "status")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_library", columnNames = {"user_id", "library_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LibraryMembershipRole role = LibraryMembershipRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

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
