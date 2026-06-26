package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.DigitalBookDTO;
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
import org.springframework.core.io.ByteArrayResource;
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
@DisplayName("Digital Book Service Tests")
class DigitalBookServiceTest extends BaseIntegrationTest {

    @Autowired private DigitalBookService digitalBookService;
    @Autowired private DigitalBookRepository digitalBookRepository;
    @Autowired private FileResourceRepository fileResourceRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BorrowRepository borrowRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;

    @MockBean private StorageService storageService;

    private User adminUser;
    private User memberUser;
    private Library library;
    private Book book;
    private MockMultipartFile pdfFile;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("admin@digital.com").passwordHash("$2a$10$encoded")
                .firstName("Admin").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        memberUser = userRepository.save(User.builder()
                .email("member@digital.com").passwordHash("$2a$10$encoded")
                .firstName("Member").lastName("User")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        library = libraryRepository.save(Library.builder()
                .name("Digital Test Library").owner(adminUser)
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
                .library(library).title("Digital Book").author("Author")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        pdfFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "fake pdf content".getBytes());

        when(storageService.store(any(), anyString()))
                .thenReturn("digital-books/test-uuid.pdf");
        when(storageService.load(anyString()))
                .thenReturn(new ByteArrayResource("fake pdf content".getBytes()));
        doNothing().when(storageService).delete(anyString());
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin can upload a digital book")
    void uploadDigitalBook_adminSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        DigitalBookDTO result = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, "v1");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getFileFormat()).isEqualTo("PDF");
        assertThat(result.getOriginalFilename()).isEqualTo("test.pdf");
        assertThat(result.getVersionName()).isEqualTo("v1");
    }

    @Test
    @DisplayName("Non-admin member cannot upload a digital book")
    void uploadDigitalBook_nonAdmin_throwsUnauthorized() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        assertThatThrownBy(() -> digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مدیر کتابخانه");
    }

    @Test
    @DisplayName("Should throw BadRequestException for non-PDF file")
    void uploadDigitalBook_nonPdf_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        MockMultipartFile docFile = new MockMultipartFile(
                "file", "test.doc", "application/msword", "content".getBytes());

        assertThatThrownBy(() -> digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), docFile, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط فایل PDF");
    }

    @Test
    @DisplayName("Uploading a second PDF replaces the existing one — no exception thrown")
    void uploadDigitalBook_duplicatePdf_replaces() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        digitalBookService.uploadDigitalBook(library.getId(), book.getId(), pdfFile, null);

        MockMultipartFile pdfFile2 = new MockMultipartFile(
                "file", "another.pdf", "application/pdf", "different pdf content".getBytes());
        when(storageService.store(any(), anyString())).thenReturn("digital-books/other-uuid.pdf");

        // Second upload should succeed (replaces previous)
        assertThatNoException().isThrownBy(() ->
                digitalBookService.uploadDigitalBook(library.getId(), book.getId(), pdfFile2, null));
        // Still only one digital version for this book
        assertThat(digitalBookRepository.findByBookId(book.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Same PDF uploaded to two different books shares one FileResource")
    void uploadDigitalBook_sameContentTwoBooks_deduplicates() {
        Book book2 = bookRepository.save(Book.builder()
                .library(library).title("Book 2").author("Author 2")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        // Both books get the same file content → same checksum
        digitalBookService.uploadDigitalBook(library.getId(), book.getId(), pdfFile, null);
        digitalBookService.uploadDigitalBook(library.getId(), book2.getId(), pdfFile, null);

        // store() only called once; second upload reuses the existing FileResource
        verify(storageService, times(1)).store(any(), anyString());
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Member can list digital books")
    void listDigitalBooks_memberSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        digitalBookService.uploadDigitalBook(library.getId(), book.getId(), pdfFile, null);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        List<DigitalBookDTO> list = digitalBookService.listDigitalBooks(library.getId(), book.getId());

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getFileFormat()).isEqualTo("PDF");
    }

    @Test
    @DisplayName("Non-member cannot list digital books")
    void listDigitalBooks_nonMember_throwsUnauthorized() {
        User outsider = userRepository.save(User.builder()
                .email("outsider@digital.com").passwordHash("$2a$10$encoded")
                .firstName("Out").lastName("Sider")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(outsider, "USER");

        assertThatThrownBy(() -> digitalBookService.listDigitalBooks(library.getId(), book.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Download ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin can download without a borrow")
    void downloadDigitalBook_adminCanDownload() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        DigitalBookDTO uploaded = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null);

        assertThatNoException().isThrownBy(() ->
                digitalBookService.downloadDigitalBook(uploaded.getId()));
    }

    @Test
    @DisplayName("User with approved digital borrow can download")
    void downloadDigitalBook_approvedDigitalBorrowAllowsDownload() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        DigitalBookDTO uploaded = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null);

        // Create an approved digital borrow for memberUser
        borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book)
                .borrowType(BorrowType.DIGITAL)
                .status(BorrowStatus.APPROVED)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatNoException().isThrownBy(() ->
                digitalBookService.downloadDigitalBook(uploaded.getId()));
    }

    @Test
    @DisplayName("User without approved borrow cannot download")
    void downloadDigitalBook_noApprovedBorrow_throwsUnauthorized() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        DigitalBookDTO uploaded = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> digitalBookService.downloadDigitalBook(uploaded.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("دانلود تأییدشده");
    }

    @Test
    @DisplayName("User with only REQUESTED (not approved) borrow cannot download")
    void downloadDigitalBook_requestedBorrowNotEnough_throwsUnauthorized() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        DigitalBookDTO uploaded = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null);

        borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book)
                .borrowType(BorrowType.DIGITAL)
                .status(BorrowStatus.REQUESTED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> digitalBookService.downloadDigitalBook(uploaded.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin can delete a digital book")
    void deleteDigitalBook_adminSuccess() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        DigitalBookDTO uploaded = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null);

        digitalBookService.deleteDigitalBook(library.getId(), book.getId(), uploaded.getId());

        assertThat(digitalBookRepository.findById(uploaded.getId())).isEmpty();
        verify(storageService).delete(anyString());
    }

    @Test
    @DisplayName("Non-admin cannot delete a digital book")
    void deleteDigitalBook_nonAdmin_throwsUnauthorized() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        DigitalBookDTO uploaded = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() ->
                digitalBookService.deleteDigitalBook(library.getId(), book.getId(), uploaded.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("FileResource is not deleted when another book's PDF still references it")
    void deleteDigitalBook_sharedFile_notDeleted() {
        Book book2 = bookRepository.save(Book.builder()
                .library(library).title("Book 2").author("Author 2")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        // Same file content → shared FileResource
        DigitalBookDTO pdf1 = digitalBookService.uploadDigitalBook(
                library.getId(), book.getId(), pdfFile, null);
        DigitalBookDTO pdf2 = digitalBookService.uploadDigitalBook(
                library.getId(), book2.getId(), pdfFile, null);

        // Delete book1's PDF — book2 still references the same FileResource
        digitalBookService.deleteDigitalBook(library.getId(), book.getId(), pdf1.getId());

        verify(storageService, never()).delete(anyString());
        assertThat(digitalBookRepository.findById(pdf2.getId())).isPresent();
    }
}
