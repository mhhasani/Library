package com.library.service;

import com.library.util.TestFiles;
import com.library.BaseIntegrationTest;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Cover Image Service Tests")
class CoverImageServiceTest extends BaseIntegrationTest {

    @Autowired private CoverImageService coverImageService;
    @Autowired private BookRepository bookRepository;
    @Autowired private FileResourceRepository fileResourceRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;

    @MockBean private StorageService storageService;

    private User adminUser;
    private User memberUser;
    private Library library;
    private Book book;
    private MockMultipartFile imageFile;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("admin@cover.com").passwordHash("$2a$10$encoded")
                .firstName("Admin").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        memberUser = userRepository.save(User.builder()
                .email("member@cover.com").passwordHash("$2a$10$encoded")
                .firstName("Member").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        library = libraryRepository.save(Library.builder()
                .name("Cover Test Library").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(library)
                .role(LibraryMembershipRole.ADMIN).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        membershipRepository.save(LibraryMembership.builder()
                .user(memberUser).library(library)
                .role(LibraryMembershipRole.MEMBER).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        book = bookRepository.save(Book.builder()
                .library(library).title("Cover Book").author("Author")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        imageFile = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", TestFiles.jpeg("fake image content"));

        when(storageService.store(any(), anyString())).thenReturn("covers/test-uuid.jpg");
        doNothing().when(storageService).delete(anyString());
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("Admin can upload a cover image")
    void uploadCoverImage_adminSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        coverImageService.uploadCoverImage(library.getId(), book.getId(), imageFile);

        Book updated = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(updated.getCoverImage()).isNotNull();
        assertThat(updated.getCoverImage().getContentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Non-admin member cannot upload a cover image")
    void uploadCoverImage_nonAdmin_throwsUnauthorized() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        assertThatThrownBy(() -> coverImageService.uploadCoverImage(library.getId(), book.getId(), imageFile))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مدیر کتابخانه");
    }

    @Test
    @DisplayName("Non-member cannot upload a cover image")
    void uploadCoverImage_nonMember_throwsUnauthorized() {
        User outsider = userRepository.save(User.builder()
                .email("outsider@cover.com").passwordHash("$2a$10$encoded")
                .firstName("Out").lastName("Sider")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(outsider, "USER");

        assertThatThrownBy(() -> coverImageService.uploadCoverImage(library.getId(), book.getId(), imageFile))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Empty file — throws BadRequestException")
    void uploadCoverImage_emptyFile_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        MockMultipartFile emptyFile = new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> coverImageService.uploadCoverImage(library.getId(), book.getId(), emptyFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فایل خالی است");
    }

    @Test
    @DisplayName("Unsupported content type — throws BadRequestException")
    void uploadCoverImage_unsupportedType_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "cover.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> coverImageService.uploadCoverImage(library.getId(), book.getId(), pdfFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فرمت تصویر پشتیبانی نمی‌شود");
    }

    @Test
    @DisplayName("Spoofed content type (HTML declared as image/png) — throws BadRequestException")
    void uploadCoverImage_spoofedContent_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        MockMultipartFile disguised = new MockMultipartFile(
                "file", "cover.png", "image/png", "<html><script>alert(1)</script></html>".getBytes());

        assertThatThrownBy(() -> coverImageService.uploadCoverImage(library.getId(), book.getId(), disguised))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("تصویر معتبر");
    }

    @Test
    @DisplayName("Book belonging to another library — throws BadRequestException")
    void uploadCoverImage_bookNotInLibrary_throws() {
        Library otherLibrary = libraryRepository.save(Library.builder()
                .name("Other Library").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(otherLibrary)
                .role(LibraryMembershipRole.ADMIN).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> coverImageService.uploadCoverImage(otherLibrary.getId(), book.getId(), imageFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("این کتاب مربوط به این کتابخانه نیست");
    }

    @Test
    @DisplayName("Replacing a cover image deletes the old file when unreferenced")
    void uploadCoverImage_replace_deletesOldFile() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        coverImageService.uploadCoverImage(library.getId(), book.getId(), imageFile);

        MockMultipartFile secondImage = new MockMultipartFile(
                "file", "cover2.png", "image/png", TestFiles.png("different content"));
        when(storageService.store(any(), anyString())).thenReturn("covers/other-uuid.png");

        coverImageService.uploadCoverImage(library.getId(), book.getId(), secondImage);

        verify(storageService, times(1)).delete(anyString());
        Book updated = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(updated.getCoverImage().getContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Same image content reused across two books shares one FileResource")
    void uploadCoverImage_sameContentTwoBooks_deduplicates() {
        Book book2 = bookRepository.save(Book.builder()
                .library(library).title("Book 2").author("Author 2")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        coverImageService.uploadCoverImage(library.getId(), book.getId(), imageFile);
        coverImageService.uploadCoverImage(library.getId(), book2.getId(), imageFile);

        verify(storageService, times(1)).store(any(), anyString());
    }
}
