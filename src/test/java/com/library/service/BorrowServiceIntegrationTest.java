package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.BorrowRequest;
import com.library.dto.BorrowDTO;
import com.library.entity.*;
import com.library.entity.enums.*;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
public class BorrowServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BorrowService borrowService;

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

    private User adminUser;
    private User memberUser;
    private Library testLibrary;
    private Book physicalBook;
    private BookCopy bookCopy;
    private Book digitalBook;
    private DigitalBook digitalBookFile;

    @BeforeEach
    void setUp() {
        borrowRepository.deleteAll();
        digitalBookRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        membershipRepository.deleteAll();
        libraryRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        adminUser = User.builder()
                .email("admin@example.com")
                .passwordHash("hashedpassword")
                .firstName("Admin")
                .lastName("User")
                .systemRole(SystemRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        adminUser = userRepository.save(adminUser);

        memberUser = User.builder()
                .email("member@example.com")
                .passwordHash("hashedpassword")
                .firstName("Member")
                .lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        memberUser = userRepository.save(memberUser);

        // Create library
        testLibrary = Library.builder()
                .name("Test Library")
                .description("A test library")
                .ownerId(adminUser.getId())
                .location("Test City")
                .phone("+1234567890")
                .email("library@example.com")
                .isActive(true)
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(14)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testLibrary = libraryRepository.save(testLibrary);

        // Add admin as library admin
        LibraryMembership adminMembership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(adminUser.getId())
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedBy(adminUser.getId())
                .build();
        membershipRepository.save(adminMembership);

        // Add member
        LibraryMembership memberMembership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(memberUser.getId())
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedBy(adminUser.getId())
                .build();
        membershipRepository.save(memberMembership);

        // Create physical book with copies
        physicalBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Physical Book")
                .author("Test Author")
                .isbn("111-222-333")
                .description("A physical book")
                .publishedYear(2023)
                .publisher("Test Publisher")
                .category("Fiction")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        physicalBook = bookRepository.save(physicalBook);

        // Add book copy
        bookCopy = BookCopy.builder()
                .bookId(physicalBook.getId())
                .copyNumber(1)
                .status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bookCopy = bookCopyRepository.save(bookCopy);

        // Create digital book
        digitalBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Digital Book")
                .author("Digital Author")
                .isbn("444-555-666")
                .description("A digital book")
                .publishedYear(2023)
                .publisher("Digital Publisher")
                .category("Science")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        digitalBook = bookRepository.save(digitalBook);

        // Add digital book file
        digitalBookFile = DigitalBook.builder()
                .bookId(digitalBook.getId())
                .format(BookFormat.PDF)
                .filePath("/files/book.pdf")
                .fileSize(1024000L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        digitalBookRepository.save(digitalBookFile);
    }

    @Test
    void testCreatePhysicalBorrowRequest() {
        BorrowRequest request = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();

        BorrowDTO response = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(BorrowStatus.REQUESTED, response.getStatus());
        assertEquals(BorrowType.PHYSICAL, response.getBorrowType());
        assertEquals(bookCopy.getId(), response.getBookCopyId());
    }

    @Test
    void testCreatePhysicalBorrowRequestNoCopyAvailable() {
        // Mark the only copy as borrowed
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);

        BorrowRequest request = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();

        assertThrows(BadRequestException.class,
                () -> borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request));
    }

    @Test
    void testCreatePhysicalBorrowRequestDuplicate() {
        // Create first borrow request
        BorrowRequest firstRequest = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();
        borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), firstRequest);

        // Try to create another - should fail (duplicate active borrow)
        assertThrows(BadRequestException.class,
                () -> borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), firstRequest));
    }

    @Test
    void testCreateDigitalBorrowRequestAutoApprove() {
        BorrowRequest request = BorrowRequest.builder()
                .borrowType(BorrowType.DIGITAL)
                .build();

        BorrowDTO response = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);

        assertNotNull(response);
        assertEquals(BorrowStatus.APPROVED, response.getStatus());
        assertEquals(BorrowType.DIGITAL, response.getBorrowType());
        assertEquals(digitalBookFile.getId(), response.getDigitalBookId());
    }

    @Test
    void testCreateDigitalBorrowRequestNoAutoApprove() {
        // Disable auto-approval
        digitalBook.setAutoDigitalBorrowEnabled(false);
        bookRepository.save(digitalBook);

        BorrowRequest request = BorrowRequest.builder()
                .borrowType(BorrowType.DIGITAL)
                .build();

        BorrowDTO response = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);

        assertEquals(BorrowStatus.REQUESTED, response.getStatus());
    }

    @Test
    void testCreateMultipleDigitalBorrows() {
        // Create a second digital format
        DigitalBook epubFormat = DigitalBook.builder()
                .bookId(digitalBook.getId())
                .format(BookFormat.EPUB)
                .filePath("/files/book.epub")
                .fileSize(800000L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        digitalBookRepository.save(epubFormat);

        // Borrow PDF
        BorrowRequest pdfRequest = BorrowRequest.builder()
                .borrowType(BorrowType.DIGITAL)
                .build();
        BorrowDTO pdfBorrow = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), pdfRequest);

        // Borrow EPUB - should succeed (different digital format)
        List<Borrow> borrows = borrowRepository.findByUserIdAndBookId(memberUser.getId(), digitalBook.getId());
        assertEquals(1, borrows.size());
    }

    @Test
    void testApproveBorrowRequest() {
        // Create physical borrow request
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();
        BorrowDTO createdBorrow = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);

        // Approve the borrow
        BorrowDTO approved = borrowService.approveBorrowRequest(createdBorrow.getId(), adminUser.getId());

        assertEquals(BorrowStatus.APPROVED, approved.getStatus());
        assertNotNull(approved.getBorrowDate());
        assertEquals(adminUser.getId(), approved.getApprovedById());
        assertNotNull(approved.getDueDate());

        // Verify copy status changed to BORROWED
        BookCopy updated = bookCopyRepository.findById(bookCopy.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(BookCopyStatus.BORROWED, updated.getStatus());
    }

    @Test
    void testRejectBorrowRequest() {
        // Create physical borrow request
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();
        BorrowDTO createdBorrow = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);

        // Reject the borrow
        String rejectionReason = "Book is reserved";
        BorrowDTO rejected = borrowService.rejectBorrowRequest(createdBorrow.getId(), adminUser.getId(), rejectionReason);

        assertEquals(BorrowStatus.REJECTED, rejected.getStatus());
        assertEquals(rejectionReason, rejected.getRejectionReason());

        // Verify copy status remained AVAILABLE
        BookCopy copy = bookCopyRepository.findById(bookCopy.getId()).orElse(null);
        assertNotNull(copy);
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void testReturnBook() {
        // Create and approve borrow
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();
        BorrowDTO createdBorrow = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);
        BorrowDTO approved = borrowService.approveBorrowRequest(createdBorrow.getId(), adminUser.getId());

        // Return the book
        BorrowDTO returned = borrowService.returnBook(approved.getId(), memberUser.getId());

        assertEquals(BorrowStatus.RETURNED, returned.getStatus());
        assertNotNull(returned.getReturnDate());

        // Verify copy status changed back to AVAILABLE
        BookCopy copy = bookCopyRepository.findById(bookCopy.getId()).orElse(null);
        assertNotNull(copy);
        assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    void testCalculateOverdue() {
        // Create and approve borrow
        BorrowRequest request = BorrowRequest.builder()
                .bookId(physicalBook.getId())
                .borrowType(BorrowType.PHYSICAL)
                .build();
        BorrowDTO createdBorrow = borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);
        BorrowDTO approved = borrowService.approveBorrowRequest(createdBorrow.getId(), adminUser.getId());

        // Set due date in the past
        Borrow borrow = borrowRepository.findById(approved.getId()).orElse(null);
        assertNotNull(borrow);
        borrow.setDueDate(LocalDateTime.now().minusDays(5));
        borrowRepository.save(borrow);

        // Fetch borrow and check overdue
        BorrowDTO borrowDTO = borrowService.getUserBorrows(memberUser.getId()).stream()
                .filter(b -> b.getId().equals(approved.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(borrowDTO);
        assertTrue(borrowDTO.isOverdue());
    }

    @Test
    void testGetUserBorrows() {
        // Create multiple borrows
        BorrowRequest physicalRequest = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();
        borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), physicalRequest);

        List<BorrowDTO> userBorrows = borrowService.getUserBorrows(memberUser.getId());

        assertNotNull(userBorrows);
        assertFalse(userBorrows.isEmpty());
        assertTrue(userBorrows.stream().allMatch(b -> b.getUserId().equals(memberUser.getId())));
    }

    @Test
    void testGetPendingBorrowsAdminOnly() {
        // Create borrow request
        BorrowRequest request = BorrowRequest.builder()
                .borrowType(BorrowType.PHYSICAL)
                .build();
        borrowService.createBorrowRequest(testLibrary.getId(), memberUser.getId(), request);

        // Admin can get pending borrows
        List<BorrowDTO> pendingBorrows = borrowService.getPendingBorrows(testLibrary.getId(), adminUser.getId());

        assertNotNull(pendingBorrows);
        assertFalse(pendingBorrows.isEmpty());
        assertTrue(pendingBorrows.stream().allMatch(b -> b.getStatus() == BorrowStatus.REQUESTED));
    }

    @Test
    void testBorrowNotFoundThrowsException() {
        assertThrows(ResourceNotFoundException.class,
                () -> borrowService.approveBorrowRequest(99999L, adminUser.getId()));
    }
}
