package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.AddBookCopyRequest;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
public class BookControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

        LibraryMembership adminMembership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(testUser.getId())
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedBy(testUser.getId())
                .build();
        membershipRepository.save(adminMembership);

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
    void testCreateBook() throws Exception {
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

        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", equalTo("New Book")))
                .andExpect(jsonPath("$.data.author", equalTo("New Author")))
                .andExpect(jsonPath("$.data.isbn", equalTo("999-888-777")));
    }

    @Test
    void testGetBookById() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/books/" + testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", equalTo("Test Book")))
                .andExpect(jsonPath("$.data.author", equalTo("Test Author")));
    }

    @Test
    void testGetBookNotFound() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/books/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testGetLibraryBooks() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/books")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testSearchBooksByTitle() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/books/search")
                        .param("title", "Test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testSearchBooksByAuthor() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/books/search")
                        .param("author", "Test Author")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testUpdateBook() throws Exception {
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

        mockMvc.perform(put("/v1/libraries/" + testLibrary.getId() + "/books/" + testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", equalTo("Updated Title")))
                .andExpect(jsonPath("$.data.author", equalTo("Updated Author")));
    }

    @Test
    void testDeleteBook() throws Exception {
        mockMvc.perform(delete("/v1/libraries/" + testLibrary.getId() + "/books/" + testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify book is deleted
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId() + "/books/" + testBook.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddBookCopies() throws Exception {
        AddBookCopyRequest request = AddBookCopyRequest.builder()
                .quantity(5)
                .build();

        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/books/" + testBook.getId() + "/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify copies were added
        int copyCount = bookCopyRepository.findByBookId(testBook.getId()).size();
        assert copyCount == 5;
    }
}
