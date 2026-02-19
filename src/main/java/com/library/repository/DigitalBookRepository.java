package com.library.repository;

import com.library.entity.DigitalBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DigitalBookRepository extends JpaRepository<DigitalBook, Long> {
    List<DigitalBook> findByBookId(Long bookId);
    Optional<DigitalBook> findByBookIdAndFileFormat(Long bookId, String fileFormat);
}
