package com.library.service;

import com.library.entity.Book;
import com.library.entity.Borrow;
import com.library.entity.Library;
import com.library.entity.User;
import com.library.entity.enums.NotificationType;
import com.library.repository.BorrowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Borrow Reminder Scheduler Tests")
class BorrowReminderSchedulerTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private NotificationService notificationService;

    private BorrowReminderScheduler scheduler;

    private Borrow overdueBorrow;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new BorrowReminderScheduler();
        ReflectionTestUtils.setField(scheduler, "borrowRepository", borrowRepository);
        ReflectionTestUtils.setField(scheduler, "notificationService", notificationService);

        User user = User.builder().id(1L).firstName("علی").lastName("احمدی").build();
        Book book = Book.builder().id(1L).title("کتاب تست").build();
        Library library = Library.builder().id(1L).build();

        overdueBorrow = Borrow.builder()
                .id(10L)
                .user(user)
                .book(book)
                .library(library)
                .dueDate(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Test
    @DisplayName("Sends RETURN_DUE notification for overdue borrow not yet notified")
    void sendReturnDueReminders_notifiesWhenNotAlreadyNotified() {
        when(borrowRepository.findReceivedOverdue(any(LocalDateTime.class)))
                .thenReturn(List.of(overdueBorrow));
        when(notificationService.alreadyNotified("BORROW", 10L, NotificationType.RETURN_DUE))
                .thenReturn(false);

        scheduler.sendReturnDueReminders();

        verify(notificationService, times(1)).notify(
                eq(overdueBorrow.getUser()),
                eq(NotificationType.RETURN_DUE),
                anyString(),
                anyString(),
                eq("BORROW"),
                eq(10L),
                anyString());
    }

    @Test
    @DisplayName("Skips notification when borrow was already notified (duplicate guard)")
    void sendReturnDueReminders_skipsWhenAlreadyNotified() {
        when(borrowRepository.findReceivedOverdue(any(LocalDateTime.class)))
                .thenReturn(List.of(overdueBorrow));
        when(notificationService.alreadyNotified("BORROW", 10L, NotificationType.RETURN_DUE))
                .thenReturn(true);

        scheduler.sendReturnDueReminders();

        verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Does nothing when there are no overdue borrows")
    void sendReturnDueReminders_noOverdueBorrows() {
        when(borrowRepository.findReceivedOverdue(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.sendReturnDueReminders();

        verify(notificationService, never()).alreadyNotified(any(), any(), any());
        verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any(), any());
    }
}
