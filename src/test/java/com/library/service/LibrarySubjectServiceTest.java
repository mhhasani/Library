package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.LibrarySubjectDTO;
import com.library.entity.*;
import com.library.entity.enums.*;
import com.library.exception.BadRequestException;
import com.library.exception.UnauthorizedException;
import com.library.repository.*;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("LibrarySubject Service Tests")
class LibrarySubjectServiceTest extends BaseIntegrationTest {

    @Autowired private LibrarySubjectService subjectService;
    @Autowired private LibrarySubjectRepository subjectRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;

    private User adminUser;
    private User regularUser;
    private Library library;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("subj-admin@test.com").passwordHash("$2a$10$x")
                .firstName("Admin").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        regularUser = userRepository.save(User.builder()
                .email("subj-member@test.com").passwordHash("$2a$10$x")
                .firstName("Member").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        library = libraryRepository.save(Library.builder()
                .name("Subject Test Library").owner(adminUser)
                .autoMembershipApproval(false).defaultBorrowDurationDays(14)
                .isActive(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(library).role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        membershipRepository.save(LibraryMembership.builder()
                .user(regularUser).library(library).role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("Admin can create a subject")
    void createSubject_asAdmin_succeeds() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        LibrarySubjectDTO dto = subjectService.createSubject(library.getId(), "علوم کامپیوتر");

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getName()).isEqualTo("علوم کامپیوتر");
        assertThat(dto.getLibraryId()).isEqualTo(library.getId());
    }

    @Test
    @DisplayName("Creating duplicate subject throws BadRequestException")
    void createSubject_duplicate_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        subjectService.createSubject(library.getId(), "تاریخ");

        assertThatThrownBy(() -> subjectService.createSubject(library.getId(), "تاریخ"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("از قبل وجود دارد");
    }

    @Test
    @DisplayName("Regular member cannot create a subject")
    void createSubject_asMember_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");

        assertThatThrownBy(() -> subjectService.createSubject(library.getId(), "ریاضی"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Admin can list subjects alphabetically")
    void getSubjects_returnsSortedList() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        subjectService.createSubject(library.getId(), "ریاضی");
        subjectService.createSubject(library.getId(), "ادبیات");
        subjectService.createSubject(library.getId(), "فیزیک");

        List<LibrarySubjectDTO> subjects = subjectService.getSubjects(library.getId());

        assertThat(subjects).hasSize(3);
        // Alphabetical order
        assertThat(subjects.get(0).getName()).isEqualTo("ادبیات");
    }

    @Test
    @DisplayName("Admin can delete a subject")
    void deleteSubject_asAdmin_succeeds() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        LibrarySubjectDTO created = subjectService.createSubject(library.getId(), "حقوق");

        subjectService.deleteSubject(library.getId(), created.getId());

        assertThat(subjectRepository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("Creating subject with blank name throws BadRequestException")
    void createSubject_blankName_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        assertThatThrownBy(() -> subjectService.createSubject(library.getId(), "  "))
                .isInstanceOf(BadRequestException.class);
    }
}
