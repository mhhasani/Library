package com.library.service;

import com.library.entity.Borrow;
import com.library.entity.enums.NotificationType;
import com.library.repository.BorrowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Emits "please return the book" notifications once a physical loan passes its due date.
 * Runs hourly; a duplicate guard ensures each loan is reminded at most once.
 */
@Slf4j
@Component
public class BorrowReminderScheduler {

    private static final String ENTITY_BORROW = "BORROW";

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *") // top of every hour
    @Transactional
    public void sendReturnDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Borrow> overdue = borrowRepository.findReceivedOverdue(now);
        for (Borrow borrow : overdue) {
            if (notificationService.alreadyNotified(ENTITY_BORROW, borrow.getId(), NotificationType.RETURN_DUE)) {
                continue;
            }
            notificationService.notify(borrow.getUser(), NotificationType.RETURN_DUE,
                    "پایان مهلت امانت",
                    String.format("مهلت امانت کتاب «%s» به پایان رسیده است. لطفاً کتاب را بازگردانید.",
                            borrow.getBook().getTitle()),
                    ENTITY_BORROW, borrow.getId(),
                    "/libraries/" + borrow.getLibrary().getId() + "/borrows/physical");
            log.info("RETURN_DUE reminder created for borrow {}", borrow.getId());
        }
    }
}
