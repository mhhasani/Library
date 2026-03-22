package com.library.service;

import com.library.dto.BorrowDTO;
import com.library.dto.BorrowRequest;
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

    public BorrowDTO createBorrowRequest(Long libraryId, Long bookId, BorrowRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Current user not found"));

        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
        }

        // Check user membership
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (!membership.getStatus().equals(MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User membership is not approved");
        }

        Borrow borrow;

        if (request.getBorrowType() == BorrowType.PHYSICAL) {
            return createPhysicalBorrow(user, library, book, request);
        } else if (request.getBorrowType() == BorrowType.DIGITAL) {
            return createDigitalBorrow(user, library, book, request);
        } else {
            throw new BadRequestException("Invalid borrow type");
        }
    }

    private BorrowDTO createPhysicalBorrow(User user, Library library, Book book, BorrowRequest request) {
        BookCopy bookCopy;

        if (request.getBookCopyId() != null) {
            bookCopy = bookCopyRepository.findById(request.getBookCopyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book copy not found"));
            if (!bookCopy.getBook().getId().equals(book.getId())) {
                throw new BadRequestException("Book copy does not belong to this book");
            }
            if (bookCopy.getStatus() != BookCopyStatus.AVAILABLE) {
                throw new BadRequestException("Book copy is not available");
            }
        } else {
            // Auto-select first available copy
            bookCopy = bookCopyRepository.findByBookIdAndStatus(book.getId(), BookCopyStatus.AVAILABLE)
                    .stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("No available copy of this book"));
        }

        // Check user doesn't already have an active physical borrow of this book
        List<Borrow> activeBorrows = borrowRepository.findActivePhysicalBorrowByUserAndBook(
                user.getId(), book.getId(), BorrowType.PHYSICAL);
        if (!activeBorrows.isEmpty()) {
            throw new BadRequestException("User already has an active physical borrow of this book");
        }

        Borrow borrow = Borrow.builder()
                .user(user)
                .library(library)
                .book(book)
                .borrowType(BorrowType.PHYSICAL)
                .bookCopy(bookCopy)
                .status(BorrowStatus.REQUESTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        borrow = borrowRepository.save(borrow);
        log.info("Physical borrow request created: {} by user: {}", borrow.getId(), user.getEmail());

        return mapToBorrowDTO(borrow);
    }

    private BorrowDTO createDigitalBorrow(User user, Library library, Book book, BorrowRequest request) {
        if (request.getDigitalBookId() == null) {
            throw new BadRequestException("Digital book ID is required for digital borrow");
        }

        DigitalBook digitalBook = digitalBookRepository.findById(request.getDigitalBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Digital book not found"));

        // Check digital book belongs to the book
        if (!digitalBook.getBook().getId().equals(book.getId())) {
            throw new BadRequestException("Digital book does not belong to this book");
        }

        // Check user doesn't already have this digital version
        List<Borrow> existingBorrows = borrowRepository.findActivePhysicalBorrowByUserAndBook(
                user.getId(), book.getId(), BorrowType.DIGITAL);
        boolean alreadyHas = existingBorrows.stream()
                .anyMatch(b -> b.getDigitalBook().getId().equals(digitalBook.getId()) 
                        && b.getReturnDate() == null);
        if (alreadyHas) {
            throw new BadRequestException("User already has an active borrow of this digital version");
        }

        BorrowStatus initialStatus = book.getAutoDigitalBorrowEnabled() ? BorrowStatus.APPROVED : BorrowStatus.REQUESTED;

        Borrow borrow = Borrow.builder()
                .user(user)
                .library(library)
                .book(book)
                .borrowType(BorrowType.DIGITAL)
                .digitalBook(digitalBook)
                .status(initialStatus)
                .approvedBy(book.getAutoDigitalBorrowEnabled() ? library.getOwner() : null)
                .borrowDate(book.getAutoDigitalBorrowEnabled() ? LocalDateTime.now() : null)
                .dueDate(book.getAutoDigitalBorrowEnabled() ? 
                        LocalDateTime.now().plusDays(library.getDefaultBorrowDurationDays()) : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        borrow = borrowRepository.save(borrow);
        log.info("Digital borrow request created: {} by user: {}", borrow.getId(), user.getEmail());

        return mapToBorrowDTO(borrow);
    }

    public BorrowDTO approveBorrowRequest(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow request not found"));

        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Borrow does not belong to this library");
        }

        // Check if user is library admin
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can approve borrow requests");
        }

        if (borrow.getStatus() != BorrowStatus.REQUESTED) {
            throw new BadRequestException("Only REQUESTED borrows can be approved");
        }

        User admin = userRepository.findById(currentUserId).orElseThrow();
        borrow.setStatus(BorrowStatus.APPROVED);
        borrow.setApprovedBy(admin);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setDueDate(LocalDateTime.now().plusDays(borrow.getLibrary().getDefaultBorrowDurationDays()));
        borrow.setUpdatedAt(LocalDateTime.now());

        // Update book copy status if physical
        if (borrow.getBorrowType() == BorrowType.PHYSICAL && borrow.getBookCopy() != null) {
            BookCopy copy = borrow.getBookCopy();
            copy.setStatus(BookCopyStatus.BORROWED);
            copy.setUpdatedAt(LocalDateTime.now());
            bookCopyRepository.save(copy);

            // If no available copies remain, auto-reject all other pending requests for this book
            long availableCopies = bookCopyRepository.countByBookIdAndStatus(borrow.getBook().getId(), BookCopyStatus.AVAILABLE);
            if (availableCopies == 0) {
                Long bookId = borrow.getBook().getId();
                List<Borrow> pendingBorrows = borrowRepository.findByStatus(BorrowStatus.REQUESTED).stream()
                        .filter(b -> b.getBook().getId().equals(bookId) && !b.getId().equals(borrowId))
                        .collect(Collectors.toList());
                for (Borrow pending : pendingBorrows) {
                    pending.setStatus(BorrowStatus.REJECTED);
                    pending.setRejectionReason("رد خودکار: موجودی کتاب به پایان رسید");
                    pending.setUpdatedAt(LocalDateTime.now());
                    borrowRepository.save(pending);
                    log.info("Auto-rejected borrow {} due to no available copies of book {}", pending.getId(), bookId);
                }
            }
        }

        borrow = borrowRepository.save(borrow);
        log.info("Borrow request approved: {} by admin: {}", borrowId, admin.getEmail());

        return mapToBorrowDTO(borrow);
    }

    public BorrowDTO rejectBorrowRequest(Long libraryId, Long borrowId, String rejectionReason) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow request not found"));

        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Borrow does not belong to this library");
        }

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can reject borrow requests");
        }

        if (borrow.getStatus() != BorrowStatus.REQUESTED) {
            throw new BadRequestException("Only REQUESTED borrows can be rejected");
        }

        User admin = userRepository.findById(currentUserId).orElseThrow();
        borrow.setStatus(BorrowStatus.REJECTED);
        borrow.setRejectedBy(admin);
        borrow.setRejectionReason(rejectionReason);
        borrow.setUpdatedAt(LocalDateTime.now());

        borrow = borrowRepository.save(borrow);
        log.info("Borrow request rejected: {} by admin: {}", borrowId, admin.getEmail());

        return mapToBorrowDTO(borrow);
    }

    public BorrowDTO returnBook(Long libraryId, Long borrowId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found"));

        if (!borrow.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Borrow does not belong to this library");
        }

        if (!borrow.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("User can only return their own borrows");
        }

        if (borrow.getStatus() != BorrowStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED borrows can be returned");
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

    public List<BorrowDTO> getUserBorrows(Long libraryId, BorrowStatus status) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found"));

        return borrowRepository.findByUserId(currentUserId).stream()
                .filter(b -> b.getLibrary().getId().equals(libraryId))
                .filter(b -> status == null || b.getStatus() == status)
                .map(this::mapToBorrowDTO)
                .collect(Collectors.toList());
    }

    public List<BorrowDTO> getPendingBorrows(Long libraryId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found"));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can view pending borrows");
        }

        List<Borrow> borrows = borrowRepository.findByStatus(BorrowStatus.REQUESTED).stream()
                .filter(b -> b.getLibrary().getId().equals(libraryId))
                .collect(Collectors.toList());

        return borrows.stream()
                .map(this::mapToBorrowDTO)
                .collect(Collectors.toList());
    }

    public List<BorrowDTO> getLibraryBorrows(Long libraryId, BorrowStatus status) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can view all borrows");
        }
        List<Borrow> borrows = status != null
                ? borrowRepository.findByLibraryIdAndStatus(libraryId, status)
                : borrowRepository.findByLibraryId(libraryId);
        return borrows.stream().map(this::mapToBorrowDTO).collect(Collectors.toList());
    }

    private BorrowDTO mapToBorrowDTO(Borrow borrow) {
        boolean isOverdue = false;
        if (borrow.getDueDate() != null && borrow.getStatus() == BorrowStatus.APPROVED && borrow.getReturnDate() == null) {
            isOverdue = LocalDateTime.now().isAfter(borrow.getDueDate());
        }

        return BorrowDTO.builder()
                .id(borrow.getId())
                .userId(borrow.getUser().getId())
                .userEmail(borrow.getUser().getEmail())
                .libraryId(borrow.getLibrary().getId())
                .bookId(borrow.getBook().getId())
                .bookTitle(borrow.getBook().getTitle())
                .borrowType(borrow.getBorrowType())
                .bookCopyId(borrow.getBookCopy() != null ? borrow.getBookCopy().getId() : null)
                .copyNumber(borrow.getBookCopy() != null ? borrow.getBookCopy().getCopyNumber() : null)
                .digitalBookId(borrow.getDigitalBook() != null ? borrow.getDigitalBook().getId() : null)
                .fileFormat(borrow.getDigitalBook() != null ? borrow.getDigitalBook().getFileFormat() : null)
                .status(borrow.getStatus())
                .approvedById(borrow.getApprovedBy() != null ? borrow.getApprovedBy().getId() : null)
                .rejectionReason(borrow.getRejectionReason())
                .borrowDate(borrow.getBorrowDate())
                .dueDate(borrow.getDueDate())
                .returnDate(borrow.getReturnDate())
                .isOverdue(isOverdue)
                .createdAt(borrow.getCreatedAt())
                .updatedAt(borrow.getUpdatedAt())
                .build();
    }
}
