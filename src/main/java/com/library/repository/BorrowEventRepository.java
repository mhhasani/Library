package com.library.repository;

import com.library.entity.BorrowEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowEventRepository extends JpaRepository<BorrowEvent, Long> {
    List<BorrowEvent> findByBorrowIdOrderByCreatedAtAsc(Long borrowId);
}
