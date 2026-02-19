package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.AddBookCopyRequest;
import com.library.dto.BookRequest;
import com.library.dto.BookDTO;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
public class BookServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Library testLibrary;
    private Book testBook;

    @BeforeEach
    void setUp() {
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        membershipRepository.deleteAll();
        libraryRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("admin@example.com")
                .passwordHash("hashedpassword")
                .firstName("Admin")
                .lastName("User")
                .systemRole(SystemRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        // Create test library
        testLibrary = Library.builder()
                .name("Test Library")
                .description("A test library")
                .ownerId(testUser.getId())
                .location("Test City")
                .phone("+1234567890")
                .email("library@example.com")
                .isActive(true)
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(14)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testLibrary = libraryRepository.save(testLibrary);

        // Add user as admin member
        LibraryMembership membership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(testUser.getId())
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedBy(testUser.getId())
                .build();
        membershipRepository.save(membership);

        // Create test book
        testBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Test Book")
                .author("Test Author")
                .isbn("123-456-789")
                .description("A test book")
                .publishedYear(2023)
                .publisher("Test Publisher")
                .category("Fiction")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testBook = bookRepository.save(testBook);
    }

    @Test
    void testCreateBook() {
        BookRequest request = BookRequest.builder()
                .title("New Book")
                .author("New Author")
                .isbn("999-888-777")
                .description("A new book")
                .publishedYear(2024)
                .publisher("New Publisher")
                .category("Science Fiction")
                .language("English")
                .autoDigitalBorrowEnabled(true)
                .build();

        BookDTO response = bookService.createBook(testLibrary.getId(), request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("New Book", response.getTitle());
        assertEquals("New Author", response.getAuthor());
        assertEquals("999-888-777", response.getIsbn());
    }

    @Test
    void testGetBookById() {
        BookDTO response = bookService.getBookById(testLibrary.getId(), testBook.getId());

        assertNotNull(response);
        assertEquals(testBook.getId(), response.getId());
        assertEquals("Test Book", response.getTitle());
    }

    @Test
    void testGetBookByIdNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> bookService.getBookById(testLibrary.getId(), 99999L));
    }

    @Test
    void testGetLibraryBooks() {
        // Create another book
        Book secondBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Second Book")
                .author("Second Author")
                .isbn("111-222-333")
                .description("Another test book")
                .publishedYear(2022)
                .publisher("Publisher Two")
                .category("Mystery")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bookRepository.save(secondBook);

        List<BookDTO> books = bookService.getLibraryBooks(testLibrary.getId());

        assertNotNull(books);
        assertEquals(2, books.size());
        assertTrue(books.stream().anyMatch(b -> b.getTitle().equals("Test Book")));
        assertTrue(books.stream().anyMatch(b -> b.getTitle().equals("Second Book")));
    }

    @Test
    void testSearchBooksByTitle() {
        // Create another book
        Book secondBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Different Title")
                .author("Test Author")
                .isbn("111-222-333")
                .description("Another book")
                .publishedYear(2022)
                .publisher("Publisher Two")
                .category("Mystery")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bookRepository.save(secondBook);

        List<BookDTO> results = bookService.searchBooks(testLibrary.getId(), "Test", null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Book", results.get(0).getTitle());
    }

    @Test
    void testSearchBooksByAuthor() {
        // Create another book
        Book secondBook = Book.builder()
                .libraryId(testLibrary.getId())
                .title("Different Title")
                .author("Different Author")
                .isbn("111-222-333")
                .description("Another book")
                .publishedYear(2022)
                .publisher("Publisher Two")
                .category("Mystery")
                .language("English")
                .isActive(true)
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bookRepository.save(secondBook);

        List<BookDTO> results = bookService.searchBooks(testLibrary.getId(), null, "Test Author");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Author", results.get(0).getAuthor());
    }

    @Test
    void testSearchBooksNoResults() {
        List<BookDTO> results = bookService.searchBooks(testLibrary.getId(), "Nonexistent", null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testUpdateBook() {
        BookRequest updateRequest = BookRequest.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn("123-456-789")
                .description("Updated description")
                .publishedYear(2024)
                .publisher("Updated Publisher")
                .category("Updated Category")
                .language("French")
                .autoDigitalBorrowEnabled(true)
                .build();

        BookDTO response = bookService.updateBook(testLibrary.getId(), testBook.getId(), updateRequest);

        assertEquals("Updated Title", response.getTitle());
        assertEquals("Updated Author", response.getAuthor());
        assertEquals(true, response.getAutoDigitalBorrowEnabled());
    }

    @Test
    void testDeleteBook() {
        Long bookId = testBook.getId();

        bookService.deleteBook(testLibrary.getId(), bookId);

        Optional<Book> deleted = bookRepository.findById(bookId);
        assertTrue(deleted.isEmpty());
    }

    @Test
    void testAddBookCopies() {
        AddBookCopyRequest request = AddBookCopyRequest.builder()
                .quantity(5)
                .build();

        bookService.addBookCopies(testLibrary.getId(), testBook.getId(), request);

        List<BookCopy> copies = bookCopyRepository.findByBookId(testBook.getId());

        assertEquals(5, copies.size());
        assertTrue(copies.stream().allMatch(c -> c.getStatus() == BookCopyStatus.AVAILABLE));
    }

    @Test
    void testAddBookCopiesWithExistingCopies() {
        // Add first batch of copies
        AddBookCopyRequest firstRequest = AddBookCopyRequest.builder()
                .quantity(3)
                .build();
        bookService.addBookCopies(testLibrary.getId(), testBook.getId(), firstRequest);

        // Add second batch of copies
        AddBookCopyRequest secondRequest = AddBookCopyRequest.builder()
                .quantity(2)
                .build();
        bookService.addBookCopies(testLibrary.getId(), testBook.getId(), secondRequest);

        List<BookCopy> copies = bookCopyRepository.findByBookId(testBook.getId());

        assertEquals(5, copies.size());
        // Verify copy numbers are sequential
        assertTrue(copies.stream().allMatch(c -> c.getCopyNumber() > 0 && c.getCopyNumber() <= 5));
    }

    @Test
    void testGetAvailableCopiesCount() {
        // Add copies
        AddBookCopyRequest request = AddBookCopyRequest.builder()
                .quantity(3)
                .build();
        bookService.addBookCopies(testLibrary.getId(), testBook.getId(), request);

        BookDTO bookDTO = bookService.getBookById(testLibrary.getId(), testBook.getId());

        assertEquals(3, bookDTO.getAvailableCopiesCount());
        assertEquals(3, bookDTO.getTotalCopiesCount());
    }

    @Test
    void testGetAvailableCopiesCountAfterBorrow() {
        // Add copies
        AddBookCopyRequest request = AddBookCopyRequest.builder()
                .quantity(3)
                .build();
        bookService.addBookCopies(testLibrary.getId(), testBook.getId(), request);

        // Get one copy and change status to BORROWED
        List<BookCopy> copies = bookCopyRepository.findByBookId(testBook.getId());
        BookCopy firstCopy = copies.get(0);
        firstCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(firstCopy);

        BookDTO bookDTO = bookService.getBookById(testLibrary.getId(), testBook.getId());

        assertEquals(2, bookDTO.getAvailableCopiesCount());
        assertEquals(3, bookDTO.getTotalCopiesCount());
    }
}
