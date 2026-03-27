package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.BorrowDTO;
import com.library.entity.*;
import com.library.entity.enums.*;
import com.library.exception.BadRequestException;
import com.library.repository.*;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Book Reservation Tests")
class ReservationServiceTest extends BaseIntegrationTest {

    @Autowired private BorrowService borrowService;
    @Autowired private BorrowRepository borrowRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;

    private User adminUser;
    private User memberUser;
    private Library library;
    private Book book;
    private BookCopy bookCopy;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("resv-admin@test.com").passwordHash("$2a$10$x")
                .firstName("Admin").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        memberUser = userRepository.save(User.builder()
                .email("resv-member@test.com").passwordHash("$2a$10$x")
                .firstName("Member").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        library = libraryRepository.save(Library.builder()
                .name("Reserve Library").owner(adminUser)
                .autoMembershipApproval(false).defaultBorrowDurationDays(14)
                .isActive(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(library).role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED).approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        membershipRepository.save(LibraryMembership.builder()
                .user(memberUser).library(library).role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED).approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        book = bookRepository.save(Book.builder()
                .library(library).title("Reserved Book").author("Author")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        bookCopy = bookCopyRepository.save(BookCopy.builder()
                .book(book).library(library).copyNumber(1)
                .status(BookCopyStatus.BORROWED)  // all copies already borrowed
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("Member can reserve a book when no copies are available")
    void reserveBook_noCopiesAvailable_succeeds() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowDTO result = borrowService.reserveBook(library.getId(), book.getId());

        assertThat(result.getBorrowType()).isEqualTo(BorrowType.PHYSICAL);
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.REQUESTED);
        assertThat(result.getBookCopyId()).isNull();
        assertThat(result.getIsReservation()).isTrue();
    }

    @Test
    @DisplayName("Cannot reserve a book that has available copies")
    void reserveBook_copiesAvailable_throws() {
        bookCopy.setStatus(BookCopyStatus.AVAILABLE);
        bookCopyRepository.save(bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        assertThatThrownBy(() -> borrowService.reserveBook(library.getId(), book.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("موجود است");
    }

    @Test
    @DisplayName("Cannot reserve same book twice")
    void reserveBook_duplicate_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        borrowService.reserveBook(library.getId(), book.getId());

        assertThatThrownBy(() -> borrowService.reserveBook(library.getId(), book.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Admin approving a reservation auto-assigns an available copy")
    void approveReservation_autoAssignsCopy() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO reservation = borrowService.reserveBook(library.getId(), book.getId());

        // Now return the copy so it becomes available
        bookCopy.setStatus(BookCopyStatus.AVAILABLE);
        bookCopyRepository.save(bookCopy);

        // Admin approves the reservation
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO approved = borrowService.approveBorrowRequest(library.getId(), reservation.getId());

        assertThat(approved.getStatus()).isEqualTo(BorrowStatus.APPROVED);
        assertThat(approved.getBookCopyId()).isNotNull();
        assertThat(approved.getBorrowDate()).isNotNull();
    }

    @Test
    @DisplayName("Approving reservation when no copy available throws")
    void approveReservation_noCopyAvailable_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO reservation = borrowService.reserveBook(library.getId(), book.getId());

        // bookCopy is still BORROWED
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        assertThatThrownBy(() -> borrowService.approveBorrowRequest(library.getId(), reservation.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("موجود");
    }
}
