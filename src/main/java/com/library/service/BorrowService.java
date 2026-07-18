package com.library.service;

import com.library.dto.BorrowDTO;
import com.library.dto.BorrowRequest;
import com.library.dto.DeliveryDetailsRequest;
import com.library.dto.PhysicalApprovalRequest;
import com.library.dto.ReturnRequest;
import com.library.dto.ReturnScheduleRequest;
import com.library.entity.*;
import com.library.entity.enums.*;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.*;
import com.library.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class BorrowService {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private DigitalBookRepository digitalBookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.library.repository.BorrowEventRepository borrowEventRepository;

    private static final String ENTITY_BORROW = "BORROW";

    /** Append an immutable timeline entry for a borrow, attributed to the current user (or "سیستم"). */
    private void logEvent(Borrow borrow, String title, String detail) {
        String actor = "سیستم";
        try {
            Long uid = SecurityUtils.getCurrentUserId();
            User u = userRepository.findById(uid).orElse(null);
            if (u != null) actor = fullName(u);
        } catch (Exception ignored) { /* no auth context (e.g. scheduler) */ }
        borrowEventRepository.save(com.library.entity.BorrowEvent.builder()
                .borrow(borrow).title(title).detail(detail).actorName(actor)
                .createdAt(LocalDateTime.now()).build());
    }

    /** Full event timeline of a borrow (admin only). */
    @Transactional(readOnly = true)
    public List<com.library.dto.BorrowEventDTO> getBorrowEvents(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        requireAdmin(currentUserId, libraryId);
        return borrowEventRepository.findByBorrowIdOrderByCreatedAtAsc(borrowId).stream()
                .map(e -> com.library.dto.BorrowEventDTO.builder()
                        .id(e.getId()).title(e.getTitle()).detail(e.getDetail())
                        .actorName(e.getActorName()).createdAt(e.getCreatedAt()).build())
                .collect(Collectors.toList());
    }

    public BorrowDTO createBorrowRequest(Long libraryId, Long bookId, BorrowRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("کاربر فعلی پیدا نشد"));

        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }

        // Check user membership
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (!membership.getStatus().equals(MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("عضویت شما هنوز تأیید نشده است");
        }

        Borrow borrow;

        if (request.getBorrowType() == BorrowType.PHYSICAL) {
            return createPhysicalBorrow(user, library, book, request);
        } else if (request.getBorrowType() == BorrowType.DIGITAL) {
            return createDigitalBorrow(user, library, book, request);
        } else {
            throw new BadRequestException("نوع امانت نامعتبر است");
        }
    }

    private BorrowDTO createPhysicalBorrow(User user, Library library, Book book, BorrowRequest request) {
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
            throw new BadRequestException("آدرس تحویل الزامی است");
        }
        if (request.getDeliveryExtension() != null && !request.getDeliveryExtension().isBlank()
                && !request.getDeliveryExtension().matches("\\d{8}")) {
            throw new BadRequestException("شماره تلفن داخلی باید ۸ رقم باشد");
        }

        BookCopy bookCopy;

        if (request.getBookCopyId() != null) {
            bookCopy = bookCopyRepository.findById(request.getBookCopyId())
                    .orElseThrow(() -> new ResourceNotFoundException("نسخه‌ی کتاب پیدا نشد"));
            if (!bookCopy.getBook().getId().equals(book.getId())) {
                throw new BadRequestException("این نسخه مربوط به این کتاب نیست");
            }
            if (bookCopy.getStatus() != BookCopyStatus.AVAILABLE) {
                throw new BadRequestException("این نسخه در دسترس نیست");
            }
        } else {
            // Auto-select first available copy
            bookCopy = bookCopyRepository.findByBookIdAndStatus(book.getId(), BookCopyStatus.AVAILABLE)
                    .stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("در حال حاضر نسخه‌ی موجودی از این کتاب نیست"));
        }

        // Check user doesn't already have an active physical borrow of this book
        List<Borrow> activeBorrows = borrowRepository.findActiveBorrowByUserAndBookAndType(
                user.getId(), book.getId(), BorrowType.PHYSICAL);
        if (!activeBorrows.isEmpty()) {
            throw new BadRequestException("شما در حال حاضر یک امانت فعال از این کتاب دارید");
        }

        // Optionally persist the delivery details as profile defaults
        if (Boolean.TRUE.equals(request.getSaveToProfile())) {
            user.setDeliveryAddress(request.getDeliveryAddress());
            user.setInternalExtension(request.getDeliveryExtension());
            userRepository.save(user);
        }

        Borrow borrow = Borrow.builder()
                .user(user)
                .library(library)
                .book(book)
                .borrowType(BorrowType.PHYSICAL)
                .bookCopy(bookCopy)
                .status(BorrowStatus.REQUESTED)
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryExtension(request.getDeliveryExtension())
                .requestedDurationDays(request.getRequestedDurationDays())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        borrow = borrowRepository.save(borrow);
        log.info("Physical borrow request created: {} by user: {}", borrow.getId(), user.getEmail());

        logEvent(borrow, "ثبت درخواست",
                String.format("درخواست امانت ثبت شد (مدت درخواستی: %s روز).",
                        request.getRequestedDurationDays() != null ? request.getRequestedDurationDays() : "—"));
        notifyLibraryAdmins(borrow, NotificationType.NEW_PHYSICAL_REQUEST,
                "درخواست امانت جدید",
                String.format("کاربر %s درخواست امانت کتاب «%s» را ثبت کرد.",
                        fullName(user), book.getTitle()));

        return mapToBorrowDTO(borrow);
    }

    private BorrowDTO createDigitalBorrow(User user, Library library, Book book, BorrowRequest request) {
        // Digital borrow is per-book: check the book has at least one digital version
        boolean hasDigital = !digitalBookRepository.findByBookId(book.getId()).isEmpty();
        if (!hasDigital) {
            throw new BadRequestException("این کتاب نسخه‌ی دیجیتال ندارد");
        }

        // Check user doesn't already have an active digital borrow for this book
        List<Borrow> existingBorrows = borrowRepository.findActiveBorrowByUserAndBookAndType(
                user.getId(), book.getId(), BorrowType.DIGITAL);
        if (!existingBorrows.isEmpty()) {
            throw new BadRequestException("شما در حال حاضر یک دانلود فعال از این کتاب دارید");
        }

        // Digital access is instant: it needs no librarian approval — it is simply granted and logged.
        Borrow borrow = Borrow.builder()
                .user(user)
                .library(library)
                .book(book)
                .borrowType(BorrowType.DIGITAL)
                .status(BorrowStatus.APPROVED)
                .approvedBy(library.getOwner())
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(library.getDefaultBorrowDurationDays()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        borrow = borrowRepository.save(borrow);
        log.info("Digital borrow request created: {} by user: {}", borrow.getId(), user.getEmail());
        logEvent(borrow, "دسترسی دیجیتال", "دسترسی دیجیتال به‌صورت آنی صادر شد.");

        return mapToBorrowDTO(borrow);
    }

    /** Allow the requester to edit delivery details of a PHYSICAL borrow while it is still REQUESTED. */
    public BorrowDTO updatePhysicalRequest(Long libraryId, Long borrowId, BorrowRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("کاربر فعلی پیدا نشد"));

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        if (!borrow.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("فقط می‌توانید درخواست‌های خودتان را ویرایش کنید");
        }
        if (borrow.getBorrowType() != BorrowType.PHYSICAL) {
            throw new BadRequestException("فقط درخواست‌های فیزیکی قابل ویرایش هستند");
        }
        if (borrow.getStatus() != BorrowStatus.REQUESTED) {
            throw new BadRequestException("فقط درخواست‌های تأییدنشده قابل ویرایش هستند");
        }
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
            throw new BadRequestException("آدرس تحویل الزامی است");
        }
        if (request.getDeliveryExtension() != null && !request.getDeliveryExtension().isBlank()
                && !request.getDeliveryExtension().matches("\\d{8}")) {
            throw new BadRequestException("شماره تلفن داخلی باید ۸ رقم باشد");
        }

        borrow.setDeliveryAddress(request.getDeliveryAddress());
        borrow.setDeliveryExtension(request.getDeliveryExtension());
        borrow.setRequestedDurationDays(request.getRequestedDurationDays());
        borrow.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(request.getSaveToProfile())) {
            user.setDeliveryAddress(request.getDeliveryAddress());
            user.setInternalExtension(request.getDeliveryExtension());
            userRepository.save(user);
        }

        borrow = borrowRepository.save(borrow);
        log.info("Physical borrow request {} edited by user {}", borrowId, currentUserId);
        logEvent(borrow, "ویرایش درخواست", "گیرنده اطلاعات تحویل درخواست را ویرایش کرد.");
        return mapToBorrowDTO(borrow);
    }

    public BorrowDTO approveBorrowRequest(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));

        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }

        // Check if user is library admin
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند درخواست‌های امانت را تأیید کند");
        }

        if (borrow.getStatus() != BorrowStatus.REQUESTED) {
            throw new BadRequestException("فقط درخواست‌های در انتظار را می‌توان تأیید کرد");
        }

        if (borrow.getBorrowType() == BorrowType.PHYSICAL) {
            throw new BadRequestException("برای تأیید امانت از مسیر تأیید با جزئیات تحویل استفاده کنید");
        }

        User admin = userRepository.findById(currentUserId).orElseThrow();

        borrow.setStatus(BorrowStatus.APPROVED);
        borrow.setApprovedBy(admin);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setDueDate(LocalDateTime.now().plusDays(borrow.getLibrary().getDefaultBorrowDurationDays()));
        borrow.setUpdatedAt(LocalDateTime.now());

        borrow = borrowRepository.save(borrow);
        log.info("Borrow request approved: {} by admin: {}", borrowId, admin.getEmail());

        return mapToBorrowDTO(borrow);
    }

    public BorrowDTO reserveBook(Long libraryId, Long bookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("کاربر فعلی پیدا نشد"));

        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه پیدا نشد"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتاب پیدا نشد"));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (!membership.getStatus().equals(MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("عضویت شما هنوز تأیید نشده است");
        }

        long availableCopies = bookCopyRepository.countByBookIdAndStatus(bookId, BookCopyStatus.AVAILABLE);
        if (availableCopies > 0) {
            throw new BadRequestException("کتاب موجود است. از گزینه امانت استفاده کنید.");
        }

        List<Borrow> existing = borrowRepository.findActiveBorrowByUserAndBookAndType(
                currentUserId, bookId, BorrowType.PHYSICAL);
        if (!existing.isEmpty()) {
            throw new BadRequestException("شما قبلاً برای این کتاب درخواست فعال دارید.");
        }

        Borrow borrow = Borrow.builder()
                .user(user)
                .library(library)
                .book(book)
                .borrowType(BorrowType.PHYSICAL)
                .bookCopy(null) // no copy assigned yet
                .status(BorrowStatus.REQUESTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        borrow = borrowRepository.save(borrow);
        log.info("Book {} reserved by user {} in library {}", bookId, currentUserId, libraryId);
        return mapToBorrowDTO(borrow);
    }

    public BorrowDTO rejectBorrowRequest(Long libraryId, Long borrowId, String rejectionReason) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));

        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند درخواست‌های امانت را رد کند");
        }

        if (borrow.getStatus() != BorrowStatus.REQUESTED) {
            throw new BadRequestException("فقط درخواست‌های در انتظار را می‌توان رد کرد");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BadRequestException("ذکر دلیل رد الزامی است");
        }

        User admin = userRepository.findById(currentUserId).orElseThrow();
        borrow.setStatus(BorrowStatus.REJECTED);
        borrow.setRejectedBy(admin);
        borrow.setRejectionReason(rejectionReason);
        borrow.setUpdatedAt(LocalDateTime.now());

        borrow = borrowRepository.save(borrow);
        log.info("Borrow request rejected: {} by admin: {}", borrowId, admin.getEmail());
        logEvent(borrow, "رد درخواست", "درخواست رد شد. دلیل: " + rejectionReason);

        if (borrow.getBorrowType() == BorrowType.PHYSICAL) {
            String reasonText = (rejectionReason != null && !rejectionReason.isBlank())
                    ? " دلیل: " + rejectionReason : "";
            notifyUser(borrow, NotificationType.PHYSICAL_REJECTED,
                    "درخواست امانت رد شد",
                    String.format("درخواست امانت کتاب «%s» رد شد.%s",
                            borrow.getBook().getTitle(), reasonText));
        }

        return mapToBorrowDTO(borrow);
    }

    /**
     * Approve a PHYSICAL borrow request with full delivery details. Sets the planned delivery date,
     * the finalized loan duration, the dispatched copy code and (optionally) the courier. The loan
     * clock (dueDate) is NOT started here — it starts when the recipient confirms receipt.
     */
    public BorrowDTO approvePhysicalBorrow(Long libraryId, Long borrowId, PhysicalApprovalRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));

        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        requireAdmin(currentUserId, libraryId);

        if (borrow.getBorrowType() != BorrowType.PHYSICAL) {
            throw new BadRequestException("این مسیر فقط برای امانت است");
        }
        if (borrow.getStatus() != BorrowStatus.REQUESTED) {
            throw new BadRequestException("فقط درخواست‌های در انتظار را می‌توان تأیید کرد");
        }

        User admin = userRepository.findById(currentUserId).orElseThrow();

        // Resolve the copy to dispatch — ALWAYS end with a currently AVAILABLE copy to prevent
        // double-allocation (a copy auto-assigned at request time may have been taken since).
        if (request.getBookCopyId() != null) {
            // Librarian explicitly chose a copy → it must belong to the book and be available
            BookCopy copy = bookCopyRepository.findById(request.getBookCopyId())
                    .orElseThrow(() -> new ResourceNotFoundException("نسخه‌ی کتاب پیدا نشد"));
            if (!copy.getBook().getId().equals(borrow.getBook().getId())) {
                throw new BadRequestException("این نسخه مربوط به این کتاب نیست");
            }
            if (copy.getStatus() != BookCopyStatus.AVAILABLE) {
                throw new BadRequestException("این نسخه در دسترس نیست");
            }
            borrow.setBookCopy(copy);
        } else if (borrow.getBookCopy() != null && borrow.getBookCopy().getStatus() == BookCopyStatus.AVAILABLE) {
            // Pre-assigned copy is still free → keep it
            // (no change needed)
        } else {
            // No copy, or the pre-assigned one is no longer available → pick a free one
            BookCopy availableCopy = bookCopyRepository.findByBookIdAndStatus(
                    borrow.getBook().getId(), BookCopyStatus.AVAILABLE)
                    .stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("هنوز هیچ نسخه‌ای از این کتاب موجود نیست."));
            borrow.setBookCopy(availableCopy);
        }

        borrow.setStatus(BorrowStatus.APPROVED);
        borrow.setApprovedBy(admin);
        borrow.setPlannedDeliveryDate(request.getPlannedDeliveryDate());
        borrow.setApprovedDurationDays(request.getApprovedDurationDays());
        borrow.setCopyUniqueCode(request.getCopyUniqueCode());
        borrow.setCourierName(request.getCourierName());
        borrow.setUpdatedAt(LocalDateTime.now());

        // Mark the copy as borrowed and auto-reject leftover requests if stock is gone
        BookCopy copy = borrow.getBookCopy();
        copy.setStatus(BookCopyStatus.BORROWED);
        copy.setUpdatedAt(LocalDateTime.now());
        bookCopyRepository.save(copy);

        long availableCopies = bookCopyRepository.countByBookIdAndStatus(borrow.getBook().getId(), BookCopyStatus.AVAILABLE);
        if (availableCopies == 0) {
            Long bookId = borrow.getBook().getId();
            List<Borrow> pendingBorrows = borrowRepository.findRequestedBorrowsByBookExcluding(bookId, borrowId);
            for (Borrow pending : pendingBorrows) {
                pending.setStatus(BorrowStatus.REJECTED);
                pending.setRejectionReason("رد خودکار: موجودی کتاب به پایان رسید");
                pending.setUpdatedAt(LocalDateTime.now());
                borrowRepository.save(pending);
                if (pending.getBorrowType() == BorrowType.PHYSICAL) {
                    notifyUser(pending, NotificationType.PHYSICAL_REJECTED,
                            "درخواست امانت رد شد",
                            String.format("درخواست امانت کتاب «%s» رد شد. دلیل: موجودی کتاب به پایان رسید.",
                                    pending.getBook().getTitle()));
                }
            }
        }

        borrow = borrowRepository.save(borrow);
        log.info("Physical borrow {} approved by admin {}", borrowId, admin.getEmail());
        logEvent(borrow, "تأیید کتابدار", String.format(
                "درخواست تأیید شد (مدت: %s روز، کد نسخه: %s، پیک: %s).",
                borrow.getApprovedDurationDays(),
                borrow.getCopyUniqueCode() != null ? borrow.getCopyUniqueCode() : "—",
                borrow.getCourierName() != null ? borrow.getCourierName() : "—"));

        notifyUser(borrow, NotificationType.PHYSICAL_APPROVED,
                "درخواست امانت تأیید شد",
                String.format("درخواست امانت کتاب «%s» تأیید شد (%d روز). پس از دریافت، گزینهٔ «کتاب را دریافت کردم» را بزنید.",
                        borrow.getBook().getTitle(), borrow.getApprovedDurationDays()));
        if (borrow.getCourierName() != null && !borrow.getCourierName().isBlank()) {
            notifyUser(borrow, NotificationType.BOOK_DISPATCHED,
                    "کتاب در مسیر تحویل است",
                    String.format("کتاب «%s» توسط پیک %s برای شما ارسال می‌شود.",
                            borrow.getBook().getTitle(), borrow.getCourierName()));
        }

        return mapToBorrowDTO(borrow);
    }

    /** Set or update courier / dispatch details after approval. Emits a dispatch notice the first time a courier is set. */
    public BorrowDTO updateDeliveryDetails(Long libraryId, Long borrowId, DeliveryDetailsRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        requireAdmin(currentUserId, libraryId);

        if (borrow.getBorrowType() != BorrowType.PHYSICAL) {
            throw new BadRequestException("این عملیات فقط برای امانت است");
        }
        if (borrow.getStatus() != BorrowStatus.APPROVED) {
            throw new BadRequestException("فقط برای امانت‌های تأییدشده می‌توان جزئیات تحویل را ویرایش کرد");
        }

        boolean courierWasEmpty = (borrow.getCourierName() == null || borrow.getCourierName().isBlank());

        if (request.getCourierName() != null) {
            borrow.setCourierName(request.getCourierName());
        }
        if (request.getCopyUniqueCode() != null) {
            borrow.setCopyUniqueCode(request.getCopyUniqueCode());
        }
        if (request.getPlannedDeliveryDate() != null) {
            borrow.setPlannedDeliveryDate(request.getPlannedDeliveryDate());
        }
        borrow.setUpdatedAt(LocalDateTime.now());
        borrow = borrowRepository.save(borrow);

        boolean courierNowSet = borrow.getCourierName() != null && !borrow.getCourierName().isBlank();
        if (courierWasEmpty && courierNowSet) {
            notifyUser(borrow, NotificationType.BOOK_DISPATCHED,
                    "کتاب در مسیر تحویل است",
                    String.format("کتاب «%s» توسط پیک %s برای شما ارسال می‌شود.",
                            borrow.getBook().getTitle(), borrow.getCourierName()));
        }
        log.info("Delivery details updated for borrow {}", borrowId);
        logEvent(borrow, "به‌روزرسانی تحویل", String.format(
                "جزئیات تحویل به‌روزرسانی شد (پیک: %s، کد نسخه: %s).",
                borrow.getCourierName() != null ? borrow.getCourierName() : "—",
                borrow.getCopyUniqueCode() != null ? borrow.getCopyUniqueCode() : "—"));
        return mapToBorrowDTO(borrow);
    }

    /** Recipient confirms receiving the physical book. Starts the loan clock from now. */
    public BorrowDTO confirmReceipt(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("رکورد امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        if (!borrow.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("فقط می‌توانید امانت‌های خودتان را تأیید کنید");
        }
        if (borrow.getBorrowType() != BorrowType.PHYSICAL) {
            throw new BadRequestException("این عملیات فقط برای امانت است");
        }
        if (borrow.getStatus() != BorrowStatus.APPROVED) {
            throw new BadRequestException("فقط امانت تأییدشده را می‌توان دریافت‌شده اعلام کرد");
        }

        LocalDateTime now = LocalDateTime.now();
        int days = borrow.getApprovedDurationDays() != null
                ? borrow.getApprovedDurationDays()
                : borrow.getLibrary().getDefaultBorrowDurationDays();
        borrow.setStatus(BorrowStatus.RECEIVED);
        borrow.setReceivedAt(now);
        borrow.setBorrowDate(now);
        borrow.setDueDate(now.plusDays(days));
        borrow.setUpdatedAt(now);
        borrow = borrowRepository.save(borrow);
        log.info("Receipt confirmed for borrow {} by user {}", borrowId, currentUserId);
        logEvent(borrow, "دریافت کتاب", String.format("گیرنده دریافت کتاب را تأیید کرد؛ مهلت برگرداندن: %d روز.", days));

        if (borrow.getApprovedBy() != null) {
            notificationService.notify(borrow.getApprovedBy(), NotificationType.RECEIPT_CONFIRMED,
                    "دریافت کتاب تأیید شد",
                    String.format("%s دریافت کتاب «%s» را تأیید کرد. مهلت برگرداندن: %d روز.",
                            fullName(borrow.getUser()), borrow.getBook().getTitle(), days),
                    ENTITY_BORROW, borrow.getId(), adminBorrowLink(borrow));
        }
        return mapToBorrowDTO(borrow);
    }

    /** Librarian records that the physical book has been returned, ending the loan. */
    public BorrowDTO confirmReturnByLibrarian(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("رکورد امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        requireAdmin(currentUserId, libraryId);

        if (borrow.getBorrowType() != BorrowType.PHYSICAL) {
            throw new BadRequestException("این عملیات فقط برای امانت است");
        }
        if (borrow.getStatus() != BorrowStatus.RECEIVED && borrow.getStatus() != BorrowStatus.APPROVED) {
            throw new BadRequestException("فقط امانت فعال را می‌توان تحویل‌گرفته ثبت کرد");
        }

        User admin = userRepository.findById(currentUserId).orElseThrow();
        borrow.setStatus(BorrowStatus.RETURNED);
        borrow.setReturnDate(LocalDateTime.now());
        borrow.setReturnConfirmedBy(admin);
        borrow.setUpdatedAt(LocalDateTime.now());

        if (borrow.getBookCopy() != null) {
            BookCopy copy = borrow.getBookCopy();
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copy.setUpdatedAt(LocalDateTime.now());
            bookCopyRepository.save(copy);
        }
        borrow = borrowRepository.save(borrow);
        log.info("Physical return confirmed for borrow {} by admin {}", borrowId, admin.getEmail());
        logEvent(borrow, "ثبت برگرداندن کتاب", "کتابدار برگرداندن کتاب را ثبت کرد و دوره امانت پایان یافت.");

        notifyUser(borrow, NotificationType.RETURN_CONFIRMED,
                "برگرداندن کتاب ثبت شد",
                String.format("برگرداندن کتاب «%s» با موفقیت ثبت شد. از همراهی شما سپاسگزاریم.",
                        borrow.getBook().getTitle()));
        notifyNextReservation(borrow.getBook());
        return mapToBorrowDTO(borrow);
    }

    /** Notify the oldest waiting reservation (a REQUESTED physical borrow with no copy yet) that a copy is free. */
    private void notifyNextReservation(Book book) {
        borrowRepository.findByStatus(BorrowStatus.REQUESTED).stream()
                .filter(r -> r.getBorrowType() == BorrowType.PHYSICAL
                        && r.getBookCopy() == null
                        && r.getBook().getId().equals(book.getId()))
                .min(java.util.Comparator.comparing(Borrow::getCreatedAt))
                .ifPresent(next -> notifyUser(next, NotificationType.PHYSICAL_APPROVED,
                        "نسخه‌ای از کتاب موجود شد",
                        String.format("نسخه‌ای از کتاب «%s» که رزرو کرده بودید موجود شد؛ درخواست شما به‌زودی توسط کتابدار بررسی می‌شود.",
                                book.getTitle())));
    }

    public BorrowDTO returnBook(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("رکورد امانت پیدا نشد"));

        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }

        if (!borrow.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("فقط می‌توانید امانت‌های خودتان را بازگردانید");
        }

        if (borrow.getBorrowType() == BorrowType.PHYSICAL) {
            throw new BadRequestException("برگرداندن کتاب فیزیکی توسط کتابدار ثبت می‌شود");
        }

        if (borrow.getStatus() != BorrowStatus.APPROVED) {
            throw new BadRequestException("فقط امانت‌های فعال را می‌توان بازگرداند");
        }

        borrow.setStatus(BorrowStatus.RETURNED);
        borrow.setReturnDate(LocalDateTime.now());
        borrow.setUpdatedAt(LocalDateTime.now());

        // Update book copy status if physical
        if (borrow.getBorrowType() == BorrowType.PHYSICAL && borrow.getBookCopy() != null) {
            BookCopy copy = borrow.getBookCopy();
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copy.setUpdatedAt(LocalDateTime.now());
            bookCopyRepository.save(copy);
        }

        borrow = borrowRepository.save(borrow);
        log.info("Book returned: {}", borrowId);

        return mapToBorrowDTO(borrow);
    }

    public List<BorrowDTO> getUserBorrows(Long libraryId, BorrowStatus status, BorrowType type) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه پیدا نشد"));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (!membership.getStatus().equals(MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("عضویت شما هنوز تأیید نشده است");
        }

        return borrowRepository.findByUserId(currentUserId).stream()
                .filter(b -> b.getLibrary().getId().equals(libraryId))
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> type == null || b.getBorrowType() == type)
                .map(this::mapToBorrowDTO)
                .collect(Collectors.toList());
    }

    public List<BorrowDTO> getPendingBorrows(Long libraryId, BorrowType type) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه پیدا نشد"));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند درخواست‌های در انتظار را ببیند");
        }

        return borrowRepository.findByStatus(BorrowStatus.REQUESTED).stream()
                .filter(b -> b.getLibrary().getId().equals(libraryId))
                .filter(b -> type == null || b.getBorrowType() == type)
                .map(this::mapToBorrowDTO)
                .collect(Collectors.toList());
    }

    public List<BorrowDTO> getLibraryBorrows(Long libraryId, BorrowStatus status, BorrowType type) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه پیدا نشد"));
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند همه‌ی امانت‌ها را ببیند");
        }
        List<Borrow> borrows = status != null
                ? borrowRepository.findByLibraryIdAndStatus(libraryId, status)
                : borrowRepository.findByLibraryId(libraryId);
        return borrows.stream()
                .filter(b -> type == null || b.getBorrowType() == type)
                .map(this::mapToBorrowDTO)
                .collect(Collectors.toList());
    }

    /** Human-friendly, unique per-borrow tracking code derived from the id (no extra column needed). */
    public static String trackingCodeFor(Long id) {
        return id == null ? null : String.format("BR-%06d", id);
    }

    /**
     * If the search query looks like a tracking code ("BR-123", "br123") or a bare number,
     * return the borrow id it refers to; otherwise null so the query is treated as free text.
     */
    private static Long parseTrackingCode(String q) {
        if (q == null) return null;
        String t = q.trim().toLowerCase();
        if (!t.matches("^(br-?)?\\d+$")) return null;
        String digits = t.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 18) return null;
        try { return Long.parseLong(digits); } catch (NumberFormatException e) { return null; }
    }

    /** Current user's own borrows across ALL libraries (paginated + searchable), for the profile tabs. */
    public org.springframework.data.domain.Page<BorrowDTO> getMyBorrowsPaged(
            BorrowType type, String search, org.springframework.data.domain.Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        final String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        org.springframework.data.jpa.domain.Specification<Borrow> spec = (root, cq, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("user").get("id"), currentUserId));
            if (type != null) ps.add(cb.equal(root.get("borrowType"), type));
            if (q != null) {
                var bookJoin = root.join("book");
                var libJoin = root.join("library");
                String pat = "%" + q + "%";
                java.util.List<jakarta.persistence.criteria.Predicate> or = new java.util.ArrayList<>();
                or.add(cb.like(cb.lower(bookJoin.get("title")), pat));
                or.add(cb.like(cb.lower(libJoin.get("name")), pat));
                Long idQuery = parseTrackingCode(q);
                if (idQuery != null) or.add(cb.equal(root.get("id"), idQuery));
                ps.add(cb.or(or.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return borrowRepository.findAll(spec, pageable).map(this::mapToBorrowDTO);
    }

    /** Paginated + searchable library borrows for the admin tables (search by user name/email or book title). */
    public org.springframework.data.domain.Page<BorrowDTO> getLibraryBorrowsPaged(
            Long libraryId, java.util.List<BorrowStatus> statuses, BorrowType type, String search,
            boolean needsAttention, boolean overdue,
            org.springframework.data.domain.Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireAdmin(currentUserId, libraryId);

        final String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        final java.time.LocalDateTime now = java.time.LocalDateTime.now();
        org.springframework.data.jpa.domain.Specification<Borrow> spec = (root, cq, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("library").get("id"), libraryId));
            if (statuses != null && !statuses.isEmpty()) ps.add(root.get("status").in(statuses));
            if (type != null) ps.add(cb.equal(root.get("borrowType"), type));
            if (overdue) {
                ps.add(cb.equal(root.get("status"), BorrowStatus.RECEIVED));
                ps.add(cb.isNull(root.get("returnDate")));
                ps.add(cb.lessThan(root.get("dueDate"), now));
            }
            if (needsAttention) {
                // Anything the librarian must act on: new requests, plus active loans where the
                // recipient asked to return / handed the book to the courier / is overdue.
                var receivedActionable = cb.and(
                        cb.equal(root.get("status"), BorrowStatus.RECEIVED),
                        cb.or(
                                cb.isNotNull(root.get("returnRequestedAt")),
                                cb.isNotNull(root.get("handedOverByUserAt")),
                                cb.and(cb.isNull(root.get("returnDate")), cb.lessThan(root.get("dueDate"), now))
                        ));
                ps.add(cb.or(cb.equal(root.get("status"), BorrowStatus.REQUESTED), receivedActionable));
            }
            if (q != null) {
                var user = root.join("user");
                var bookJoin = root.join("book");
                String pat = "%" + q + "%";
                java.util.List<jakarta.persistence.criteria.Predicate> or = new java.util.ArrayList<>();
                or.add(cb.like(cb.lower(user.get("email")), pat));
                or.add(cb.like(cb.lower(cb.coalesce(user.get("firstName"), "")), pat));
                or.add(cb.like(cb.lower(cb.coalesce(user.get("lastName"), "")), pat));
                or.add(cb.like(cb.lower(cb.coalesce(user.get("phoneNumber"), "")), pat));
                or.add(cb.like(cb.lower(bookJoin.get("title")), pat));
                or.add(cb.like(cb.lower(cb.coalesce(root.get("copyUniqueCode"), "")), pat));
                Long idQuery = parseTrackingCode(q);
                if (idQuery != null) or.add(cb.equal(root.get("id"), idQuery));
                ps.add(cb.or(or.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return borrowRepository.findAll(spec, pageable).map(this::mapToBorrowDTO);
    }

    /** Recipient cancels their own request/loan. Allowed before the book is in their hands. */
    public BorrowDTO cancelByUser(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        if (!borrow.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("فقط می‌توانید امانت‌های خودتان را لغو کنید");
        }
        // Recipient may cancel only before receiving the book: REQUESTED or APPROVED (not yet received)
        if (borrow.getStatus() != BorrowStatus.REQUESTED && borrow.getStatus() != BorrowStatus.APPROVED) {
            throw new BadRequestException("در این مرحله امکان لغو وجود ندارد");
        }
        applyCancellation(borrow);
        borrow = borrowRepository.save(borrow);
        log.info("Borrow {} cancelled by user {}", borrowId, currentUserId);
        logEvent(borrow, "لغو توسط کاربر", "گیرنده امانت را لغو کرد.");

        notifyLibraryAdmins(borrow, NotificationType.BORROW_CANCELLED_BY_USER,
                "لغو درخواست توسط کاربر",
                String.format("کاربر %s امانت کتاب «%s» را لغو کرد.",
                        fullName(borrow.getUser()), borrow.getBook().getTitle()));
        return mapToBorrowDTO(borrow);
    }

    /** Librarian cancels a request/loan before it is completed. */
    public BorrowDTO cancelByLibrarian(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        requireAdmin(currentUserId, libraryId);
        // Librarian may cancel before the loan is completed: REQUESTED, APPROVED or RECEIVED
        if (borrow.getStatus() != BorrowStatus.REQUESTED
                && borrow.getStatus() != BorrowStatus.APPROVED
                && borrow.getStatus() != BorrowStatus.RECEIVED) {
            throw new BadRequestException("در این مرحله امکان لغو وجود ندارد");
        }
        applyCancellation(borrow);
        borrow = borrowRepository.save(borrow);
        log.info("Borrow {} cancelled by librarian {}", borrowId, currentUserId);
        logEvent(borrow, "لغو توسط کتابدار", "کتابدار امانت را لغو کرد.");

        notifyUser(borrow, NotificationType.BORROW_CANCELLED_BY_LIBRARIAN,
                "لغو امانت توسط کتابخانه",
                String.format("امانت کتاب «%s» توسط کتابخانه لغو شد.", borrow.getBook().getTitle()));
        return mapToBorrowDTO(borrow);
    }

    /** Recipient asks for the book to be picked up for return (allowed any time while RECEIVED). */
    public BorrowDTO requestReturn(Long libraryId, Long borrowId, ReturnRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        if (!borrow.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("فقط می‌توانید امانت‌های خودتان را بازگردانید");
        }
        if (borrow.getBorrowType() != BorrowType.PHYSICAL) {
            throw new BadRequestException("این عملیات فقط برای امانت است");
        }
        if (borrow.getStatus() != BorrowStatus.RECEIVED) {
            throw new BadRequestException("فقط برای کتابی که در دست شماست می‌توانید درخواست برگرداندن کتاب بدهید");
        }
        if (request.getReturnAddress() == null || request.getReturnAddress().isBlank()) {
            throw new BadRequestException("آدرس تحویل کتاب الزامی است");
        }
        if (request.getReturnExtension() != null && !request.getReturnExtension().isBlank()
                && !request.getReturnExtension().matches("\\d{8}")) {
            throw new BadRequestException("شماره تلفن داخلی باید ۸ رقم باشد");
        }

        borrow.setReturnRequestedAt(LocalDateTime.now());
        borrow.setReturnAddress(request.getReturnAddress());
        borrow.setReturnExtension(request.getReturnExtension());
        borrow.setReturnPreferredDate(request.getPreferredDate());
        borrow.setUpdatedAt(LocalDateTime.now());
        borrow = borrowRepository.save(borrow);
        log.info("Return requested for borrow {} by user {}", borrowId, currentUserId);
        logEvent(borrow, "درخواست برگرداندن کتاب", "گیرنده درخواست برگرداندن کتاب/تحویل کتاب را ثبت کرد.");

        notifyLibraryAdmins(borrow, NotificationType.RETURN_REQUESTED,
                "درخواست برگرداندن کتاب",
                String.format("%s برای کتاب «%s» درخواست برگرداندن کتاب ثبت کرد.",
                        fullName(borrow.getUser()), borrow.getBook().getTitle()));
        return mapToBorrowDTO(borrow);
    }

    /** Librarian schedules the courier pickup for a requested return. */
    public BorrowDTO scheduleReturnPickup(Long libraryId, Long borrowId, ReturnScheduleRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        requireAdmin(currentUserId, libraryId);
        if (borrow.getStatus() != BorrowStatus.RECEIVED) {
            throw new BadRequestException("فقط برای امانت در دست گیرنده می‌توان زمان برگرداندن تعیین کرد");
        }

        if (request.getReturnCourierName() != null) {
            borrow.setReturnCourierName(request.getReturnCourierName());
        }
        if (request.getReturnPlannedDate() != null) {
            borrow.setReturnPlannedDate(request.getReturnPlannedDate());
        }
        // Librarian may initiate the return themselves (whether or not the loan is overdue):
        // if the recipient hasn't requested it yet, this records the return request now.
        boolean librarianInitiated = borrow.getReturnRequestedAt() == null;
        if (librarianInitiated) {
            borrow.setReturnRequestedAt(LocalDateTime.now());
            if (borrow.getReturnAddress() == null) borrow.setReturnAddress(borrow.getDeliveryAddress());
            if (borrow.getReturnExtension() == null) borrow.setReturnExtension(borrow.getDeliveryExtension());
        }
        borrow.setUpdatedAt(LocalDateTime.now());
        borrow = borrowRepository.save(borrow);
        log.info("Return pickup scheduled for borrow {} by admin {}", borrowId, currentUserId);
        logEvent(borrow, librarianInitiated ? "ثبت برگرداندن توسط کتابدار" : "تعیین زمان برگرداندن", String.format(
                "کتابدار %s (پیک: %s).",
                librarianInitiated ? "درخواست برگرداندن را ثبت و زمان آن را تعیین کرد" : "زمان برگرداندن را تعیین کرد",
                borrow.getReturnCourierName() != null ? borrow.getReturnCourierName() : "—"));

        notifyUser(borrow, NotificationType.RETURN_PICKUP_SCHEDULED,
                "زمان برگرداندن کتاب تعیین شد",
                String.format("برای برگرداندن کتاب «%s» پیک %s در تاریخ مشخص‌شده مراجعه می‌کند.",
                        borrow.getBook().getTitle(),
                        borrow.getReturnCourierName() != null ? borrow.getReturnCourierName() : "—"));
        return mapToBorrowDTO(borrow);
    }

    /** Cancel a pending return request (by the recipient or the librarian), reverting to a normal active loan. */
    public BorrowDTO cancelReturnRequest(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        boolean isOwner = borrow.getUser().getId().equals(currentUserId);
        boolean isAdmin = SecurityUtils.hasRole("SYSTEM_ADMIN")
                || membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                    .map(m -> m.getRole() == LibraryMembershipRole.ADMIN).orElse(false);
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("شما اجازه‌ی این کار را ندارید");
        }
        if (borrow.getStatus() != BorrowStatus.RECEIVED || borrow.getReturnRequestedAt() == null) {
            throw new BadRequestException("درخواست برگرداندنی برای لغو وجود ندارد");
        }
        borrow.setReturnRequestedAt(null);
        borrow.setReturnAddress(null);
        borrow.setReturnExtension(null);
        borrow.setReturnPreferredDate(null);
        borrow.setReturnCourierName(null);
        borrow.setReturnPlannedDate(null);
        borrow.setHandedOverByUserAt(null);
        borrow.setUpdatedAt(LocalDateTime.now());
        borrow = borrowRepository.save(borrow);
        log.info("Return request for borrow {} cancelled by {}", borrowId, currentUserId);
        logEvent(borrow, "لغو درخواست برگرداندن", "درخواست برگرداندن کتاب لغو شد؛ امانت همچنان فعال است.");

        if (isOwner) {
            notifyLibraryAdmins(borrow, NotificationType.RETURN_REQUESTED,
                    "لغو درخواست برگرداندن",
                    String.format("%s درخواست برگرداندن کتاب «%s» را لغو کرد.",
                            fullName(borrow.getUser()), borrow.getBook().getTitle()));
        } else {
            notifyUser(borrow, NotificationType.RETURN_PICKUP_SCHEDULED,
                    "لغو درخواست برگرداندن",
                    String.format("درخواست برگرداندن کتاب «%s» توسط کتابخانه لغو شد.", borrow.getBook().getTitle()));
        }
        return mapToBorrowDTO(borrow);
    }

    /** Recipient confirms they handed the book to the courier; awaits the librarian's final confirmation. */
    public BorrowDTO confirmHandover(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("درخواست امانت پیدا نشد"));
        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این امانت مربوط به این کتابخانه نیست");
        }
        if (!borrow.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("فقط می‌توانید امانت‌های خودتان را تحویل دهید");
        }
        if (borrow.getStatus() != BorrowStatus.RECEIVED) {
            throw new BadRequestException("در این مرحله امکان تحویل کتاب وجود ندارد");
        }
        if (borrow.getReturnRequestedAt() == null) {
            throw new BadRequestException("ابتدا باید درخواست برگرداندن کتاب را ثبت کنید");
        }
        borrow.setHandedOverByUserAt(LocalDateTime.now());
        borrow.setUpdatedAt(LocalDateTime.now());
        borrow = borrowRepository.save(borrow);
        log.info("Borrow {} handed over to courier by user {}", borrowId, currentUserId);
        logEvent(borrow, "تحویل به پیک", "گیرنده کتاب را به پیک تحویل داد؛ در انتظار تأیید نهایی کتابدار.");

        notifyLibraryAdmins(borrow, NotificationType.BOOK_HANDED_OVER,
                "کتاب تحویل پیک شد",
                String.format("%s کتاب «%s» را به پیک تحویل داد؛ لطفاً پس از دریافت، برگرداندن را ثبت کنید.",
                        fullName(borrow.getUser()), borrow.getBook().getTitle()));
        return mapToBorrowDTO(borrow);
    }

    /** Shared cancellation effects: mark CANCELLED and free the physical copy if it was held. */
    private void applyCancellation(Borrow borrow) {
        borrow.setStatus(BorrowStatus.CANCELLED);
        borrow.setUpdatedAt(LocalDateTime.now());
        if (borrow.getBorrowType() == BorrowType.PHYSICAL && borrow.getBookCopy() != null) {
            BookCopy copy = borrow.getBookCopy();
            if (copy.getStatus() == BookCopyStatus.BORROWED) {
                copy.setStatus(BookCopyStatus.AVAILABLE);
                copy.setUpdatedAt(LocalDateTime.now());
                bookCopyRepository.save(copy);
            }
        }
    }

    /** Summary of a user's borrowing situation in a library (to help the librarian decide). */
    public com.library.dto.BorrowerSummaryDTO getBorrowerSummary(Long libraryId, Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        requireAdmin(currentUserId, libraryId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربر پیدا نشد"));

        List<Borrow> all = borrowRepository.findByUserId(userId).stream()
                .filter(b -> b.getLibrary().getId().equals(libraryId))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        long activePhysical = all.stream().filter(b -> b.getBorrowType() == BorrowType.PHYSICAL
                && (b.getStatus() == BorrowStatus.APPROVED || b.getStatus() == BorrowStatus.RECEIVED)).count();
        long activeDigital = all.stream().filter(b -> b.getBorrowType() == BorrowType.DIGITAL
                && b.getStatus() == BorrowStatus.APPROVED).count();
        long pending = all.stream().filter(b -> b.getStatus() == BorrowStatus.REQUESTED).count();
        long overdue = all.stream().filter(b -> b.getReturnDate() == null && b.getDueDate() != null
                && (b.getStatus() == BorrowStatus.APPROVED || b.getStatus() == BorrowStatus.RECEIVED)
                && now.isAfter(b.getDueDate())).count();
        long returned = all.stream().filter(b -> b.getStatus() == BorrowStatus.RETURNED).count();
        long rejected = all.stream().filter(b -> b.getStatus() == BorrowStatus.REJECTED).count();
        long cancelled = all.stream().filter(b -> b.getStatus() == BorrowStatus.CANCELLED).count();

        // Current active loans (in-hand or dispatched) to display
        List<BorrowDTO> activeLoans = all.stream()
                .filter(b -> b.getStatus() == BorrowStatus.APPROVED || b.getStatus() == BorrowStatus.RECEIVED)
                .map(this::mapToBorrowDTO)
                .collect(Collectors.toList());

        return com.library.dto.BorrowerSummaryDTO.builder()
                .userId(user.getId())
                .userFullName(fullName(user))
                .userEmail(user.getEmail())
                .userPhone(user.getPhoneNumber())
                .activePhysical(activePhysical)
                .activeDigital(activeDigital)
                .pending(pending)
                .overdue(overdue)
                .returned(returned)
                .rejected(rejected)
                .cancelled(cancelled)
                .totalBorrows(all.size())
                .activeLoans(activeLoans)
                .build();
    }

    // ---- Helpers ----

    private void requireAdmin(Long userId, Long libraryId) {
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(userId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("شما اجازه‌ی انجام این کار را ندارید");
        }
    }

    private String fullName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? user.getEmail() : name;
    }

    private String userBorrowLink(Borrow borrow) {
        return "/libraries/" + borrow.getLibrary().getId() + "/borrows/physical";
    }

    private String adminBorrowLink(Borrow borrow) {
        return "/libraries/" + borrow.getLibrary().getId() + "/admin/borrows/physical";
    }

    /** Notify the borrow's owner (recipient). */
    private void notifyUser(Borrow borrow, NotificationType type, String title, String message) {
        notificationService.notify(borrow.getUser(), type, title, message,
                ENTITY_BORROW, borrow.getId(), userBorrowLink(borrow));
    }

    /** Notify all admins of the borrow's library. */
    private void notifyLibraryAdmins(Borrow borrow, NotificationType type, String title, String message) {
        membershipRepository.findByLibraryId(borrow.getLibrary().getId()).stream()
                .filter(m -> m.getRole() == LibraryMembershipRole.ADMIN)
                .forEach(m -> notificationService.notify(m.getUser(), type, title, message,
                        ENTITY_BORROW, borrow.getId(), adminBorrowLink(borrow)));
    }

    private BorrowDTO mapToBorrowDTO(Borrow borrow) {
        boolean isOverdue = false;
        if (borrow.getDueDate() != null
                && (borrow.getStatus() == BorrowStatus.APPROVED || borrow.getStatus() == BorrowStatus.RECEIVED)
                && borrow.getReturnDate() == null) {
            isOverdue = LocalDateTime.now().isAfter(borrow.getDueDate());
        }

        boolean isReservation = borrow.getBorrowType() == BorrowType.PHYSICAL
                && borrow.getBookCopy() == null
                && borrow.getStatus() == BorrowStatus.REQUESTED;

        User u = borrow.getUser();
        return BorrowDTO.builder()
                .id(borrow.getId())
                .trackingCode(trackingCodeFor(borrow.getId()))
                .userId(u.getId())
                .userEmail(u.getEmail())
                .userFullName(fullName(u))
                .userPhone(u.getPhoneNumber())
                .libraryId(borrow.getLibrary().getId())
                .libraryName(borrow.getLibrary().getName())
                .bookId(borrow.getBook().getId())
                .bookTitle(borrow.getBook().getTitle())
                .borrowType(borrow.getBorrowType())
                .bookCopyId(borrow.getBookCopy() != null ? borrow.getBookCopy().getId() : null)
                .copyNumber(borrow.getBookCopy() != null ? borrow.getBookCopy().getCopyNumber() : null)
                .status(borrow.getStatus())
                .approvedById(borrow.getApprovedBy() != null ? borrow.getApprovedBy().getId() : null)
                .rejectionReason(borrow.getRejectionReason())
                .borrowDate(borrow.getBorrowDate())
                .dueDate(borrow.getDueDate())
                .returnDate(borrow.getReturnDate())
                .isOverdue(isOverdue)
                .isReservation(isReservation)
                .deliveryAddress(borrow.getDeliveryAddress())
                .deliveryExtension(borrow.getDeliveryExtension())
                .requestedDurationDays(borrow.getRequestedDurationDays())
                .approvedDurationDays(borrow.getApprovedDurationDays())
                .plannedDeliveryDate(borrow.getPlannedDeliveryDate())
                .courierName(borrow.getCourierName())
                .copyUniqueCode(borrow.getCopyUniqueCode())
                .receivedAt(borrow.getReceivedAt())
                .returnRequestedAt(borrow.getReturnRequestedAt())
                .returnAddress(borrow.getReturnAddress())
                .returnExtension(borrow.getReturnExtension())
                .returnPreferredDate(borrow.getReturnPreferredDate())
                .returnCourierName(borrow.getReturnCourierName())
                .returnPlannedDate(borrow.getReturnPlannedDate())
                .handedOverByUserAt(borrow.getHandedOverByUserAt())
                .createdAt(borrow.getCreatedAt())
                .updatedAt(borrow.getUpdatedAt())
                .build();
    }
}
