package com.library.repository;

import com.library.entity.LibrarySubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibrarySubjectRepository extends JpaRepository<LibrarySubject, Long> {
    List<LibrarySubject> findByLibraryIdOrderByNameAsc(Long libraryId);
    Optional<LibrarySubject> findByLibraryIdAndName(Long libraryId, String name);
    boolean existsByLibraryIdAndName(Long libraryId, String name);
}
