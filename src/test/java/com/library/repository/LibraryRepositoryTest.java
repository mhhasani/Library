package com.library.repository;

import com.library.BaseIntegrationTest;
import com.library.entity.Library;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LibraryRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setEmail("owner@library.com");
        owner.setPasswordHash("$2a$10$encoded");
        owner.setFirstName("Library");
        owner.setLastName("Owner");
        owner.setSystemRole(SystemRole.USER);
        owner.setAccountStatus(AccountStatus.ACTIVE);
        owner = userRepository.save(owner);
    }

    @Test
    @DisplayName("Should save and retrieve library")
    void testSaveLibrary() {
        // Given
        Library library = new Library();
        library.setName("Central Library");
        library.setDescription("Main city library");
        library.setOwner(owner);
        library.setAutoMembershipApproval(false);
        library.setDefaultBorrowDurationDays(14);

        // When
        Library saved = libraryRepository.save(library);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Central Library");
        assertThat(saved.getOwner()).isNotNull();
        assertThat(saved.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should find active libraries")
    void testFindByIsActive() {
        // Given
        Library active = new Library();
        active.setName("Active Library");
        active.setDescription("Active");
        active.setOwner(owner);
        active.setIsActive(true);
        libraryRepository.save(active);

        Library inactive = new Library();
        inactive.setName("Inactive Library");
        inactive.setDescription("Inactive");
        inactive.setOwner(owner);
        inactive.setIsActive(false);
        libraryRepository.save(inactive);

        // When
        List<Library> activeLibraries = libraryRepository.findByIsActive(true);

        // Then
        assertThat(activeLibraries).hasSize(1);
        assertThat(activeLibraries.get(0).getName()).isEqualTo("Active Library");
    }

    @Test
    @DisplayName("Should find library by ID")
    void testFindById() {
        // Given
        Library library = new Library();
        library.setName("Test Library");
        library.setDescription("Test");
        library.setOwner(owner);
        Library saved = libraryRepository.save(library);

        // When
        var found = libraryRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Library");
    }
}
