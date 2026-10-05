package com.library.repository;

import com.library.entity.Book;
import com.library.entity.enums.ClassificationLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findByLibraryId(Long libraryId);
    Page<Book> findByLibraryId(Long libraryId, Pageable pageable);
    Page<Book> findByTitleContainingIgnoreCaseAndLibraryId(String title, Long libraryId, Pageable pageable);
    Page<Book> findByAuthorContainingIgnoreCaseAndLibraryId(String author, Long libraryId, Pageable pageable);
    boolean existsByCoverImageId(Long fileResourceId);

    /** Listings restricted to the classifications the caller is cleared for. */
    Page<Book> findByLibraryIdAndDeletedAtIsNullAndClassificationIn(
            Long libraryId, Collection<ClassificationLevel> classifications, Pageable pageable);
    Page<Book> findByLibraryIdAndDeletedAtIsNotNullAndClassificationIn(
            Long libraryId, Collection<ClassificationLevel> classifications, Pageable pageable);

    List<Book> findByCoverImageId(Long fileResourceId);

    @Query("SELECT COUNT(b) FROM Book b JOIN b.subjects s WHERE s.id = :subjectId")
    long countBySubjectId(@Param("subjectId") Long subjectId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM book_subjects WHERE subject_id = :subjectId", nativeQuery = true)
    void removeSubjectFromAllBooks(@Param("subjectId") Long subjectId);
}
