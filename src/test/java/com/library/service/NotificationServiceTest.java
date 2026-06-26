package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.NotificationDTO;
import com.library.entity.Notification;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.NotificationType;
import com.library.entity.enums.SystemRole;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Notification Service Tests")
class NotificationServiceTest extends BaseIntegrationTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    private User recipient;
    private User other;

    @BeforeEach
    void setUp() {
        recipient = userRepository.save(User.builder()
                .email("notif-recipient@lib.com").passwordHash("$2a$10$x")
                .firstName("R").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        other = userRepository.save(User.builder()
                .email("notif-other@lib.com").passwordHash("$2a$10$x")
                .firstName("O").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(recipient, "USER");
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("notify() saves notification and returns it")
    void notify_savesNotification() {
        Notification saved = notificationService.notify(
                recipient, NotificationType.PHYSICAL_APPROVED,
                "تأیید عضویت", "عضویت شما تأیید شد",
                "Library", 1L, "/libraries/1");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getType()).isEqualTo(NotificationType.PHYSICAL_APPROVED);
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    @DisplayName("getMyNotifications() returns only current user's notifications")
    void getMyNotifications_returnsOwn() {
        notificationService.notify(recipient, NotificationType.PHYSICAL_APPROVED,
                "تأیید", "متن", "Library", 1L, null);
        notificationService.notify(other, NotificationType.PHYSICAL_REJECTED,
                "امانت", "متن", "Borrow", 2L, null);

        List<NotificationDTO> result = notificationService.getMyNotifications();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(NotificationType.PHYSICAL_APPROVED);
    }

    @Test
    @DisplayName("getUnreadCount() counts only unread for current user")
    void getUnreadCount_correct() {
        notificationService.notify(recipient, NotificationType.PHYSICAL_APPROVED, "t", "m", "L", 1L, null);
        notificationService.notify(recipient, NotificationType.PHYSICAL_REJECTED, "t2", "m2", "L", 2L, null);

        assertThat(notificationService.getUnreadCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("markRead() marks notification as read")
    void markRead_success() {
        Notification n = notificationService.notify(recipient, NotificationType.PHYSICAL_APPROVED,
                "t", "m", "L", 1L, null);

        notificationService.markRead(n.getId());

        Notification updated = notificationRepository.findById(n.getId()).orElseThrow();
        assertThat(updated.isRead()).isTrue();
    }

    @Test
    @DisplayName("markRead() throws when notification not found")
    void markRead_notFound() {
        assertThatThrownBy(() -> notificationService.markRead(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("markRead() throws when notification belongs to another user")
    void markRead_wrongUser_unauthorized() {
        SecurityTestUtils.setSecurityContext(other, "USER");
        Notification n = notificationService.notify(recipient, NotificationType.PHYSICAL_APPROVED,
                "t", "m", "L", 1L, null);
        SecurityTestUtils.setSecurityContext(recipient, "USER");

        // Switch to other user trying to mark recipient's notification
        SecurityTestUtils.setSecurityContext(other, "USER");
        final Long nId = n.getId();
        assertThatThrownBy(() -> notificationService.markRead(nId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("markAllRead() marks all unread as read for current user")
    void markAllRead_success() {
        notificationService.notify(recipient, NotificationType.PHYSICAL_APPROVED, "t", "m", "L", 1L, null);
        notificationService.notify(recipient, NotificationType.PHYSICAL_REJECTED, "t2", "m2", "L", 2L, null);

        notificationService.markAllRead();

        assertThat(notificationService.getUnreadCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("alreadyNotified() returns true when matching notification exists")
    void alreadyNotified_returnsTrue() {
        notificationService.notify(recipient, NotificationType.PHYSICAL_APPROVED,
                "t", "m", "Library", 5L, null);

        boolean result = notificationService.alreadyNotified("Library", 5L, NotificationType.PHYSICAL_APPROVED);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("alreadyNotified() returns false when no matching notification")
    void alreadyNotified_returnsFalse() {
        boolean result = notificationService.alreadyNotified("Library", 99L, NotificationType.PHYSICAL_APPROVED);
        assertThat(result).isFalse();
    }
}
