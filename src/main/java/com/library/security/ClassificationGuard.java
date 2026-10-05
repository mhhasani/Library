package com.library.security;

import com.library.entity.Book;
import com.library.entity.User;
import com.library.entity.enums.ClassificationLevel;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mandatory access control on classified data: a user may read a record only if their
 * clearance dominates the record's classification. Anonymous users are cleared for
 * UNCLASSIFIED only. System administrators already have full system access and are not
 * restricted (they also manage clearances).
 */
@Component
public class ClassificationGuard {

    private final UserRepository userRepository;

    public ClassificationGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ClassificationLevel currentClearance() {
        if (SecurityUtils.hasRole("SYSTEM_ADMIN")) {
            return ClassificationLevel.TOP_SECRET;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ClassificationLevel.UNCLASSIFIED;
        }
        return userRepository.findById(userId).map(User::getClearance).orElse(ClassificationLevel.UNCLASSIFIED);
    }

    /** All classifications the current user may read. */
    public Set<ClassificationLevel> readableLevels() {
        ClassificationLevel clearance = currentClearance();
        return Arrays.stream(ClassificationLevel.values())
                .filter(clearance::dominates)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClassificationLevel.class)));
    }

    /** Query restriction for book listings/searches. */
    public Specification<Book> readableBooks() {
        Set<ClassificationLevel> levels = readableLevels();
        return (root, query, cb) -> root.get("classification").in(levels);
    }

    public boolean canRead(Book book) {
        return currentClearance().dominates(book.getClassification());
    }

    /** Reports an unreadable book as non-existent, so its existence is not disclosed either. */
    public void assertCanRead(Book book) {
        if (!canRead(book)) {
            throw new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + book.getId());
        }
    }

    /** A user can only label data up to their own clearance. */
    public void assertCanAssign(ClassificationLevel level) {
        if (level != null && !currentClearance().dominates(level)) {
            throw new UnauthorizedException("شما مجاز به تعیین این سطح طبقه‌بندی نیستید");
        }
    }
}
