package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.config.WithMockCustomUser;
import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.entity.*;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.BookCopyStatus;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private LibrarySubjectRepository subjectRepository;

    @Autowired
    private DigitalBookRepository digitalBookRepository;

    @Autowired
    private FileResourceRepository fileResourceRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @MockBean
    private StorageService storageService;

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
                .autoDigitalBorrowEnabled(false)
                .build();

        when(storageService.store(any(), anyString())).thenReturn("covers/test-uuid.jpg");
        when(storageService.load(anyString())).thenReturn(new ByteArrayResource("data".getBytes()));
        doNothing().when(storageService).delete(anyString());
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
                .hasMessageContaining("فقط مدیر کتابخانه");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when library not found")
    void testCreateBookLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        
        assertThatThrownBy(() -> bookService.createBook(999L, bookRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کتابخانه");
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
                .hasMessageContaining("کتابی با این شناسه");
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
                .hasMessageContaining("این کتاب مربوط به این کتابخانه نیست");
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
                .hasMessageContaining("عضو");
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

    // ---------- createBook: subjects ----------

    @Test
    @DisplayName("Should create book with valid subject ids")
    void testCreateBookWithSubjects() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        LibrarySubject subject = subjectRepository.save(LibrarySubject.builder()
                .library(library).name("Software Engineering")
                .createdAt(LocalDateTime.now()).build());

        BookRequest req = BookRequest.builder()
                .title("With Subject").author("Author")
                .subjectIds(List.of(subject.getId()))
                .build();

        BookDTO result = bookService.createBook(library.getId(), req);

        assertThat(result.getSubjectIds()).containsExactly(subject.getId());
        assertThat(result.getSubjectNames()).containsExactly("Software Engineering");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown subject id")
    void testCreateBookSubjectNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        BookRequest req = BookRequest.builder()
                .title("Bad Subject").author("Author")
                .subjectIds(List.of(999999L))
                .build();

        assertThatThrownBy(() -> bookService.createBook(library.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("موضوعی");
    }

    @Test
    @DisplayName("Should throw BadRequestException when subject belongs to another library")
    void testCreateBookSubjectWrongLibrary() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        Library otherLibrary = libraryRepository.save(Library.builder()
                .name("Other Lib").owner(adminUser).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        LibrarySubject foreignSubject = subjectRepository.save(LibrarySubject.builder()
                .library(otherLibrary).name("Foreign Subject")
                .createdAt(LocalDateTime.now()).build());

        BookRequest req = BookRequest.builder()
                .title("Wrong Lib Subject").author("Author")
                .subjectIds(List.of(foreignSubject.getId()))
                .build();

        assertThatThrownBy(() -> bookService.createBook(library.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("این موضوع مربوط به این کتابخانه نیست");
    }

    // ---------- getBookById: library not found + pending membership ----------

    @Test
    @DisplayName("Should throw ResourceNotFoundException when library does not exist for getBookById")
    void testGetBookByIdLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.getBookById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کتابخانه");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when membership is not yet approved")
    void testGetBookByIdPendingMembership() {
        User pendingUser = userRepository.save(User.builder()
                .email("pending@library.com").passwordHash("$2a$10$encoded")
                .firstName("Pending").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        membershipRepository.save(LibraryMembership.builder()
                .user(pendingUser).library(library)
                .role(LibraryMembershipRole.MEMBER).status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        SecurityTestUtils.setSecurityContext(pendingUser, "USER");
        Long libId = library.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.getBookById(libId, bookId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("تأیید نشده");
    }

    // ---------- globalSearch ----------

    @Test
    @DisplayName("Should find books across active libraries via global search")
    void testGlobalSearchMatches() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        bookService.createBook(library.getId(), bookRequest);

        Page<BookDTO> result = bookService.globalSearch("Clean", PageRequest.of(0, 10));

        assertThat(result.getContent()).anyMatch(b -> b.getTitle().equals("Clean Code"));
    }

    @Test
    @DisplayName("Should return all active-library books when global search query is blank")
    void testGlobalSearchBlankQuery() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        bookService.createBook(library.getId(), bookRequest);

        Page<BookDTO> result = bookService.globalSearch("  ", PageRequest.of(0, 10));

        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("Should exclude books from inactive libraries in global search")
    void testGlobalSearchExcludesInactiveLibrary() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        Library inactiveLibrary = Library.builder()
                .name("Inactive Lib").owner(adminUser).isActive(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        inactiveLibrary = libraryRepository.save(inactiveLibrary);
        Book inactiveBook = Book.builder()
                .library(inactiveLibrary).title("Hidden Book").author("Ghost")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        bookRepository.save(inactiveBook);

        Page<BookDTO> result = bookService.globalSearch("Hidden", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ---------- advancedSearchBooks: subject/year filters ----------

    @Test
    @DisplayName("Should filter books by subject id")
    void testAdvancedSearchBySubject() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        LibrarySubject subject = subjectRepository.save(LibrarySubject.builder()
                .library(library).name("History").createdAt(LocalDateTime.now()).build());

        BookRequest withSubject = BookRequest.builder()
                .title("History Book").author("Author").subjectIds(List.of(subject.getId())).build();
        BookRequest withoutSubject = BookRequest.builder()
                .title("Other Book").author("Author").build();
        bookService.createBook(library.getId(), withSubject);
        bookService.createBook(library.getId(), withoutSubject);

        Page<BookDTO> result = bookService.advancedSearchBooks(
                library.getId(), null, subject.getId(), null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("History Book");
    }

    @Test
    @DisplayName("Should filter books by year range")
    void testAdvancedSearchByYearRange() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("Old Book").author("Author").publicationYear(1990).build());
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("New Book").author("Author").publicationYear(2020).build());

        Page<BookDTO> result = bookService.advancedSearchBooks(
                library.getId(), null, null, 2000, 2025, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("New Book");
    }

    @Test
    @DisplayName("Should filter books by yearFrom only")
    void testAdvancedSearchByYearFromOnly() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("Old Book").author("Author").publicationYear(1990).build());
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("New Book").author("Author").publicationYear(2020).build());

        Page<BookDTO> result = bookService.advancedSearchBooks(
                library.getId(), null, null, 2000, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("New Book");
    }

    @Test
    @DisplayName("Should filter books by yearTo only")
    void testAdvancedSearchByYearToOnly() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("Old Book").author("Author").publicationYear(1990).build());
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("New Book").author("Author").publicationYear(2020).build());

        Page<BookDTO> result = bookService.advancedSearchBooks(
                library.getId(), null, null, null, 2000, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Old Book");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for advanced search on unknown library")
    void testAdvancedSearchLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.advancedSearchBooks(999L, null, null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- updateBook: not found / bad-library branches ----------

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent library")
    void testUpdateBookLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.updateBook(999L, 1L, bookRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent book")
    void testUpdateBookNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.updateBook(library.getId(), 999L, bookRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کتابی با این شناسه");
    }

    @Test
    @DisplayName("Should throw BadRequestException when updating a book from a different library")
    void testUpdateBookWrongLibrary() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        Library otherLibrary = libraryRepository.save(Library.builder()
                .name("Other Lib").owner(adminUser).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(otherLibrary)
                .role(LibraryMembershipRole.ADMIN).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Long otherLibId = otherLibrary.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.updateBook(otherLibId, bookId, bookRequest))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------- deleteBook: not found / unauthorized / bad-library branches ----------

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting from a non-existent library")
    void testDeleteBookLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.deleteBook(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-member deletes a book")
    void testDeleteBookNonMember() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        User outsider = userRepository.save(User.builder()
                .email("outsider-del@library.com").passwordHash("$2a$10$encoded")
                .firstName("Out").lastName("Sider")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(outsider, "USER");

        Long libId = library.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.deleteBook(libId, bookId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting a non-existent book")
    void testDeleteBookNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.deleteBook(library.getId(), 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کتابی با این شناسه");
    }

    @Test
    @DisplayName("Should throw BadRequestException when deleting a book from a different library")
    void testDeleteBookWrongLibrary() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        Library otherLibrary = libraryRepository.save(Library.builder()
                .name("Other Lib 2").owner(adminUser).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(otherLibrary)
                .role(LibraryMembershipRole.ADMIN).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Long otherLibId = otherLibrary.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.deleteBook(otherLibId, bookId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should soft-delete (not hard-delete/throw) a book with borrow history, preserving the row")
    void testDeleteBookWithBorrowHistory_softDeletesInsteadOfThrowing() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        bookService.addBookCopies(library.getId(), createdBook.getId(), 1);
        BookCopy copy = bookCopyRepository.findByBookId(createdBook.getId()).get(0);

        borrowRepository.save(Borrow.builder()
                .user(adminUser).library(library).book(bookRepository.findById(createdBook.getId()).orElseThrow())
                .bookCopy(copy)
                .borrowType(com.library.entity.enums.BorrowType.PHYSICAL)
                .status(com.library.entity.enums.BorrowStatus.RETURNED)
                .returnDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Long libId = library.getId();
        Long bookId = createdBook.getId();
        bookService.deleteBook(libId, bookId);

        // Row must still exist (soft delete), but marked deleted and hidden from normal access.
        Book deleted = bookRepository.findById(bookId).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThatThrownBy(() -> bookService.getBookById(libId, bookId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw BadRequestException when deleting an already-deleted book")
    void testDeleteBookAlreadyDeleted_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        Long libId = library.getId();
        Long bookId = createdBook.getId();
        bookService.deleteBook(libId, bookId);

        assertThatThrownBy(() -> bookService.deleteBook(libId, bookId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("قبلاً حذف شده");
    }

    @Test
    @DisplayName("Should restore a soft-deleted book, making it visible again")
    void testRestoreBook_makesBookVisibleAgain() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        Long libId = library.getId();
        Long bookId = createdBook.getId();
        bookService.deleteBook(libId, bookId);

        BookDTO restored = bookService.restoreBook(libId, bookId);

        assertThat(restored.getDeletedAt()).isNull();
        assertThat(bookService.getBookById(libId, bookId).getId()).isEqualTo(bookId);
    }

    @Test
    @DisplayName("Should throw BadRequestException when restoring a book that is not deleted")
    void testRestoreBook_notDeleted_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        Long libId = library.getId();
        Long bookId = createdBook.getId();

        assertThatThrownBy(() -> bookService.restoreBook(libId, bookId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("حذف نشده");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when a non-admin tries to restore a book")
    void testRestoreBook_nonAdmin_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        Long libId = library.getId();
        Long bookId = createdBook.getId();
        bookService.deleteBook(libId, bookId);

        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        assertThatThrownBy(() -> bookService.restoreBook(libId, bookId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should list a soft-deleted book via getDeletedBooks and exclude it from getLibraryBooks/searchBooks")
    void testGetDeletedBooks_listsDeletedAndHidesFromNormalListings() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        Long libId = library.getId();
        Long bookId = createdBook.getId();
        bookService.deleteBook(libId, bookId);

        Page<BookDTO> deleted = bookService.getDeletedBooks(libId, PageRequest.of(0, 10));
        assertThat(deleted.getContent()).extracting(BookDTO::getId).contains(bookId);

        Page<BookDTO> normalListing = bookService.getLibraryBooks(libId, PageRequest.of(0, 10));
        assertThat(normalListing.getContent()).extracting(BookDTO::getId).doesNotContain(bookId);

        Page<BookDTO> searchResults = bookService.searchBooks(libId, bookRequest.getTitle(), PageRequest.of(0, 10));
        assertThat(searchResults.getContent()).extracting(BookDTO::getId).doesNotContain(bookId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when a non-admin lists deleted books")
    void testGetDeletedBooks_nonAdmin_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        Long libId = library.getId();
        assertThatThrownBy(() -> bookService.getDeletedBooks(libId, PageRequest.of(0, 10)))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---------- addBookCopies ----------

    @Test
    @DisplayName("Should add copies with incrementing copy numbers")
    void testAddBookCopiesSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        bookService.addBookCopies(library.getId(), createdBook.getId(), 3);

        List<BookCopy> copies = bookCopyRepository.findByBookId(createdBook.getId());
        assertThat(copies).hasSize(3);
        assertThat(copies.stream().map(BookCopy::getCopyNumber)).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(copies).allMatch(c -> c.getStatus() == BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when adding copies to unknown library")
    void testAddBookCopiesLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.addBookCopies(999L, 1L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin adds copies")
    void testAddBookCopiesNonAdmin() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        Long libId = library.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.addBookCopies(libId, bookId, 1))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when adding copies to unknown book")
    void testAddBookCopiesBookNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.addBookCopies(library.getId(), 999L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کتابی با این شناسه");
    }

    @Test
    @DisplayName("Should throw BadRequestException when adding copies for a book in a different library")
    void testAddBookCopiesWrongLibrary() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        Library otherLibrary = libraryRepository.save(Library.builder()
                .name("Other Lib 3").owner(adminUser).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(otherLibrary)
                .role(LibraryMembershipRole.ADMIN).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Long otherLibId = otherLibrary.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.addBookCopies(otherLibId, bookId, 1))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------- setBookCopyCount ----------

    @Test
    @DisplayName("Should increase copy count by adding new copies")
    void testSetBookCopyCountIncrease() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        bookService.addBookCopies(library.getId(), createdBook.getId(), 2);

        BookDTO result = bookService.setBookCopyCount(library.getId(), createdBook.getId(), 5);

        assertThat(result.getTotalCopiesCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should decrease copy count by removing available copies")
    void testSetBookCopyCountDecrease() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        bookService.addBookCopies(library.getId(), createdBook.getId(), 5);

        BookDTO result = bookService.setBookCopyCount(library.getId(), createdBook.getId(), 2);

        assertThat(result.getTotalCopiesCount()).isEqualTo(2);
        List<BookCopy> remaining = bookCopyRepository.findByBookId(createdBook.getId());
        assertThat(remaining).hasSize(2);
        assertThat(remaining.stream().map(BookCopy::getCopyNumber)).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("Should keep copy count unchanged when target equals current total")
    void testSetBookCopyCountUnchanged() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        bookService.addBookCopies(library.getId(), createdBook.getId(), 3);

        BookDTO result = bookService.setBookCopyCount(library.getId(), createdBook.getId(), 3);

        assertThat(result.getTotalCopiesCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should let SYSTEM_ADMIN set copy count without library membership admin role")
    void testSetBookCopyCountAsSystemAdmin() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        bookService.addBookCopies(library.getId(), createdBook.getId(), 5);

        SecurityTestUtils.setSecurityContext(regularUser, "SYSTEM_ADMIN");
        BookDTO result = bookService.setBookCopyCount(library.getId(), createdBook.getId(), 2);

        assertThat(result.getTotalCopiesCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin non-system-admin sets copy count")
    void testSetBookCopyCountUnauthorized() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        Long libId = library.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.setBookCopyCount(libId, bookId, 4))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when setting copy count on unknown library")
    void testSetBookCopyCountLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.setBookCopyCount(999L, 1L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when setting copy count on unknown book")
    void testSetBookCopyCountBookNotFound() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> bookService.setBookCopyCount(library.getId(), 999L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کتابی با این شناسه");
    }

    @Test
    @DisplayName("Should throw BadRequestException when setting copy count for a book in a different library")
    void testSetBookCopyCountWrongLibrary() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        Library otherLibrary = libraryRepository.save(Library.builder()
                .name("Other Lib 4").owner(adminUser).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(otherLibrary)
                .role(LibraryMembershipRole.ADMIN).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Long otherLibId = otherLibrary.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.setBookCopyCount(otherLibId, bookId, 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw BadRequestException when target count is negative")
    void testSetBookCopyCountNegative() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);

        Long libId = library.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.setBookCopyCount(libId, bookId, -1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("تعداد نسخه نامعتبر است");
    }

    @Test
    @DisplayName("Should throw BadRequestException when target count is below copies currently on loan")
    void testSetBookCopyCountBelowOnLoan() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        bookService.addBookCopies(library.getId(), createdBook.getId(), 3);

        List<BookCopy> copies = bookCopyRepository.findByBookId(createdBook.getId());
        BookCopy onLoanCopy = copies.get(0);
        onLoanCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(onLoanCopy);

        Long libId = library.getId();
        Long bookId = createdBook.getId();
        assertThatThrownBy(() -> bookService.setBookCopyCount(libId, bookId, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در امانت است");
    }

    @Test
    @DisplayName("Should throw BadRequestException (not a DB constraint 500) when reducing count " +
            "would require deleting an AVAILABLE copy that has borrow history")
    void testSetBookCopyCountAvailableCopyWithHistory_throwsInsteadOf500() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        bookService.addBookCopies(library.getId(), createdBook.getId(), 2);

        // Simulate a copy that was borrowed and returned: status is AVAILABLE again,
        // but a Borrow row still references it via book_copy_id.
        List<BookCopy> copies = bookCopyRepository.findByBookId(createdBook.getId());
        BookCopy historyCopy = copies.get(0);
        borrowRepository.save(Borrow.builder()
                .user(adminUser).library(library).book(bookRepository.findById(createdBook.getId()).orElseThrow())
                .bookCopy(historyCopy)
                .borrowType(com.library.entity.enums.BorrowType.PHYSICAL)
                .status(com.library.entity.enums.BorrowStatus.RETURNED)
                .returnDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Long libId = library.getId();
        Long bookId = createdBook.getId();
        // Both copies are AVAILABLE, but only 1 has no history — asking to drop to 0
        // must fail cleanly instead of hitting the FK constraint on delete.
        assertThatThrownBy(() -> bookService.setBookCopyCount(libId, bookId, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("سابقه‌ی امانت");

        // And the deletable copy must not have been removed by a partial/failed attempt.
        assertThat(bookCopyRepository.findByBookId(bookId)).hasSize(2);
    }

    // ---------- consolidated create/update/patch with assets ----------

    @Test
    @DisplayName("createBookWithAssets: creates book and applies cover + digital + copy count in one call")
    void testCreateBookWithAssets_allAssets() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", "img".getBytes());
        MockMultipartFile digital = new MockMultipartFile("digital", "book.pdf", "application/pdf", "pdf".getBytes());

        BookDTO result = bookService.createBookWithAssets(library.getId(), bookRequest, cover, digital, "v1", 3);

        assertThat(result.getTotalCopiesCount()).isEqualTo(3);
        assertThat(result.getCoverImageUrl()).isNotNull();
        assertThat(result.getHasDigitalVersions()).isTrue();
    }

    @Test
    @DisplayName("createBookWithAssets: all assets optional — plain metadata-only create still works")
    void testCreateBookWithAssets_noAssets() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        BookDTO result = bookService.createBookWithAssets(library.getId(), bookRequest, null, null, null, null);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTotalCopiesCount()).isEqualTo(0);
        assertThat(result.getCoverImageUrl()).isNull();
    }

    @Test
    @DisplayName("updateBookWithAssets: replaces metadata and applies cover + copy count together")
    void testUpdateBookWithAssets() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO created = bookService.createBook(library.getId(), bookRequest);
        MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", "img".getBytes());

        BookRequest updateRequest = BookRequest.builder()
                .title("Clean Code 2nd Edition").author("Robert C. Martin")
                .publisher("Prentice Hall").publicationYear(2020)
                .autoDigitalBorrowEnabled(false)
                .build();

        BookDTO result = bookService.updateBookWithAssets(
                library.getId(), created.getId(), updateRequest, cover, null, null, 4);

        assertThat(result.getTitle()).isEqualTo("Clean Code 2nd Edition");
        assertThat(result.getTotalCopiesCount()).isEqualTo(4);
        assertThat(result.getCoverImageUrl()).isNotNull();
    }

    @Test
    @DisplayName("patchBookWithAssets: only supplied metadata field changes, rest is untouched")
    void testPatchBookWithAssets_partialMetadataOnly() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO created = bookService.createBook(library.getId(), bookRequest);

        BookRequest partial = BookRequest.builder().publicationYear(2099).build();
        BookDTO result = bookService.patchBookWithAssets(
                library.getId(), created.getId(), partial, null, null, null, null);

        assertThat(result.getPublicationYear()).isEqualTo(2099);
        assertThat(result.getTitle()).isEqualTo("Clean Code"); // untouched
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin"); // untouched
    }

    @Test
    @DisplayName("patchBookWithAssets: request part entirely absent — only the asset (copy count) changes")
    void testPatchBookWithAssets_assetsOnlyNoMetadata() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO created = bookService.createBook(library.getId(), bookRequest);

        BookDTO result = bookService.patchBookWithAssets(
                library.getId(), created.getId(), null, null, null, null, 2);

        assertThat(result.getTotalCopiesCount()).isEqualTo(2);
        assertThat(result.getTitle()).isEqualTo("Clean Code"); // untouched
    }

    @Test
    @DisplayName("patchBookWithAssets: blank title in the partial request is rejected")
    void testPatchBookWithAssets_blankTitleRejected() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO created = bookService.createBook(library.getId(), bookRequest);
        Long libId = library.getId();
        Long bookId = created.getId();

        BookRequest partial = BookRequest.builder().title("  ").build();
        assertThatThrownBy(() -> bookService.patchBookWithAssets(libId, bookId, partial, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("عنوان کتاب را وارد کنید");
    }

    @Test
    @DisplayName("patchBookWithAssets: non-admin is rejected even when only assets are supplied (no metadata part)")
    void testPatchBookWithAssets_nonAdminRejectedAssetsOnly() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO created = bookService.createBook(library.getId(), bookRequest);
        Long libId = library.getId();
        Long bookId = created.getId();

        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        assertThatThrownBy(() -> bookService.patchBookWithAssets(libId, bookId, null, null, null, null, 5))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---------- mapToBookDTO: cover image + digital versions ----------

    @Test
    @DisplayName("Should populate cover image URL and digital-version flag in DTO")
    void testMapToBookDTOWithCoverAndDigitalVersion() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BookDTO createdBook = bookService.createBook(library.getId(), bookRequest);
        Book book = bookRepository.findById(createdBook.getId()).orElseThrow();

        FileResource coverResource = fileResourceRepository.save(FileResource.builder()
                .originalFilename("cover.png").storedFilename("stored-cover.png")
                .filePath("/covers/stored-cover.png").fileSizeBytes(1024L)
                .contentType("image/png").checksumSha256("abc123")
                .uploadedBy(adminUser).createdAt(LocalDateTime.now()).build());
        book.setCoverImage(coverResource);
        book = bookRepository.save(book);

        FileResource digitalResource = fileResourceRepository.save(FileResource.builder()
                .originalFilename("book.pdf").storedFilename("stored-book.pdf")
                .filePath("/digital/stored-book.pdf").fileSizeBytes(2048L)
                .contentType("application/pdf").checksumSha256("def456")
                .uploadedBy(adminUser).createdAt(LocalDateTime.now()).build());
        digitalBookRepository.save(DigitalBook.builder()
                .book(book).fileResource(digitalResource).fileFormat("PDF")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BookDTO result = bookService.mapToBookDTO(book);

        assertThat(result.getCoverImageUrl()).isEqualTo("/api/v1/files/" + coverResource.getId());
        assertThat(result.getCoverImageFileResourceId()).isEqualTo(coverResource.getId());
        assertThat(result.getHasDigitalVersions()).isTrue();
    }
}
