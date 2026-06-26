package com.library.service;

import com.library.dto.NotificationDTO;
import com.library.entity.Notification;
import com.library.entity.User;
import com.library.entity.enums.NotificationType;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /** Create a notification for a recipient. Title/message are pre-built by the caller. */
    public Notification notify(User recipient, NotificationType type, String title, String message,
                               String relatedEntityType, Long relatedEntityId, String link) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .link(link)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    public boolean alreadyNotified(String relatedEntityType, Long relatedEntityId, NotificationType type) {
        return notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndType(
                relatedEntityType, relatedEntityId, type);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<NotificationDTO> getMyNotificationsPaged(
            org.springframework.data.domain.Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    public void markRead(Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("اعلان پیدا نشد"));
        if (!n.getRecipient().getId().equals(userId)) {
            throw new UnauthorizedException("شما به اعلان‌های کاربر دیگری دسترسی ندارید");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    public void markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Notification> unread = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationDTO toDto(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .link(n.getLink())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
