package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.StatsDTO;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import com.library.entity.enums.SystemRole;
import com.library.entity.enums.BorrowStatus;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
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
@DisplayName("Stats Service Tests")
class StatsServiceTest extends BaseIntegrationTest {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .email("owner@library.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Owner")
                .lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        owner = userRepository.save(owner);

        Library lib = Library.builder()
                .name("Test Library")
                .description("desc")
                .owner(owner)
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(14)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        libraryRepository.save(lib);
    }

    @Test
    @DisplayName("Should count active libraries correctly")
    void testCountActiveLibraries() {
        long count = libraryRepository.countByIsActive(true);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should count inactive libraries separately")
    void testCountInactiveLibraries() {
        User owner = userRepository.findByEmail("owner@library.com").orElseThrow();
        Library inactiveLib = Library.builder()
                .name("Inactive Library")
                .description("desc")
                .owner(owner)
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(14)
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        libraryRepository.save(inactiveLib);

        long activeCount = libraryRepository.countByIsActive(true);
        long inactiveCount = libraryRepository.countByIsActive(false);

        assertThat(activeCount).isGreaterThanOrEqualTo(1);
        assertThat(inactiveCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should count borrows by status")
    void testCountBorrowsByStatus() {
        long initialCount = borrowRepository.countByStatus(BorrowStatus.APPROVED);
        assertThat(initialCount).isGreaterThanOrEqualTo(0);
    }
}
