package com.library.repository;

import com.library.entity.LibraryMembership;
import com.library.entity.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryMembershipRepository extends JpaRepository<LibraryMembership, Long> {
    Optional<LibraryMembership> findByUserIdAndLibraryId(Long userId, Long libraryId);
    List<LibraryMembership> findByLibraryId(Long libraryId);
    List<LibraryMembership> findByUserId(Long userId);
    List<LibraryMembership> findByLibraryIdAndStatus(Long libraryId, MembershipStatus status);
}
