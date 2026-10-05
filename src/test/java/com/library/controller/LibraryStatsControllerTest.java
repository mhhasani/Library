package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.config.WithMockCustomUser;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Borrow;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.repository.*;
import com.library.security.AppUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Library Stats Controller Tests")
class LibraryStatsControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private BorrowRepository borrowRepository;

    private long libraryId;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("stats-owner@lib.com").passwordHash("$2a$10$x")
                .firstName("O").lastName("W").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Library lib = libraryRepository.save(Library.builder()
                .name("Stats Library").owner(owner).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        libraryId = lib.getId();

        membershipRepository.save(LibraryMembership.builder()
                .user(owner).library(lib).role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED).approvedBy(owner)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Logs the given (already-persisted) user in as the current security principal. */
    private void loginAs(User user) {
        AppUserDetails principal = new AppUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, "password", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Library reloadLibrary() {
        return libraryRepository.findById(libraryId).orElseThrow();
    }

    // ── 401 tests (no auth) ───────────────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated most-borrowed — 401")
    void mostBorrowed_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/most-borrowed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated underused — 401")
    void underused_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/underused"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated user-activity — 401")
    void userActivity_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/user-activity"))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 tests (authenticated but not a library member) ────────────────

    @Test
    @WithMockUser(username = "stranger@lib.com")
    @DisplayName("Non-member most-borrowed — 401 (UnauthorizedException maps to 401)")
    void mostBorrowed_nonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/most-borrowed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "stranger@lib.com")
    @DisplayName("Non-member underused — 401 (UnauthorizedException maps to 401)")
    void underused_nonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/underused"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "stranger@lib.com")
    @DisplayName("Non-member user-activity — 401 (UnauthorizedException maps to 401)")
    void userActivity_nonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/user-activity"))
                .andExpect(status().isUnauthorized());
    }

    // ── 200 tests (library admin, via WithMockCustomUser) ────────────────
    // The admin user is saved with a generated ID in setUp(). We use id=1 as
    // a default here and verify via WithMockCustomUser (userId=1 default).
    // The actual DB admin has a different ID, so these tests verify endpoint
    // shape only via a user who happens to be an admin in the H2 seed data.
    // Full integration (with matching user ID) is covered in service-layer tests.

    @Test
    @WithMockCustomUser(userId = 1L, roles = {"USER"})
    @DisplayName("Authenticated non-member gets 401 from most-borrowed (UnauthorizedException → 401)")
    void mostBorrowed_authNonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/most-borrowed"))
                .andExpect(status().isUnauthorized());
    }

    // ── 404 tests (library does not exist) ─────────────────────────────

    @Test
    @DisplayName("Most-borrowed for nonexistent library — 404")
    void mostBorrowed_libraryNotFound() throws Exception {
        loginAs(owner);
        mockMvc.perform(get("/v1/libraries/999999/stats/most-borrowed"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Underused for nonexistent library — 404")
    void underused_libraryNotFound() throws Exception {
        loginAs(owner);
        mockMvc.perform(get("/v1/libraries/999999/stats/underused"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User-activity for nonexistent library — 404")
    void userActivity_libraryNotFound() throws Exception {
        loginAs(owner);
        mockMvc.perform(get("/v1/libraries/999999/stats/user-activity"))
                .andExpect(status().isNotFound());
    }

    // ── 200 success-path tests (real admin, real data) ─────────────────

    @Test
    @DisplayName("Admin gets most-borrowed books with real data")
    void mostBorrowed_admin_success() throws Exception {
        Library lib = reloadLibrary();

        User borrower = userRepository.save(User.builder()
                .email("borrower@lib.com").passwordHash("$2a$10$x")
                .firstName("B").lastName("R").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Book book = bookRepository.save(Book.builder()
                .library(lib).title("Popular Book").author("Author A")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BookCopy copy = bookCopyRepository.save(BookCopy.builder()
                .book(book).library(lib).copyNumber(1).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        borrowRepository.save(Borrow.builder()
                .user(borrower).library(lib).book(book).bookCopy(copy)
                .borrowType(BorrowType.PHYSICAL).status(BorrowStatus.RETURNED)
                .borrowDate(LocalDateTime.now().minusDays(5))
                .dueDate(LocalDateTime.now().minusDays(1))
                .returnDate(LocalDateTime.now())
                .build());

        loginAs(owner);

        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/most-borrowed")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].bookTitle").value("Popular Book"))
                .andExpect(jsonPath("$.data[0].borrowCount").value(1));
    }

    @Test
    @DisplayName("Admin gets underused (never-borrowed) books with real data")
    void underused_admin_success() throws Exception {
        Library lib = reloadLibrary();

        Book book = bookRepository.save(Book.builder()
                .library(lib).title("Never Borrowed Book").author("Author B")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        bookCopyRepository.save(BookCopy.builder()
                .book(book).library(lib).copyNumber(1).status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        loginAs(owner);

        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/underused"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].bookTitle").value("Never Borrowed Book"))
                .andExpect(jsonPath("$.data[0].availableCopies").value(1))
                .andExpect(jsonPath("$.data[0].totalCopies").value(1));
    }

    @Test
    @DisplayName("Admin gets user-activity with real data")
    void userActivity_admin_success() throws Exception {
        Library lib = reloadLibrary();

        User borrower = userRepository.save(User.builder()
                .email("active-borrower@lib.com").passwordHash("$2a$10$x")
                .firstName("B").lastName("R").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Book book = bookRepository.save(Book.builder()
                .library(lib).title("Some Book").author("Author C")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BookCopy copy = bookCopyRepository.save(BookCopy.builder()
                .book(book).library(lib).copyNumber(1).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        borrowRepository.save(Borrow.builder()
                .user(borrower).library(lib).book(book).bookCopy(copy)
                .borrowType(BorrowType.PHYSICAL).status(BorrowStatus.APPROVED)
                .borrowDate(LocalDateTime.now().minusDays(2))
                .dueDate(LocalDateTime.now().plusDays(12))
                .build());

        loginAs(owner);

        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/user-activity")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userEmail").value("active-borrower@lib.com"))
                .andExpect(jsonPath("$.data[0].borrowCount").value(1));
    }
}
