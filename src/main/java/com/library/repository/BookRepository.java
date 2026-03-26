package com.library.repository;

import com.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByLibraryId(Long libraryId);
    Page<Book> findByLibraryId(Long libraryId, Pageable pageable);
    Page<Book> findByTitleContainingIgnoreCaseAndLibraryId(String title, Long libraryId, Pageable pageable);
    Page<Book> findByAuthorContainingIgnoreCaseAndLibraryId(String author, Long libraryId, Pageable pageable);
    boolean existsByCoverImageId(Long fileResourceId);
}
