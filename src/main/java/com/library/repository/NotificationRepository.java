package com.library.repository;

import com.library.entity.Notification;
import com.library.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /** Used to avoid emitting duplicate notifications for the same entity event (e.g. return reminders). */
    boolean existsByRelatedEntityTypeAndRelatedEntityIdAndType(
            String relatedEntityType, Long relatedEntityId, NotificationType type);
}
