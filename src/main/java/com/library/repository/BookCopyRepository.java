package com.library.repository;

import com.library.entity.BookCopy;
import com.library.entity.enums.BookCopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findByBookId(Long bookId);
    List<BookCopy> findByBookIdAndLibraryId(Long bookId, Long libraryId);
    List<BookCopy> findByBookIdAndStatus(Long bookId, BookCopyStatus status);
    long countByBookIdAndStatus(Long bookId, BookCopyStatus status);
}
