package com.library.repository;

import com.library.BaseIntegrationTest;
import com.library.entity.Book;
import com.library.entity.Library;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    private Library library;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("bookowner@library.com");
        owner.setPasswordHash("$2a$10$encoded");
        owner.setFirstName("Book");
        owner.setLastName("Owner");
        owner.setSystemRole(SystemRole.USER);
        owner.setAccountStatus(AccountStatus.ACTIVE);
        owner = userRepository.save(owner);

        library = new Library();
        library.setName("Tech Library");
        library.setDescription("Technology books");
        library.setOwner(owner);
        library = libraryRepository.save(library);
    }

    @Test
    @DisplayName("Should save and retrieve book")
    void testSaveBook() {
        // Given
        Book book = new Book();
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setPublisher("Prentice Hall");
        book.setPublicationYear(2008);
        book.setDescription("A handbook of agile software craftsmanship");
        book.setLibrary(library);

        // When
        Book saved = bookRepository.save(book);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Clean Code");
        assertThat(saved.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(saved.getLibrary()).isNotNull();
        assertThat(saved.getLibrary().getId()).isEqualTo(library.getId());
    }

    @Test
    @DisplayName("Should find books by title containing")
    void testFindByTitleContainingIgnoreCase() {
        // Given
        Book book1 = new Book();
        book1.setTitle("Clean Code");
        book1.setAuthor("Robert C. Martin");
        book1.setLibrary(library);
        bookRepository.save(book1);

        Book book2 = new Book();
        book2.setTitle("Clean Architecture");
        book2.setAuthor("Robert C. Martin");
        book2.setLibrary(library);
        bookRepository.save(book2);

        // When
        Page<Book> results = bookRepository.findByTitleContainingIgnoreCaseAndLibraryId("clean", library.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Clean Code", "Clean Architecture");
    }

    @Test
    @DisplayName("Should find books by author")
    void testFindByAuthorContainingIgnoreCase() {
        // Given
        Book book = new Book();
        book.setTitle("Effective Java");
        book.setAuthor("Joshua Bloch");
        book.setLibrary(library);
        bookRepository.save(book);

        // When
        Page<Book> results = bookRepository.findByAuthorContainingIgnoreCaseAndLibraryId("bloch", library.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getAuthor()).isEqualTo("Joshua Bloch");
    }

    @Test
    @DisplayName("Should find books by library ID")
    void testFindByLibraryId() {
        // Given
        for (int i = 0; i < 3; i++) {
            Book book = new Book();
            book.setTitle("Book " + i);
            book.setAuthor("Author " + i);
            book.setLibrary(library);
            bookRepository.save(book);
        }

        // When
        List<Book> books = bookRepository.findByLibraryId(library.getId());

        // Then
        assertThat(books).hasSize(3);
        assertThat(books).allMatch(book -> book.getLibrary().getId().equals(library.getId()));
    }
}
