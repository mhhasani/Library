package com.library.repository;

import com.library.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByActorId(Long actorId);

    Optional<AuditLog> findTopByOrderByIdDesc();

    /** Chain verification walks the log in insertion order, one page at a time. */
    Page<AuditLog> findAllByOrderByIdAsc(Pageable pageable);
}
