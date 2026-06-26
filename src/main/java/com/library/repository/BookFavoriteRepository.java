package com.library.repository;

import com.library.entity.BookFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookFavoriteRepository extends JpaRepository<BookFavorite, Long>, JpaSpecificationExecutor<BookFavorite> {
    Optional<BookFavorite> findByUserIdAndBookId(Long userId, Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    void deleteByUserIdAndBookId(Long userId, Long bookId);

    @Query("select f.book.id from BookFavorite f where f.user.id = :userId")
    List<Long> findBookIdsByUserId(Long userId);
}
