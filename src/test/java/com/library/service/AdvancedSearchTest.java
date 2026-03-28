package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.dto.LibrarySubjectDTO;
import com.library.entity.*;
import com.library.entity.enums.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Advanced Book Search Tests")
class AdvancedSearchTest extends BaseIntegrationTest {

    @Autowired private BookService bookService;
    @Autowired private LibrarySubjectService subjectService;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;

    private User adminUser;
    private Library library;
    private LibrarySubjectDTO scienceSubject;
    private LibrarySubjectDTO historySubject;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("search-admin@test.com").passwordHash("$2a$10$x")
                .firstName("Admin").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        library = libraryRepository.save(Library.builder()
                .name("Search Test Library").owner(adminUser)
                .autoMembershipApproval(false).defaultBorrowDurationDays(14)
                .isActive(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(library).role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        scienceSubject = subjectService.createSubject(library.getId(), "علوم");
        historySubject = subjectService.createSubject(library.getId(), "تاریخ");

        // Create test books
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("مبانی فیزیک").author("هالیدی").publicationYear(2010)
                .subjectIds(List.of(scienceSubject.getId())).autoDigitalBorrowEnabled(false).build());

        bookService.createBook(library.getId(), BookRequest.builder()
                .title("تاریخ ایران باستان").author("پیرنیا").publicationYear(1990)
                .subjectIds(List.of(historySubject.getId())).autoDigitalBorrowEnabled(false).build());

        bookService.createBook(library.getId(), BookRequest.builder()
                .title("شیمی آلی").author("موریسون").publicationYear(2015)
                .subjectIds(List.of(scienceSubject.getId())).autoDigitalBorrowEnabled(false).build());
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("Search by text query matches title")
    void searchByQuery_matchesTitle() {
        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), "فیزیک", null, null, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTitle()).contains("فیزیک");
    }

    @Test
    @DisplayName("Filter by subjectId returns only books in that subject")
    void filterBySubjectId_returnsOnlyMatchingBooks() {
        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), null, scienceSubject.getId(), null, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(b -> b.getSubjectIds().contains(scienceSubject.getId()));
    }

    @Test
    @DisplayName("Filter by year range returns matching books")
    void filterByYearRange_returnsMatchingBooks() {
        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), null, null, 2000, 2020, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(b -> b.getPublicationYear() >= 2000 && b.getPublicationYear() <= 2020);
    }

    @Test
    @DisplayName("Combined subject and year filter narrows results")
    void filterBySubjectAndYear_narrowsResults() {
        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), null, scienceSubject.getId(), 2012, 2020, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTitle()).isEqualTo("شیمی آلی");
    }

    @Test
    @DisplayName("No matching filters returns empty page")
    void noMatchingFilters_returnsEmpty() {
        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), "ژنتیک", null, null, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Book DTO includes subject names list")
    void bookDTO_includesSubjectNames() {
        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), "فیزیک", null, null, null, PageRequest.of(0, 10));

        BookDTO book = results.getContent().get(0);
        assertThat(book.getSubjectNames()).containsExactly("علوم");
        assertThat(book.getSubjectIds()).containsExactly(scienceSubject.getId());
    }

    @Test
    @DisplayName("Book can have multiple subjects (many-to-many)")
    void bookWithMultipleSubjects_returnsBothSubjectIds() {
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("تاریخ علم").author("نویسنده").publicationYear(2000)
                .subjectIds(List.of(scienceSubject.getId(), historySubject.getId()))
                .autoDigitalBorrowEnabled(false).build());

        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), "تاریخ علم", null, null, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        BookDTO book = results.getContent().get(0);
        assertThat(book.getSubjectIds()).containsExactlyInAnyOrder(scienceSubject.getId(), historySubject.getId());
        assertThat(book.getSubjectNames()).containsExactlyInAnyOrder("علوم", "تاریخ");
    }

    @Test
    @DisplayName("Filtering by subject also returns multi-subject books that include that subject")
    void filterBySubject_includesMultiSubjectBooks() {
        // Book with both subjects
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("تاریخ علم").author("نویسنده").publicationYear(2000)
                .subjectIds(List.of(scienceSubject.getId(), historySubject.getId()))
                .autoDigitalBorrowEnabled(false).build());

        Page<BookDTO> scienceResults = bookService.advancedSearchBooks(
                library.getId(), null, scienceSubject.getId(), null, null, PageRequest.of(0, 10));

        // Should return: مبانی فیزیک, شیمی آلی, تاریخ علم (3 books)
        assertThat(scienceResults.getContent()).hasSize(3);
        assertThat(scienceResults.getContent()).allMatch(b -> b.getSubjectIds().contains(scienceSubject.getId()));
    }

    @Test
    @DisplayName("Deleting a subject removes it from all associated books")
    void deleteSubject_removesFromBooks() {
        // Verify subject is assigned
        Page<BookDTO> before = bookService.advancedSearchBooks(
                library.getId(), null, scienceSubject.getId(), null, null, PageRequest.of(0, 10));
        assertThat(before.getContent()).hasSize(2);

        // Delete subject
        subjectService.deleteSubject(library.getId(), scienceSubject.getId());

        // Now books should have no scienceSubject
        Page<BookDTO> after = bookService.advancedSearchBooks(
                library.getId(), null, null, null, null, PageRequest.of(0, 10));
        assertThat(after.getContent()).allMatch(b -> !b.getSubjectIds().contains(scienceSubject.getId()));
    }

    @Test
    @DisplayName("bookCount in subject DTO reflects number of books using it")
    void subjectDTO_bookCount_isAccurate() {
        // scienceSubject should have 2 books (مبانی فیزیک + شیمی آلی)
        var subjects = subjectService.getSubjects(library.getId());
        var science = subjects.stream().filter(s -> s.getId().equals(scienceSubject.getId())).findFirst().orElseThrow();
        assertThat(science.getBookCount()).isEqualTo(2L);

        var history = subjects.stream().filter(s -> s.getId().equals(historySubject.getId())).findFirst().orElseThrow();
        assertThat(history.getBookCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Book with no subjects has empty subject lists in DTO")
    void bookWithNoSubject_hasEmptySubjectLists() {
        bookService.createBook(library.getId(), BookRequest.builder()
                .title("کتاب بدون موضوع").author("نویسنده")
                .autoDigitalBorrowEnabled(false).build());

        Page<BookDTO> results = bookService.advancedSearchBooks(
                library.getId(), "بدون موضوع", null, null, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        BookDTO book = results.getContent().get(0);
        assertThat(book.getSubjectIds()).isEmpty();
        assertThat(book.getSubjectNames()).isEmpty();
    }
}
