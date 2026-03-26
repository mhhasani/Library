package com.library.repository;

import com.library.entity.Borrow;
import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.BorrowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {
    List<Borrow> findByUserId(Long userId);
    List<Borrow> findByBookId(Long bookId);
    List<Borrow> findByBookCopyId(Long bookCopyId);
    List<Borrow> findByDigitalBookId(Long digitalBookId);
    List<Borrow> findByStatus(BorrowStatus status);
    
    @Query("SELECT b FROM Borrow b WHERE b.user.id = :userId AND b.status = 'APPROVED' AND b.returnDate IS NULL")
    List<Borrow> findActiveBorrowsByUser(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Borrow b WHERE b.user.id = :userId AND b.book.id = :bookId AND b.borrowType = :borrowType AND b.status IN ('REQUESTED', 'APPROVED') AND b.returnDate IS NULL")
    List<Borrow> findActivePhysicalBorrowByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("borrowType") BorrowType borrowType);
    
    @Query("SELECT b FROM Borrow b WHERE b.dueDate <= :dueDate AND b.status = 'APPROVED' AND b.returnDate IS NULL")
    List<Borrow> findOverdueBooks(@Param("dueDate") LocalDateTime dueDate);

    List<Borrow> findByLibraryId(Long libraryId);
    List<Borrow> findByLibraryIdAndStatus(Long libraryId, BorrowStatus status);
    long countByStatus(BorrowStatus status);
}
