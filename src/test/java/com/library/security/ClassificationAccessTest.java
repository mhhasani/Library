package com.library.security;

import com.library.BaseIntegrationTest;
import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.BookRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import com.library.service.BookService;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("Classification labels: users only see data their clearance dominates")
class ClassificationAccessTest extends BaseIntegrationTest {

    @Autowired private BookService bookService;
    @Autowired private BookRepository bookRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    private User librarian;
    private User reader;
    private Library library;
    private Book openBook;
    private Book confidentialBook;

    @BeforeEach
    void setUp() {
        librarian = userRepository.save(user("librarian@x.ir", ClassificationLevel.CONFIDENTIAL));
        reader = userRepository.save(user("reader@x.ir", ClassificationLevel.UNCLASSIFIED));
        library = libraryRepository.save(Library.builder().name("Lib").owner(librarian)
                .autoMembershipApproval(false).defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        member(librarian, LibraryMembershipRole.ADMIN);
        member(reader, LibraryMembershipRole.MEMBER);
        openBook = bookRepository.save(book("Open book", ClassificationLevel.UNCLASSIFIED));
        confidentialBook = bookRepository.save(book("Confidential book", ClassificationLevel.CONFIDENTIAL));
    }

    @AfterEach
    void clear() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("A reader without clearance sees only unclassified books in listings and searches")
    void listingsHideClassifiedBooks() {
        SecurityTestUtils.setSecurityContext(reader, "USER");

        assertThat(titles(bookService.getLibraryBooks(library.getId(), PageRequest.of(0, 20)).getContent()))
                .containsExactly("Open book");
        assertThat(titles(bookService.searchBooks(library.getId(), "book", PageRequest.of(0, 20)).getContent()))
                .containsExactly("Open book");
        assertThat(titles(bookService.globalSearch("book", PageRequest.of(0, 20)).getContent()))
                .containsExactly("Open book");
    }

    @Test
    @DisplayName("Direct access to a classified book is reported as not found")
    void directAccessLooksLikeNotFound() {
        SecurityTestUtils.setSecurityContext(reader, "USER");

        assertThatThrownBy(() -> bookService.getBookById(library.getId(), confidentialBook.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Anonymous global search never returns classified books")
    void anonymousSeesOnlyUnclassified() {
        assertThat(titles(bookService.globalSearch("book", PageRequest.of(0, 20)).getContent()))
                .containsExactly("Open book");
    }

    @Test
    @DisplayName("A cleared user sees classified books up to their clearance, with the label in the DTO")
    void clearedUserSeesLabeledBook() {
        SecurityTestUtils.setSecurityContext(librarian, "USER");

        BookDTO dto = bookService.getBookById(library.getId(), confidentialBook.getId());
        assertThat(dto.getClassification()).isEqualTo(ClassificationLevel.CONFIDENTIAL);
        assertThat(bookService.getLibraryBooks(library.getId(), PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("System administrators are not restricted by clearance")
    void systemAdminSeesEverything() {
        SecurityTestUtils.setSecurityContext(reader, "SYSTEM_ADMIN");

        assertThat(bookService.globalSearch("book", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Nobody can label data above their own clearance")
    void cannotLabelAboveOwnClearance() {
        SecurityTestUtils.setSecurityContext(librarian, "USER");
        BookRequest request = BookRequest.builder().title("T").author("A")
                .classification(ClassificationLevel.SECRET).build();

        assertThatThrownBy(() -> bookService.createBook(library.getId(), request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("New books default to UNCLASSIFIED and an update without a label keeps the current one")
    void defaultsAndPreservation() {
        SecurityTestUtils.setSecurityContext(librarian, "USER");

        BookDTO created = bookService.createBook(library.getId(),
                BookRequest.builder().title("New").author("A").build());
        assertThat(created.getClassification()).isEqualTo(ClassificationLevel.UNCLASSIFIED);

        BookDTO updated = bookService.updateBook(library.getId(), confidentialBook.getId(),
                BookRequest.builder().title("Renamed").author("A").autoDigitalBorrowEnabled(false).build());
        assertThat(updated.getClassification()).isEqualTo(ClassificationLevel.CONFIDENTIAL);
    }

    @Test
    @DisplayName("The database itself rejects an unknown classification value")
    void databaseRejectsInvalidLabel() {
        assertThatThrownBy(() -> jdbc.update("UPDATE books SET classification = 'BOGUS' WHERE id = ?",
                openBook.getId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private static List<String> titles(List<BookDTO> books) {
        return books.stream().map(BookDTO::getTitle).toList();
    }

    private static User user(String email, ClassificationLevel clearance) {
        return User.builder().email(email).passwordHash("x").firstName("F").lastName("L")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE).clearance(clearance).build();
    }

    private void member(User user, LibraryMembershipRole role) {
        membershipRepository.save(LibraryMembership.builder().user(user).library(library).role(role)
                .status(MembershipStatus.APPROVED).approvedBy(librarian)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private Book book(String title, ClassificationLevel classification) {
        return Book.builder().library(library).title(title).author("Author").classification(classification).build();
    }
}
