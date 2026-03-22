package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.config.WithMockCustomUser;
import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.entity.*;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.*;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Book Service Tests")
class BookServiceTest extends BaseIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User regularUser;
    private Library library;
    private BookRequest bookRequest;

    @BeforeEach
    void setUp() {
        // Create admin user with ID 1
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@library.com");
        adminUser.setPasswordHash("$2a$10$encoded");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setSystemRole(SystemRole.USER);
        adminUser.setAccountStatus(AccountStatus.ACTIVE);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);

        // Create regular user with ID 2
        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setEmail("user@library.com");
        regularUser.setPasswordHash("$2a$10$encoded");
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setSystemRole(SystemRole.USER);
        regularUser.setAccountStatus(AccountStatus.ACTIVE);
        regularUser.setCreatedAt(LocalDateTime.now());
        regularUser.setUpdatedAt(LocalDateTime.now());
        regularUser = userRepository.save(regularUser);

        // Create library
        library = Library.builder()
                .name("Test Library")
                .description("Test library description")
                .owner(adminUser)
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(14)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        library = libraryRepository.save(library);

        // Add admin as ADMIN member
        LibraryMembership adminMembership = LibraryMembership.builder()
                .user(adminUser)
                .library(library)
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(adminMembership);

        // Add regular user as MEMBER
        LibraryMembership regularMembership = LibraryMembership.builder()
                .user(regularUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(regularMembership);

        bookRequest = BookRequest.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .publisher("Prentice Hall")
                .publicationYear(2008)
                .description("A handbook of agile software craftsmanship")
                .coverImageUrl("http://example.com/clean-code.jpg")
                .autoDigitalBorrowEnabled(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("Should create book when user is library admin")
    void testCreateBookSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO result = bookService.createBook(library.getId(), bookRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin tries to create book")
    void testCreateBookNonAdmin() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        
        assertThatThrownBy(() -> bookService.createBook(library.getId(), bookRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only library admins can create books");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when library not found")
    void testCreateBookLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        assertThatThrownBy(() -> bookService.createBook(999L, bookRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Library not found");
    }

    @Test
    @DisplayName("Should get book by id successfully")
    void testGetBookByIdSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        // Create a book first
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        // Get the book
        BookDTO result = bookService.getBookById(library.getId(), createdBook.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(createdBook.getId());
        assertThat(result.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when book not found")
    void testGetBookByIdNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        assertThatThrownBy(() -> bookService.getBookById(library.getId(), 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should throw BadRequestException when book does not belong to library")
    void testGetBookBadLibrary() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        // Create another library owned by adminUser
        Library otherLibrary = Library.builder()
                .name("Other Library")
                .owner(adminUser)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        otherLibrary = libraryRepository.save(otherLibrary);

        // Add adminUser as APPROVED member of otherLibrary so membership check passes
        LibraryMembership otherMembership = LibraryMembership.builder()
                .user(adminUser)
                .library(otherLibrary)
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(otherMembership);

        // Create book in first library
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        // Try to get book from different library — membership check passes, but book-library check fails
        Long otherLibraryId = otherLibrary.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.getBookById(otherLibraryId, bookId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Book does not belong to this library");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-member tries to read books")
    void testGetLibraryBooksNonMember() {
        // Create a user with no membership
        User outsider = User.builder()
                .email("outsider@library.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Out")
                .lastName("Sider")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        outsider = userRepository.save(outsider);
        SecurityTestUtils.setSecurityContext(outsider, "USER");

        Long libId = library.getId();
        assertThatThrownBy(() -> bookService.getLibraryBooks(libId, PageRequest.of(0, 10)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    @DisplayName("Should get library books with pagination")
    void testGetLibraryBooks() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        // Create multiple books
        for (int i = 0; i < 3; i++) {
            BookRequest req = BookRequest.builder()
                    .title("Book " + i)
                    .author("Author " + i)
                    .publicationYear(2020 + i)
                    .build();
            bookService.createBook(library.getId(), req);
        }

        // Get books with pagination
        Page<BookDTO> result = bookService.getLibraryBooks(library.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should search books by title")
    void testSearchBooksByTitle() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        // Create books with different titles
        BookRequest book1 = BookRequest.builder()
                .title("Clean Code")
                .author("Martin")
                .publicationYear(2008)
                .build();
        BookRequest book2 = BookRequest.builder()
                .title("Design Patterns")
                .author("Gang of Four")
                .publicationYear(1994)
                .build();

        bookService.createBook(library.getId(), book1);
        bookService.createBook(library.getId(), book2);

        // Search by title
        Page<BookDTO> result = bookService.searchBooks(library.getId(), "Clean", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Should search books by author")
    void testSearchBooksByAuthor() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        // Create books
        BookRequest book1 = BookRequest.builder()
                .title("Clean Code")
                .author("Robert Martin")
                .publicationYear(2008)
                .build();
        BookRequest book2 = BookRequest.builder()
                .title("Design Patterns")
                .author("Gang of Four")
                .publicationYear(1994)
                .build();

        bookService.createBook(library.getId(), book1);
        bookService.createBook(library.getId(), book2);

        // Search by author
        Page<BookDTO> result = bookService.searchBooks(library.getId(), "Robert", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor()).isEqualTo("Robert Martin");
    }

    @Test
    @DisplayName("Should update book successfully")
    void testUpdateBookSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        // Create book
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        // Update book
        BookRequest updateRequest = BookRequest.builder()
                .title("Updated Title")
                .author("Updated Author")
                .publisher("Updated Publisher")
                .publicationYear(2023)
                .description("Updated description")
                .autoDigitalBorrowEnabled(false)
                .build();

        BookDTO updatedBook = bookService.updateBook(library.getId(), createdBook.getId(), updateRequest);

        assertThat(updatedBook.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedBook.getAuthor()).isEqualTo("Updated Author");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin tries to update book")
    void testUpdateBookNonAdmin() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        BookRequest updateRequest = BookRequest.builder()
                .title("Updated Title")
                .author("Updated Author")
                .build();

        assertThatThrownBy(() -> bookService.updateBook(library.getId(), createdBook.getId(), updateRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should delete book successfully")
    void testDeleteBookSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        // Create book
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        // Delete book
        bookService.deleteBook(library.getId(), createdBook.getId());

        // Verify book is deleted
        assertThatThrownBy(() -> bookService.getBookById(library.getId(), createdBook.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
