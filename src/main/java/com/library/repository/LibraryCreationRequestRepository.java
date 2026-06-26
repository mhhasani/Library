package com.library.repository;

import com.library.entity.LibraryCreationRequest;
import com.library.entity.enums.LibraryRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibraryCreationRequestRepository extends JpaRepository<LibraryCreationRequest, Long> {
    List<LibraryCreationRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<LibraryCreationRequest> findByStatusOrderByCreatedAtDesc(LibraryRequestStatus status);
    List<LibraryCreationRequest> findAllByOrderByCreatedAtDesc();
}
