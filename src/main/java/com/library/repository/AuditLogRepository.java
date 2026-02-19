package com.library.repository;

import com.library.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityType(String entityType);
    List<AuditLog> findByActorId(Long actorId);
}
