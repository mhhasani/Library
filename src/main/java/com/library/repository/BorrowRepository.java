package com.library.repository;

import com.library.entity.Borrow;
import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.BorrowType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long>, JpaSpecificationExecutor<Borrow> {
    List<Borrow> findByUserId(Long userId);
    List<Borrow> findByBookId(Long bookId);
    List<Borrow> findByBookCopyId(Long bookCopyId);
    List<Borrow> findByStatus(BorrowStatus status);

    @Query("SELECT b FROM Borrow b WHERE b.book.id = :bookId AND b.status = 'REQUESTED' AND b.id <> :excludeBorrowId")
    List<Borrow> findRequestedBorrowsByBookExcluding(@Param("bookId") Long bookId, @Param("excludeBorrowId") Long excludeBorrowId);
    
    @Query("SELECT b FROM Borrow b WHERE b.user.id = :userId AND b.status = 'APPROVED' AND b.returnDate IS NULL")
    List<Borrow> findActiveBorrowsByUser(@Param("userId") Long userId);
    
    @Query("SELECT b FROM Borrow b WHERE b.user.id = :userId AND b.book.id = :bookId AND b.borrowType = :borrowType AND b.status IN ('REQUESTED', 'APPROVED', 'RECEIVED') AND b.returnDate IS NULL")
    List<Borrow> findActiveBorrowByUserAndBookAndType(@Param("userId") Long userId, @Param("bookId") Long bookId, @Param("borrowType") BorrowType borrowType);

    @Query("SELECT b FROM Borrow b WHERE b.dueDate <= :dueDate AND b.status IN ('APPROVED', 'RECEIVED') AND b.returnDate IS NULL")
    List<Borrow> findOverdueBooks(@Param("dueDate") LocalDateTime dueDate);

    /** Physical loans currently in the recipient's hands and past due (for return reminders). */
    @Query("SELECT b FROM Borrow b WHERE b.status = 'RECEIVED' AND b.dueDate IS NOT NULL AND b.dueDate < :now AND b.returnDate IS NULL")
    List<Borrow> findReceivedOverdue(@Param("now") LocalDateTime now);

    List<Borrow> findByLibraryId(Long libraryId);
    List<Borrow> findByLibraryIdAndStatus(Long libraryId, BorrowStatus status);
    long countByStatus(BorrowStatus status);

    /** Most borrowed books in a library: [bookId, bookTitle, borrowCount] */
    @Query("SELECT b.book.id, b.book.title, COUNT(b) AS cnt " +
           "FROM Borrow b WHERE b.library.id = :libraryId " +
           "AND b.status IN ('APPROVED', 'RECEIVED', 'RETURNED') " +
           "AND b.book.deletedAt IS NULL " +
           "GROUP BY b.book.id, b.book.title ORDER BY cnt DESC")
    List<Object[]> findMostBorrowedBooks(@Param("libraryId") Long libraryId, Pageable pageable);

    /** Books never borrowed in a library (excludes soft-deleted books) */
    @Query("SELECT b FROM Book b WHERE b.library.id = :libraryId AND b.deletedAt IS NULL " +
           "AND NOT EXISTS (SELECT br FROM Borrow br WHERE br.book.id = b.id " +
           "               AND br.status IN ('APPROVED', 'RECEIVED', 'RETURNED'))")
    List<com.library.entity.Book> findNeverBorrowedBooks(@Param("libraryId") Long libraryId);

    /** Borrow count per user in a library: [userId, userEmail, borrowCount] */
    @Query("SELECT b.user.id, b.user.email, COUNT(b) AS cnt " +
           "FROM Borrow b WHERE b.library.id = :libraryId " +
           "AND b.status IN ('APPROVED', 'RECEIVED', 'RETURNED') " +
           "GROUP BY b.user.id, b.user.email ORDER BY cnt DESC")
    List<Object[]> findBorrowCountByUser(@Param("libraryId") Long libraryId, Pageable pageable);
}
