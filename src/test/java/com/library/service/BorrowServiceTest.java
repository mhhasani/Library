package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowRequest;
import com.library.dto.BorrowerSummaryDTO;
import com.library.dto.DeliveryDetailsRequest;
import com.library.dto.ReturnRequest;
import com.library.dto.ReturnScheduleRequest;
import com.library.entity.*;
import com.library.entity.enums.*;
import com.library.dto.PhysicalApprovalRequest;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@DisplayName("Borrow Service Tests")
class BorrowServiceTest extends BaseIntegrationTest {

    @Autowired private BorrowService borrowService;
    @Autowired private BorrowRepository borrowRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private DigitalBookRepository digitalBookRepository;
    @Autowired private FileResourceRepository fileResourceRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BorrowEventRepository borrowEventRepository;

    @MockBean private NotificationService notificationService;

    private User adminUser;
    private User memberUser;
    private Library library;
    private Book book;
    private BookCopy bookCopy;

    @BeforeEach
    void setUp() {
        when(notificationService.notify(any(), any(), anyString(), anyString(), anyString(), any(), anyString())).thenReturn(null);

        adminUser = userRepository.save(User.builder()
                .email("admin@borrow.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Admin").lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        memberUser = userRepository.save(User.builder()
                .email("member@borrow.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Member").lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        library = libraryRepository.save(Library.builder()
                .name("Borrow Test Library")
                .owner(adminUser)
                .defaultBorrowDurationDays(14)
                .isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(library)
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        membershipRepository.save(LibraryMembership.builder()
                .user(memberUser).library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        book = bookRepository.save(Book.builder()
                .library(library)
                .title("Test Book")
                .author("Test Author")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        bookCopy = bookCopyRepository.save(BookCopy.builder()
                .book(book).library(library)
                .copyNumber(1)
                .status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    // ---- helpers ----

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .email(email).passwordHash("$2a$10$encoded")
                .firstName("F").lastName("L")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private void addMembership(User user, LibraryMembershipRole role, MembershipStatus status) {
        membershipRepository.save(LibraryMembership.builder()
                .user(user).library(library).role(role).status(status)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private Borrow saveRawBorrow(User user, BorrowType type, BorrowStatus status, BookCopy copy) {
        return borrowRepository.save(Borrow.builder()
                .user(user).library(library).book(book)
                .borrowType(type).status(status).bookCopy(copy)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private DigitalBook createDigitalVersion(String checksum) {
        FileResource fr = fileResourceRepository.save(FileResource.builder()
                .originalFilename("book.pdf").storedFilename("uuid-" + checksum + ".pdf")
                .filePath("digital-books/uuid-" + checksum + ".pdf")
                .fileSizeBytes(1024L).contentType("application/pdf")
                .checksumSha256(checksum).uploadedBy(adminUser)
                .createdAt(LocalDateTime.now()).build());
        return digitalBookRepository.save(DigitalBook.builder()
                .book(book).fileResource(fr).fileFormat("PDF")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    // ── createBorrowRequest / createPhysicalBorrow ──────────────────────────

    @Test
    @DisplayName("Should create physical borrow request successfully")
    void createPhysicalBorrow_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build();

        BorrowDTO result = borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThat(result.getBorrowType()).isEqualTo(BorrowType.PHYSICAL);
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.REQUESTED);
        assertThat(result.getBookId()).isEqualTo(book.getId());
        assertThat(result.getBookCopyId()).isNotNull();
        verify(notificationService, atLeastOnce()).notify(eq(adminUser), eq(NotificationType.NEW_PHYSICAL_REQUEST), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException when no copy available")
    void createPhysicalBorrow_noCopyAvailable_throws() {
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("نسخه‌ی موجودی");
    }

    @Test
    @DisplayName("Should throw BadRequestException when user already has active physical borrow")
    void createPhysicalBorrow_duplicate_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build();

        borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("امانت فعال");
    }

    @Test
    @DisplayName("createBorrowRequest throws for unknown library")
    void createBorrowRequest_libraryNotFound_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(999999L, book.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createBorrowRequest throws for unknown book")
    void createBorrowRequest_bookNotFound_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), 999999L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createBorrowRequest throws when book belongs to a different library")
    void createBorrowRequest_bookLibraryMismatch_throws() {
        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(other.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("مربوط به این کتابخانه نیست");
    }

    @Test
    @DisplayName("createBorrowRequest throws when membership pending (not approved)")
    void createBorrowRequest_membershipNotApproved_throws() {
        User pendingUser = createUser("pending@borrow.com");
        addMembership(pendingUser, LibraryMembershipRole.MEMBER, MembershipStatus.PENDING);
        SecurityTestUtils.setSecurityContext(pendingUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("تأیید نشده");
    }

    @Test
    @DisplayName("createBorrowRequest throws BadRequestException for invalid/null borrow type")
    void createBorrowRequest_invalidType_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(null).build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("نوع امانت نامعتبر است");
    }

    @Test
    @DisplayName("createPhysicalBorrow throws when delivery address blank")
    void createPhysicalBorrow_blankAddress_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("  ").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("آدرس تحویل الزامی است");
    }

    @Test
    @DisplayName("createPhysicalBorrow throws on invalid extension format")
    void createPhysicalBorrow_invalidExtension_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL)
                .deliveryAddress("addr").deliveryExtension("12").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("۸ رقم");
    }

    @Test
    @DisplayName("createPhysicalBorrow with explicit bookCopyId succeeds and saves profile defaults")
    void createPhysicalBorrow_explicitCopy_saveToProfile() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL)
                .deliveryAddress("addr").deliveryExtension("12345678")
                .bookCopyId(bookCopy.getId()).saveToProfile(true).requestedDurationDays(5).build();

        BorrowDTO result = borrowService.createBorrowRequest(library.getId(), book.getId(), req);
        assertThat(result.getBookCopyId()).isEqualTo(bookCopy.getId());

        User reloaded = userRepository.findById(memberUser.getId()).orElseThrow();
        assertThat(reloaded.getDeliveryAddress()).isEqualTo("addr");
        assertThat(reloaded.getInternalExtension()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("createPhysicalBorrow throws when explicit copy belongs to a different book")
    void createPhysicalBorrow_explicitCopyWrongBook_throws() {
        Book book2 = bookRepository.save(Book.builder().library(library).title("Book2").author("A")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        BookCopy otherCopy = bookCopyRepository.save(BookCopy.builder().book(book2).library(library)
                .copyNumber(1).status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL)
                .deliveryAddress("addr").bookCopyId(otherCopy.getId()).build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("این نسخه مربوط به این کتاب نیست");
    }

    @Test
    @DisplayName("createPhysicalBorrow throws when explicit copy is not available")
    void createPhysicalBorrow_explicitCopyNotAvailable_throws() {
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL)
                .deliveryAddress("addr").bookCopyId(bookCopy.getId()).build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در دسترس نیست");
    }

    @Test
    @DisplayName("createPhysicalBorrow throws for unknown explicit copy id")
    void createPhysicalBorrow_explicitCopyNotFound_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL)
                .deliveryAddress("addr").bookCopyId(999999L).build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Digital Borrow ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create digital borrow and immediately approve it")
    void createDigitalBorrow_immediatelyApproved() {
        createDigitalVersion("abc123");

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build();

        BorrowDTO result = borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThat(result.getBorrowType()).isEqualTo(BorrowType.DIGITAL);
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.APPROVED);
        assertThat(result.getBorrowDate()).isNotNull();
        assertThat(result.getDueDate()).isNotNull();
    }

    @Test
    @DisplayName("Should throw BadRequestException when book has no digital versions")
    void createDigitalBorrow_noDigitalVersions_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build();

        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("نسخه‌ی دیجیتال ندارد");
    }

    @Test
    @DisplayName("Should throw BadRequestException when user already has active digital borrow")
    void createDigitalBorrow_duplicate_throws() {
        createDigitalVersion("abc789");

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build();

        borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("دانلود فعال");
    }

    @Test
    @DisplayName("Physical and digital borrows can coexist for the same book")
    void physicalAndDigitalBorrow_canCoexist() {
        createDigitalVersion("coexist123");

        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowDTO physical = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build());
        BorrowDTO digital = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build());

        assertThat(physical.getBorrowType()).isEqualTo(BorrowType.PHYSICAL);
        assertThat(digital.getBorrowType()).isEqualTo(BorrowType.DIGITAL);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-member requests borrow")
    void createBorrow_nonMember_throwsUnauthorized() {
        User outsider = createUser("outsider@borrow.com");
        SecurityTestUtils.setSecurityContext(outsider, "USER");

        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── updatePhysicalRequest ────────────────────────────────────────────────

    @Test
    @DisplayName("updatePhysicalRequest updates delivery details and saves profile")
    void updatePhysicalRequest_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr1").build());

        BorrowRequest update = BorrowRequest.builder().deliveryAddress("addr2")
                .deliveryExtension("87654321").requestedDurationDays(10).saveToProfile(true).build();
        BorrowDTO result = borrowService.updatePhysicalRequest(library.getId(), created.getId(), update);

        assertThat(result.getDeliveryAddress()).isEqualTo("addr2");
        assertThat(result.getRequestedDurationDays()).isEqualTo(10);
        User reloaded = userRepository.findById(memberUser.getId()).orElseThrow();
        assertThat(reloaded.getDeliveryAddress()).isEqualTo("addr2");
    }

    @Test
    @DisplayName("updatePhysicalRequest throws for wrong library")
    void updatePhysicalRequest_wrongLibrary_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr1").build());
        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BorrowRequest update = BorrowRequest.builder().deliveryAddress("addr2").build();
        assertThatThrownBy(() -> borrowService.updatePhysicalRequest(other.getId(), created.getId(), update))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updatePhysicalRequest throws when editor is not the owner")
    void updatePhysicalRequest_notOwner_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr1").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowRequest update = BorrowRequest.builder().deliveryAddress("addr2").build();
        assertThatThrownBy(() -> borrowService.updatePhysicalRequest(library.getId(), created.getId(), update))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("updatePhysicalRequest throws for digital borrows")
    void updatePhysicalRequest_notPhysical_throws() {
        createDigitalVersion("digupd");
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build());

        BorrowRequest update = BorrowRequest.builder().deliveryAddress("addr2").build();
        assertThatThrownBy(() -> borrowService.updatePhysicalRequest(library.getId(), created.getId(), update))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط درخواست‌های فیزیکی");
    }

    @Test
    @DisplayName("updatePhysicalRequest throws when status is not REQUESTED")
    void updatePhysicalRequest_notRequestedStatus_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr1").build());
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(), PhysicalApprovalRequest.builder()
                .plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(5).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest update = BorrowRequest.builder().deliveryAddress("addr2").build();
        assertThatThrownBy(() -> borrowService.updatePhysicalRequest(library.getId(), created.getId(), update))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("تأییدنشده");
    }

    @Test
    @DisplayName("updatePhysicalRequest throws when address blank / extension invalid")
    void updatePhysicalRequest_invalidFields_throw() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr1").build());

        assertThatThrownBy(() -> borrowService.updatePhysicalRequest(library.getId(), created.getId(),
                BorrowRequest.builder().deliveryAddress(" ").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("آدرس تحویل الزامی است");

        assertThatThrownBy(() -> borrowService.updatePhysicalRequest(library.getId(), created.getId(),
                BorrowRequest.builder().deliveryAddress("ok").deliveryExtension("1").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("۸ رقم");
    }

    // ── approveBorrowRequest (digital-only path) ────────────────────────────

    @Test
    @DisplayName("approveBorrowRequest approves a manually-requested digital borrow")
    void approveBorrowRequest_digital_success() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.REQUESTED, null);
        SecurityTestUtils.setSecurityContext(adminUser, "USER");

        BorrowDTO result = borrowService.approveBorrowRequest(library.getId(), raw.getId());
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.APPROVED);
        assertThat(result.getBorrowDate()).isNotNull();
        assertThat(result.getDueDate()).isNotNull();
    }

    @Test
    @DisplayName("approveBorrowRequest throws for physical borrows (must use approvePhysicalBorrow)")
    void approveBorrowRequest_physical_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.approveBorrowRequest(library.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("جزئیات تحویل");
    }

    @Test
    @DisplayName("approveBorrowRequest throws for unknown borrow / wrong library / non-admin / non-member / wrong status")
    void approveBorrowRequest_variousFailures() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.REQUESTED, null);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.approveBorrowRequest(library.getId(), 999999L))
                .isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.approveBorrowRequest(other.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class);

        User outsider = createUser("out2@borrow.com");
        SecurityTestUtils.setSecurityContext(outsider, "USER");
        assertThatThrownBy(() -> borrowService.approveBorrowRequest(library.getId(), raw.getId()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.approveBorrowRequest(library.getId(), raw.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مدیر کتابخانه");

        Borrow approved = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.APPROVED, null);
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.approveBorrowRequest(library.getId(), approved.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در انتظار");
    }

    // ── reserveBook ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserveBook creates a reservation when no copies are available")
    void reserveBook_success() {
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowDTO result = borrowService.reserveBook(library.getId(), book.getId());
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.REQUESTED);
        assertThat(result.getBookCopyId()).isNull();
        assertThat(result.getIsReservation()).isTrue();
    }

    @Test
    @DisplayName("reserveBook throws when copies are available")
    void reserveBook_copyAvailable_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.reserveBook(library.getId(), book.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("کتاب موجود است");
    }

    @Test
    @DisplayName("reserveBook throws for various validation failures")
    void reserveBook_variousFailures() {
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.reserveBook(999999L, book.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> borrowService.reserveBook(library.getId(), 999999L))
                .isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.reserveBook(other.getId(), book.getId()))
                .isInstanceOf(BadRequestException.class);

        User outsider = createUser("out3@borrow.com");
        SecurityTestUtils.setSecurityContext(outsider, "USER");
        assertThatThrownBy(() -> borrowService.reserveBook(library.getId(), book.getId()))
                .isInstanceOf(UnauthorizedException.class);

        User pendingUser = createUser("pending2@borrow.com");
        addMembership(pendingUser, LibraryMembershipRole.MEMBER, MembershipStatus.PENDING);
        SecurityTestUtils.setSecurityContext(pendingUser, "USER");
        assertThatThrownBy(() -> borrowService.reserveBook(library.getId(), book.getId()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.reserveBook(library.getId(), book.getId());
        assertThatThrownBy(() -> borrowService.reserveBook(library.getId(), book.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("درخواست فعال دارید");
    }

    // ── rejectBorrowRequest ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject borrow request with reason and notify user (physical)")
    void rejectBorrow_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO rejected = borrowService.rejectBorrowRequest(library.getId(), created.getId(), "Out of policy");

        assertThat(rejected.getStatus()).isEqualTo(BorrowStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Out of policy");
        verify(notificationService).notify(eq(memberUser), eq(NotificationType.PHYSICAL_REJECTED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("rejectBorrowRequest on a digital REQUESTED borrow does not notify (non-physical branch)")
    void rejectBorrow_digital_noNotify() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.REQUESTED, null);
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO rejected = borrowService.rejectBorrowRequest(library.getId(), raw.getId(), "reason");
        assertThat(rejected.getStatus()).isEqualTo(BorrowStatus.REJECTED);
        verify(notificationService, never()).notify(any(), eq(NotificationType.PHYSICAL_REJECTED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("rejectBorrowRequest throws for various validation failures")
    void rejectBorrow_variousFailures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        assertThatThrownBy(() -> borrowService.rejectBorrowRequest(library.getId(), 999999L, "r"))
                .isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.rejectBorrowRequest(other.getId(), created.getId(), "r"))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.rejectBorrowRequest(library.getId(), created.getId(), "r"))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.rejectBorrowRequest(library.getId(), created.getId(), " "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ذکر دلیل رد الزامی است");

        borrowService.rejectBorrowRequest(library.getId(), created.getId(), "final");
        assertThatThrownBy(() -> borrowService.rejectBorrowRequest(library.getId(), created.getId(), "again"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در انتظار");
    }

    // ── approvePhysicalBorrow ────────────────────────────────────────────────

    @Test
    @DisplayName("Should approve physical borrow and mark book copy as BORROWED, with courier notification")
    void approveBorrow_physical_marksCopyBorrowed_notifiesCourier() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO approved = borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(7)
                        .courierName("Ali").build());

        assertThat(approved.getStatus()).isEqualTo(BorrowStatus.APPROVED);
        assertThat(approved.getBookCopyId()).isNotNull();

        BookCopy copy = bookCopyRepository.findById(approved.getBookCopyId()).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.BORROWED);
        verify(notificationService).notify(eq(memberUser), eq(NotificationType.BOOK_DISPATCHED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("approvePhysicalBorrow without courier does not send dispatch notification")
    void approveBorrow_physical_noCourier_noDispatchNotify() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(7).build());

        verify(notificationService, never()).notify(any(), eq(NotificationType.BOOK_DISPATCHED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("approvePhysicalBorrow with explicit bookCopyId assigns that copy")
    void approvePhysicalBorrow_explicitCopy() {
        BookCopy copy2 = bookCopyRepository.save(BookCopy.builder().book(book).library(library)
                .copyNumber(2).status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").bookCopyId(bookCopy.getId()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO approved = borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now())
                        .approvedDurationDays(3).bookCopyId(copy2.getId()).build());

        assertThat(approved.getBookCopyId()).isEqualTo(copy2.getId());
    }

    @Test
    @DisplayName("approvePhysicalBorrow throws when explicit copy belongs to different book / not available / not found")
    void approvePhysicalBorrow_explicitCopyFailures() {
        Book book2 = bookRepository.save(Book.builder().library(library).title("B2").author("A")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        BookCopy otherBookCopy = bookCopyRepository.save(BookCopy.builder().book(book2).library(library)
                .copyNumber(1).status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3)
                        .bookCopyId(otherBookCopy.getId()).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("مربوط به این کتاب نیست");

        otherBookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(otherBookCopy);
        BookCopy sameBookUnavailable = bookCopyRepository.save(BookCopy.builder().book(book).library(library)
                .copyNumber(9).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3)
                        .bookCopyId(sameBookUnavailable.getId()).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در دسترس نیست");

        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3)
                        .bookCopyId(999999L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("approvePhysicalBorrow throws when no copy available for a reservation")
    void approvePhysicalBorrow_reservationNoCopy_throws() {
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO reservation = borrowService.reserveBook(library.getId(), book.getId());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), reservation.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("هنوز هیچ نسخه‌ای");
    }

    @Test
    @DisplayName("approvePhysicalBorrow throws for wrong library / non-admin / not physical / not requested status")
    void approvePhysicalBorrow_variousFailures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), 999999L,
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build()))
                .isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(other.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build()))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build()))
                .isInstanceOf(UnauthorizedException.class);

        Borrow rawDigital = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.APPROVED, null);
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), rawDigital.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط برای امانت است");

        borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build());
        assertThatThrownBy(() -> borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در انتظار");
    }

    @Test
    @DisplayName("Should auto-reject other pending borrows and notify them when last copy is taken (approveBorrowRequest path via approvePhysicalBorrow)")
    void approvePhysicalBorrow_autoRejectsOthers_whenNoCopiesLeft() {
        User member2 = createUser("member2@borrow.com");
        addMembership(member2, LibraryMembershipRole.MEMBER, MembershipStatus.APPROVED);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO req1 = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        BookCopy copy2 = bookCopyRepository.save(BookCopy.builder()
                .book(book).library(library).copyNumber(2)
                .status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(member2, "USER");
        BorrowDTO req2 = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        copy2.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(copy2);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), req1.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build());

        Borrow pending2 = borrowRepository.findById(req2.getId()).orElseThrow();
        assertThat(pending2.getStatus()).isEqualTo(BorrowStatus.REJECTED);
        verify(notificationService).notify(eq(member2), eq(NotificationType.PHYSICAL_REJECTED), anyString(), anyString(), anyString(), any(), anyString());
    }

    // ── updateDeliveryDetails ────────────────────────────────────────────────

    @Test
    @DisplayName("updateDeliveryDetails notifies only the first time a courier is set")
    void updateDeliveryDetails_courierNotifiesOnlyOnce() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build());

        borrowService.updateDeliveryDetails(library.getId(), created.getId(),
                DeliveryDetailsRequest.builder().courierName("Reza").copyUniqueCode("C-1").build());
        verify(notificationService, times(1)).notify(eq(memberUser), eq(NotificationType.BOOK_DISPATCHED), anyString(), anyString(), anyString(), any(), anyString());

        borrowService.updateDeliveryDetails(library.getId(), created.getId(),
                DeliveryDetailsRequest.builder().courierName("Reza2").build());
        verify(notificationService, times(1)).notify(eq(memberUser), eq(NotificationType.BOOK_DISPATCHED), anyString(), anyString(), anyString(), any(), anyString());

        BorrowDTO reloaded = borrowService.updateDeliveryDetails(library.getId(), created.getId(),
                DeliveryDetailsRequest.builder().plannedDeliveryDate(LocalDateTime.now().plusDays(1)).build());
        assertThat(reloaded.getCourierName()).isEqualTo("Reza2");
    }

    @Test
    @DisplayName("updateDeliveryDetails throws for wrong library / non-admin / not physical / not approved status")
    void updateDeliveryDetails_variousFailures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.updateDeliveryDetails(other.getId(), created.getId(),
                DeliveryDetailsRequest.builder().courierName("X").build()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.updateDeliveryDetails(library.getId(), created.getId(),
                DeliveryDetailsRequest.builder().courierName("X").build()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.updateDeliveryDetails(library.getId(), created.getId(),
                DeliveryDetailsRequest.builder().courierName("X").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("تأییدشده");

        Borrow rawDigital = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.APPROVED, null);
        assertThatThrownBy(() -> borrowService.updateDeliveryDetails(library.getId(), rawDigital.getId(),
                DeliveryDetailsRequest.builder().courierName("X").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط برای امانت است");
    }

    // ── confirmReceipt ───────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmReceipt starts the loan clock using approvedDurationDays")
    void confirmReceipt_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(9).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO received = borrowService.confirmReceipt(library.getId(), created.getId());

        assertThat(received.getStatus()).isEqualTo(BorrowStatus.RECEIVED);
        assertThat(received.getReceivedAt()).isNotNull();
        assertThat(received.getDueDate()).isAfter(LocalDateTime.now().plusDays(8));
        verify(notificationService).notify(eq(adminUser), eq(NotificationType.RECEIPT_CONFIRMED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("confirmReceipt uses library default duration when approvedDurationDays is null, and skips notify when approvedBy is null")
    void confirmReceipt_fallbackDuration_noApprovedBy_noNotify() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.APPROVED, bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowDTO received = borrowService.confirmReceipt(library.getId(), raw.getId());
        assertThat(received.getDueDate()).isCloseTo(LocalDateTime.now().plusDays(14), within(1, java.time.temporal.ChronoUnit.MINUTES));
        verify(notificationService, never()).notify(any(), eq(NotificationType.RECEIPT_CONFIRMED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("confirmReceipt throws for wrong library / not owner / not physical / not approved status")
    void confirmReceipt_variousFailures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        assertThatThrownBy(() -> borrowService.confirmReceipt(library.getId(), 999999L))
                .isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.confirmReceipt(other.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.confirmReceipt(library.getId(), created.getId()))
                .isInstanceOf(UnauthorizedException.class);

        // status != APPROVED (still REQUESTED)
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.confirmReceipt(library.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("تأییدشده را می‌توان");

        Borrow rawDigital = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.APPROVED, null);
        assertThatThrownBy(() -> borrowService.confirmReceipt(library.getId(), rawDigital.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط برای امانت است");
    }

    // ── confirmReturnByLibrarian / notifyNextReservation ────────────────────

    @Test
    @DisplayName("Librarian confirms physical return, copy becomes AVAILABLE, waiting reservation gets notified")
    void confirmReturn_physical_marksCopyAvailable_notifiesReservation() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("تهران، خیابان آزادی ۱۲").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(), PhysicalApprovalRequest.builder()
                .plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(5).build());

        // another user reserves the book now that no copies are available
        User waitingUser = createUser("waiting@borrow.com");
        addMembership(waitingUser, LibraryMembershipRole.MEMBER, MembershipStatus.APPROVED);
        SecurityTestUtils.setSecurityContext(waitingUser, "USER");
        BorrowDTO reservation = borrowService.reserveBook(library.getId(), book.getId());
        assertThat(reservation.getIsReservation()).isTrue();

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO returned = borrowService.confirmReturnByLibrarian(library.getId(), created.getId());

        assertThat(returned.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(returned.getReturnDate()).isNotNull();

        BookCopy copy = bookCopyRepository.findById(created.getBookCopyId()).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);

        verify(notificationService).notify(eq(memberUser), eq(NotificationType.RETURN_CONFIRMED), anyString(), anyString(), anyString(), any(), anyString());
        verify(notificationService).notify(eq(waitingUser), eq(NotificationType.PHYSICAL_APPROVED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("confirmReturnByLibrarian handles a borrow with no bookCopy assigned (defensive branch)")
    void confirmReturn_noBookCopy_noError() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, null);
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO returned = borrowService.confirmReturnByLibrarian(library.getId(), raw.getId());
        assertThat(returned.getStatus()).isEqualTo(BorrowStatus.RETURNED);
    }

    @Test
    @DisplayName("confirmReturnByLibrarian throws for wrong library / non-admin / not physical / wrong status")
    void confirmReturn_variousFailures() {
        assertThatThrownBy(() -> {
            SecurityTestUtils.setSecurityContext(adminUser, "USER");
            borrowService.confirmReturnByLibrarian(library.getId(), 999999L);
        }).isInstanceOf(ResourceNotFoundException.class);

        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.confirmReturnByLibrarian(other.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.confirmReturnByLibrarian(library.getId(), raw.getId()))
                .isInstanceOf(UnauthorizedException.class);

        Borrow rawDigital = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.APPROVED, null);
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.confirmReturnByLibrarian(library.getId(), rawDigital.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط برای امانت است");

        Borrow rawRequested = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.REQUESTED, null);
        assertThatThrownBy(() -> borrowService.confirmReturnByLibrarian(library.getId(), rawRequested.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط امانت فعال را می‌توان");
    }

    // ── returnBook ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("returnBook returns a digital borrow")
    void returnBook_digital_success() {
        createDigitalVersion("returnDigital");
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build());

        BorrowDTO returned = borrowService.returnBook(library.getId(), created.getId());
        assertThat(returned.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(returned.getReturnDate()).isNotNull();
    }

    @Test
    @DisplayName("returnBook throws for wrong library / not owner / physical type / wrong status")
    void returnBook_variousFailures() {
        createDigitalVersion("returnDigital2");
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build());

        assertThatThrownBy(() -> borrowService.returnBook(library.getId(), 999999L))
                .isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.returnBook(other.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.returnBook(library.getId(), created.getId()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO physical = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());
        assertThatThrownBy(() -> borrowService.returnBook(library.getId(), physical.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("توسط کتابدار ثبت می‌شود");

        borrowService.returnBook(library.getId(), created.getId());
        assertThatThrownBy(() -> borrowService.returnBook(library.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فعال را می‌توان بازگرداند");
    }

    // ── getUserBorrows / getPendingBorrows / getLibraryBorrows ──────────────

    @Test
    @DisplayName("getUserBorrows filters by library, status and type")
    void getUserBorrows_filters() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        List<BorrowDTO> all = borrowService.getUserBorrows(library.getId(), null, null);
        assertThat(all).hasSize(1);

        List<BorrowDTO> byStatus = borrowService.getUserBorrows(library.getId(), BorrowStatus.REQUESTED, null);
        assertThat(byStatus).hasSize(1);
        List<BorrowDTO> byWrongStatus = borrowService.getUserBorrows(library.getId(), BorrowStatus.RETURNED, null);
        assertThat(byWrongStatus).isEmpty();

        List<BorrowDTO> byType = borrowService.getUserBorrows(library.getId(), null, BorrowType.PHYSICAL);
        assertThat(byType).hasSize(1);
        List<BorrowDTO> byWrongType = borrowService.getUserBorrows(library.getId(), null, BorrowType.DIGITAL);
        assertThat(byWrongType).isEmpty();
    }

    @Test
    @DisplayName("getUserBorrows throws for unknown library / non-member / pending membership")
    void getUserBorrows_failures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.getUserBorrows(999999L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);

        User outsider = createUser("out4@borrow.com");
        SecurityTestUtils.setSecurityContext(outsider, "USER");
        assertThatThrownBy(() -> borrowService.getUserBorrows(library.getId(), null, null))
                .isInstanceOf(UnauthorizedException.class);

        User pendingUser = createUser("pending3@borrow.com");
        addMembership(pendingUser, LibraryMembershipRole.MEMBER, MembershipStatus.PENDING);
        SecurityTestUtils.setSecurityContext(pendingUser, "USER");
        assertThatThrownBy(() -> borrowService.getUserBorrows(library.getId(), null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getPendingBorrows returns REQUESTED borrows for admins, filtered by type")
    void getPendingBorrows_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        List<BorrowDTO> pending = borrowService.getPendingBorrows(library.getId(), null);
        assertThat(pending).hasSize(1);

        List<BorrowDTO> pendingWrongType = borrowService.getPendingBorrows(library.getId(), BorrowType.DIGITAL);
        assertThat(pendingWrongType).isEmpty();
    }

    @Test
    @DisplayName("getPendingBorrows throws for unknown library / non-member / non-admin")
    void getPendingBorrows_failures() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.getPendingBorrows(999999L, null))
                .isInstanceOf(ResourceNotFoundException.class);

        User outsider = createUser("out5@borrow.com");
        SecurityTestUtils.setSecurityContext(outsider, "USER");
        assertThatThrownBy(() -> borrowService.getPendingBorrows(library.getId(), null))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.getPendingBorrows(library.getId(), null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getLibraryBorrows filters by status and type")
    void getLibraryBorrows_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        List<BorrowDTO> all = borrowService.getLibraryBorrows(library.getId(), null, null);
        assertThat(all).hasSize(1);

        List<BorrowDTO> byStatus = borrowService.getLibraryBorrows(library.getId(), BorrowStatus.REQUESTED, null);
        assertThat(byStatus).hasSize(1);

        List<BorrowDTO> byType = borrowService.getLibraryBorrows(library.getId(), null, BorrowType.DIGITAL);
        assertThat(byType).isEmpty();
    }

    @Test
    @DisplayName("getLibraryBorrows throws for unknown library / non-member / non-admin")
    void getLibraryBorrows_failures() {
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.getLibraryBorrows(999999L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);

        User outsider = createUser("out6@borrow.com");
        SecurityTestUtils.setSecurityContext(outsider, "USER");
        assertThatThrownBy(() -> borrowService.getLibraryBorrows(library.getId(), null, null))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.getLibraryBorrows(library.getId(), null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── trackingCodeFor ──────────────────────────────────────────────────────

    @Test
    @DisplayName("trackingCodeFor formats id and returns null for null id")
    void trackingCodeFor_formatsCorrectly() {
        assertThat(BorrowService.trackingCodeFor(123L)).isEqualTo("BR-000123");
        assertThat(BorrowService.trackingCodeFor(null)).isNull();
    }

    // ── getMyBorrowsPaged ────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyBorrowsPaged filters by type and searches by book title / library name / tracking code")
    void getMyBorrowsPaged_search() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        Pageable pageable = PageRequest.of(0, 10);
        Page<BorrowDTO> byType = borrowService.getMyBorrowsPaged(BorrowType.PHYSICAL, null, pageable);
        assertThat(byType.getTotalElements()).isEqualTo(1);
        Page<BorrowDTO> byWrongType = borrowService.getMyBorrowsPaged(BorrowType.DIGITAL, null, pageable);
        assertThat(byWrongType.getTotalElements()).isEqualTo(0);

        Page<BorrowDTO> byTitle = borrowService.getMyBorrowsPaged(null, "Test Book", pageable);
        assertThat(byTitle.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> byLibraryName = borrowService.getMyBorrowsPaged(null, "Borrow Test Library", pageable);
        assertThat(byLibraryName.getTotalElements()).isEqualTo(1);

        String trackingCode = BorrowService.trackingCodeFor(created.getId());
        Page<BorrowDTO> byTrackingCode = borrowService.getMyBorrowsPaged(null, trackingCode, pageable);
        assertThat(byTrackingCode.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> byBareId = borrowService.getMyBorrowsPaged(null, String.valueOf(created.getId()), pageable);
        assertThat(byBareId.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> noMatch = borrowService.getMyBorrowsPaged(null, "nonexistent-xyz", pageable);
        assertThat(noMatch.getTotalElements()).isEqualTo(0);
    }

    // ── getLibraryBorrowsPaged ───────────────────────────────────────────────

    @Test
    @DisplayName("getLibraryBorrowsPaged requires admin")
    void getLibraryBorrowsPaged_requiresAdmin() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> borrowService.getLibraryBorrowsPaged(library.getId(), null, null, null, false, false, pageable))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getLibraryBorrowsPaged filters by statuses/type and searches by user email/name/phone/book title/copy code/tracking code")
    void getLibraryBorrowsPaged_searchAndFilters() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        Pageable pageable = PageRequest.of(0, 10);

        Page<BorrowDTO> byStatuses = borrowService.getLibraryBorrowsPaged(
                library.getId(), List.of(BorrowStatus.REQUESTED), null, null, false, false, pageable);
        assertThat(byStatuses.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> byWrongStatuses = borrowService.getLibraryBorrowsPaged(
                library.getId(), List.of(BorrowStatus.RETURNED), null, null, false, false, pageable);
        assertThat(byWrongStatuses.getTotalElements()).isEqualTo(0);

        Page<BorrowDTO> byType = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, BorrowType.DIGITAL, null, false, false, pageable);
        assertThat(byType.getTotalElements()).isEqualTo(0);

        Page<BorrowDTO> byEmail = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, "member@borrow.com", false, false, pageable);
        assertThat(byEmail.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> byFirstName = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, "Member", false, false, pageable);
        assertThat(byFirstName.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> byBookTitle = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, "Test Book", false, false, pageable);
        assertThat(byBookTitle.getTotalElements()).isEqualTo(1);

        String trackingCode = BorrowService.trackingCodeFor(created.getId());
        Page<BorrowDTO> byTrackingCode = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, trackingCode, false, false, pageable);
        assertThat(byTrackingCode.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> byBareId = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, String.valueOf(created.getId()), false, false, pageable);
        assertThat(byBareId.getTotalElements()).isEqualTo(1);

        Page<BorrowDTO> noMatch = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, "totally-unrelated-search", false, false, pageable);
        assertThat(noMatch.getTotalElements()).isEqualTo(0);

        // approve + set copy unique code to search on it
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now())
                        .approvedDurationDays(3).copyUniqueCode("UNIQ-CODE-1").build());
        Page<BorrowDTO> byCopyCode = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, "UNIQ-CODE-1", false, false, pageable);
        assertThat(byCopyCode.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getLibraryBorrowsPaged overdue flag matches only overdue RECEIVED loans")
    void getLibraryBorrowsPaged_overdueFlag() {
        Borrow overdueBorrow = borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book).borrowType(BorrowType.PHYSICAL)
                .bookCopy(bookCopy).status(BorrowStatus.RECEIVED)
                .dueDate(LocalDateTime.now().minusDays(2))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BookCopy copy2 = bookCopyRepository.save(BookCopy.builder().book(book).library(library)
                .copyNumber(2).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        Borrow notOverdueBorrow = borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book).borrowType(BorrowType.PHYSICAL)
                .bookCopy(copy2).status(BorrowStatus.RECEIVED)
                .dueDate(LocalDateTime.now().plusDays(5))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        Pageable pageable = PageRequest.of(0, 10);
        Page<BorrowDTO> overdue = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, null, false, true, pageable);
        assertThat(overdue.getContent()).extracting(BorrowDTO::getId).containsExactly(overdueBorrow.getId());
        assertThat(overdue.getContent()).extracting(BorrowDTO::getId).doesNotContain(notOverdueBorrow.getId());
    }

    @Test
    @DisplayName("getLibraryBorrowsPaged needsAttention flag matches REQUESTED and actionable RECEIVED loans")
    void getLibraryBorrowsPaged_needsAttentionFlag() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO requested = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        BookCopy copy2 = bookCopyRepository.save(BookCopy.builder().book(book).library(library)
                .copyNumber(2).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        Borrow returnRequestedBorrow = borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book).borrowType(BorrowType.PHYSICAL)
                .bookCopy(copy2).status(BorrowStatus.RECEIVED)
                .dueDate(LocalDateTime.now().plusDays(5))
                .returnRequestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BookCopy copy3 = bookCopyRepository.save(BookCopy.builder().book(book).library(library)
                .copyNumber(3).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        Borrow handedOverBorrow = borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book).borrowType(BorrowType.PHYSICAL)
                .bookCopy(copy3).status(BorrowStatus.RECEIVED)
                .dueDate(LocalDateTime.now().plusDays(5))
                .handedOverByUserAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BookCopy copy4 = bookCopyRepository.save(BookCopy.builder().book(book).library(library)
                .copyNumber(4).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        Borrow overdueReceivedBorrow = borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book).borrowType(BorrowType.PHYSICAL)
                .bookCopy(copy4).status(BorrowStatus.RECEIVED)
                .dueDate(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        BookCopy copy5 = bookCopyRepository.save(BookCopy.builder().book(book).library(library)
                .copyNumber(5).status(BookCopyStatus.BORROWED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        Borrow normalActiveBorrow = borrowRepository.save(Borrow.builder()
                .user(memberUser).library(library).book(book).borrowType(BorrowType.PHYSICAL)
                .bookCopy(copy5).status(BorrowStatus.RECEIVED)
                .dueDate(LocalDateTime.now().plusDays(5))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        Pageable pageable = PageRequest.of(0, 20);
        Page<BorrowDTO> needsAttention = borrowService.getLibraryBorrowsPaged(
                library.getId(), null, null, null, true, false, pageable);
        List<Long> ids = needsAttention.getContent().stream().map(BorrowDTO::getId).toList();

        assertThat(ids).contains(requested.getId(), returnRequestedBorrow.getId(),
                handedOverBorrow.getId(), overdueReceivedBorrow.getId());
        assertThat(ids).doesNotContain(normalActiveBorrow.getId());
    }

    // ── cancelByUser ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelByUser cancels a REQUESTED borrow and notifies admins")
    void cancelByUser_requested_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        BorrowDTO cancelled = borrowService.cancelByUser(library.getId(), created.getId());
        assertThat(cancelled.getStatus()).isEqualTo(BorrowStatus.CANCELLED);
        verify(notificationService).notify(eq(adminUser), eq(NotificationType.BORROW_CANCELLED_BY_USER), anyString(), anyString(), anyString(), any(), anyString());

        BookCopy copy = bookCopyRepository.findById(bookCopy.getId()).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("cancelByUser cancels an APPROVED borrow and frees the borrowed copy")
    void cancelByUser_approved_freesCopy() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO cancelled = borrowService.cancelByUser(library.getId(), created.getId());
        assertThat(cancelled.getStatus()).isEqualTo(BorrowStatus.CANCELLED);

        BookCopy copy = bookCopyRepository.findById(created.getBookCopyId()).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("cancelByUser throws for wrong library / not owner / wrong status")
    void cancelByUser_failures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        assertThatThrownBy(() -> borrowService.cancelByUser(library.getId(), 999999L))
                .isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.cancelByUser(other.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.cancelByUser(library.getId(), created.getId()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.cancelByUser(library.getId(), created.getId());
        assertThatThrownBy(() -> borrowService.cancelByUser(library.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("امکان لغو وجود ندارد");
    }

    // ── cancelByLibrarian ────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelByLibrarian cancels a RECEIVED borrow and notifies user")
    void cancelByLibrarian_received_success() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO cancelled = borrowService.cancelByLibrarian(library.getId(), raw.getId());
        assertThat(cancelled.getStatus()).isEqualTo(BorrowStatus.CANCELLED);
        verify(notificationService).notify(eq(memberUser), eq(NotificationType.BORROW_CANCELLED_BY_LIBRARIAN), anyString(), anyString(), anyString(), any(), anyString());

        BookCopy copy = bookCopyRepository.findById(bookCopy.getId()).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("cancelByLibrarian throws for wrong library / non-admin / wrong status")
    void cancelByLibrarian_failures() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RETURNED, null);

        assertThatThrownBy(() -> {
            SecurityTestUtils.setSecurityContext(adminUser, "USER");
            borrowService.cancelByLibrarian(library.getId(), 999999L);
        }).isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.cancelByLibrarian(other.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.cancelByLibrarian(library.getId(), raw.getId()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.cancelByLibrarian(library.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("امکان لغو وجود ندارد");
    }

    // ── requestReturn ────────────────────────────────────────────────────────

    @Test
    @DisplayName("requestReturn records the request and notifies admins")
    void requestReturn_success() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowDTO result = borrowService.requestReturn(library.getId(), raw.getId(),
                ReturnRequest.builder().returnAddress("ret-addr").returnExtension("12345678")
                        .preferredDate(LocalDateTime.now().plusDays(1)).build());

        assertThat(result.getReturnRequestedAt()).isNotNull();
        assertThat(result.getReturnAddress()).isEqualTo("ret-addr");
        verify(notificationService).notify(eq(adminUser), eq(NotificationType.RETURN_REQUESTED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("requestReturn throws for wrong library / not owner / not physical / wrong status / invalid fields")
    void requestReturn_failures() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);

        assertThatThrownBy(() -> {
            SecurityTestUtils.setSecurityContext(memberUser, "USER");
            borrowService.requestReturn(library.getId(), 999999L, ReturnRequest.builder().returnAddress("a").build());
        }).isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.requestReturn(other.getId(), raw.getId(), ReturnRequest.builder().returnAddress("a").build()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.requestReturn(library.getId(), raw.getId(), ReturnRequest.builder().returnAddress("a").build()))
                .isInstanceOf(UnauthorizedException.class);

        Borrow rawDigital = saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.APPROVED, null);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.requestReturn(library.getId(), rawDigital.getId(), ReturnRequest.builder().returnAddress("a").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط برای امانت است");

        Borrow rawRequested = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.REQUESTED, null);
        assertThatThrownBy(() -> borrowService.requestReturn(library.getId(), rawRequested.getId(), ReturnRequest.builder().returnAddress("a").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در دست شماست");

        assertThatThrownBy(() -> borrowService.requestReturn(library.getId(), raw.getId(), ReturnRequest.builder().returnAddress(" ").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("آدرس تحویل کتاب الزامی است");

        assertThatThrownBy(() -> borrowService.requestReturn(library.getId(), raw.getId(),
                ReturnRequest.builder().returnAddress("a").returnExtension("1").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("۸ رقم");
    }

    // ── scheduleReturnPickup ─────────────────────────────────────────────────

    @Test
    @DisplayName("scheduleReturnPickup: librarian-initiated copies delivery address/extension as return defaults")
    void scheduleReturnPickup_librarianInitiated() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        raw.setDeliveryAddress("orig-addr");
        raw.setDeliveryExtension("11112222");
        borrowRepository.save(raw);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO result = borrowService.scheduleReturnPickup(library.getId(), raw.getId(),
                ReturnScheduleRequest.builder().returnCourierName("Courier1")
                        .returnPlannedDate(LocalDateTime.now().plusDays(1)).build());

        assertThat(result.getReturnRequestedAt()).isNotNull();
        assertThat(result.getReturnAddress()).isEqualTo("orig-addr");
        assertThat(result.getReturnExtension()).isEqualTo("11112222");
        assertThat(result.getReturnCourierName()).isEqualTo("Courier1");
        verify(notificationService).notify(eq(memberUser), eq(NotificationType.RETURN_PICKUP_SCHEDULED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("scheduleReturnPickup: not librarian-initiated when return already requested by recipient")
    void scheduleReturnPickup_recipientAlreadyRequested() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.requestReturn(library.getId(), raw.getId(),
                ReturnRequest.builder().returnAddress("user-addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO result = borrowService.scheduleReturnPickup(library.getId(), raw.getId(),
                ReturnScheduleRequest.builder().build());
        assertThat(result.getReturnAddress()).isEqualTo("user-addr");
    }

    @Test
    @DisplayName("scheduleReturnPickup throws for wrong library / non-admin / wrong status")
    void scheduleReturnPickup_failures() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.REQUESTED, null);

        assertThatThrownBy(() -> {
            SecurityTestUtils.setSecurityContext(adminUser, "USER");
            borrowService.scheduleReturnPickup(library.getId(), 999999L, ReturnScheduleRequest.builder().build());
        }).isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.scheduleReturnPickup(other.getId(), raw.getId(), ReturnScheduleRequest.builder().build()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.scheduleReturnPickup(library.getId(), raw.getId(), ReturnScheduleRequest.builder().build()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.scheduleReturnPickup(library.getId(), raw.getId(), ReturnScheduleRequest.builder().build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("فقط برای امانت در دست گیرنده");
    }

    // ── cancelReturnRequest ──────────────────────────────────────────────────

    @Test
    @DisplayName("cancelReturnRequest by owner reverts return fields and notifies admins")
    void cancelReturnRequest_byOwner_success() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.requestReturn(library.getId(), raw.getId(), ReturnRequest.builder().returnAddress("a").build());

        BorrowDTO result = borrowService.cancelReturnRequest(library.getId(), raw.getId());
        assertThat(result.getReturnRequestedAt()).isNull();
        assertThat(result.getReturnAddress()).isNull();
        verify(notificationService, atLeastOnce()).notify(eq(adminUser), eq(NotificationType.RETURN_REQUESTED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("cancelReturnRequest by admin notifies the user")
    void cancelReturnRequest_byAdmin_success() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.requestReturn(library.getId(), raw.getId(), ReturnRequest.builder().returnAddress("a").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO result = borrowService.cancelReturnRequest(library.getId(), raw.getId());
        assertThat(result.getReturnRequestedAt()).isNull();
        verify(notificationService).notify(eq(memberUser), eq(NotificationType.RETURN_PICKUP_SCHEDULED), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("cancelReturnRequest throws for wrong library / unauthorized user / no pending return")
    void cancelReturnRequest_failures() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);

        assertThatThrownBy(() -> {
            SecurityTestUtils.setSecurityContext(memberUser, "USER");
            borrowService.cancelReturnRequest(library.getId(), 999999L);
        }).isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.cancelReturnRequest(other.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class);

        User outsider = createUser("out7@borrow.com");
        SecurityTestUtils.setSecurityContext(outsider, "USER");
        assertThatThrownBy(() -> borrowService.cancelReturnRequest(library.getId(), raw.getId()))
                .isInstanceOf(UnauthorizedException.class);

        // no returnRequestedAt yet
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.cancelReturnRequest(library.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("برای لغو وجود ندارد");
    }

    // ── confirmHandover ──────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmHandover records handover time and notifies admins")
    void confirmHandover_success() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        borrowService.requestReturn(library.getId(), raw.getId(), ReturnRequest.builder().returnAddress("a").build());

        BorrowDTO result = borrowService.confirmHandover(library.getId(), raw.getId());
        assertThat(result.getHandedOverByUserAt()).isNotNull();
        verify(notificationService).notify(eq(adminUser), eq(NotificationType.BOOK_HANDED_OVER), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("confirmHandover throws for wrong library / not owner / wrong status / no return request")
    void confirmHandover_failures() {
        Borrow raw = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);

        assertThatThrownBy(() -> {
            SecurityTestUtils.setSecurityContext(memberUser, "USER");
            borrowService.confirmHandover(library.getId(), 999999L);
        }).isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.confirmHandover(other.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.confirmHandover(library.getId(), raw.getId()))
                .isInstanceOf(UnauthorizedException.class);

        // no returnRequestedAt yet -> throws
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.confirmHandover(library.getId(), raw.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ابتدا باید درخواست برگرداندن");

        Borrow rawRequested = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.REQUESTED, null);
        assertThatThrownBy(() -> borrowService.confirmHandover(library.getId(), rawRequested.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("امکان تحویل کتاب وجود ندارد");
    }

    // ── getBorrowerSummary ───────────────────────────────────────────────────

    @Test
    @DisplayName("getBorrowerSummary aggregates borrow counts correctly")
    void getBorrowerSummary_success() {
        // active physical (RECEIVED)
        saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RECEIVED, bookCopy);
        // pending
        saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.REQUESTED, null);
        // rejected
        saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.REJECTED, null);
        // cancelled
        saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.CANCELLED, null);
        // returned
        saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.RETURNED, null);
        // active digital
        saveRawBorrow(memberUser, BorrowType.DIGITAL, BorrowStatus.APPROVED, null);
        // overdue (APPROVED, due date in past, no return date)
        Borrow overdue = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.APPROVED, null);
        overdue.setDueDate(LocalDateTime.now().minusDays(3));
        borrowRepository.save(overdue);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowerSummaryDTO summary = borrowService.getBorrowerSummary(library.getId(), memberUser.getId());

        assertThat(summary.getUserId()).isEqualTo(memberUser.getId());
        assertThat(summary.getActivePhysical()).isEqualTo(2); // RECEIVED + overdue APPROVED
        assertThat(summary.getActiveDigital()).isEqualTo(1);
        assertThat(summary.getPending()).isEqualTo(1);
        assertThat(summary.getOverdue()).isEqualTo(1);
        assertThat(summary.getReturned()).isEqualTo(1);
        assertThat(summary.getRejected()).isEqualTo(1);
        assertThat(summary.getCancelled()).isEqualTo(1);
        assertThat(summary.getTotalBorrows()).isEqualTo(7);
        // activeLoans includes any APPROVED/RECEIVED borrow regardless of type: the RECEIVED physical,
        // the APPROVED digital, and the overdue APPROVED physical
        assertThat(summary.getActiveLoans()).hasSize(3);
    }

    @Test
    @DisplayName("getBorrowerSummary throws for non-admin / unknown user")
    void getBorrowerSummary_failures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.getBorrowerSummary(library.getId(), adminUser.getId()))
                .isInstanceOf(UnauthorizedException.class);

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        assertThatThrownBy(() -> borrowService.getBorrowerSummary(library.getId(), 999999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getBorrowEvents ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getBorrowEvents returns the timeline for admins, ordered")
    void getBorrowEvents_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approvePhysicalBorrow(library.getId(), created.getId(),
                PhysicalApprovalRequest.builder().plannedDeliveryDate(LocalDateTime.now()).approvedDurationDays(3).build());

        List<com.library.dto.BorrowEventDTO> events = borrowService.getBorrowEvents(library.getId(), created.getId());
        assertThat(events).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events.get(0).getTitle()).isEqualTo("ثبت درخواست");
    }

    @Test
    @DisplayName("getBorrowEvents throws for wrong library / not found / non-admin")
    void getBorrowEvents_failures() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        assertThatThrownBy(() -> {
            SecurityTestUtils.setSecurityContext(adminUser, "USER");
            borrowService.getBorrowEvents(library.getId(), 999999L);
        }).isInstanceOf(ResourceNotFoundException.class);

        Library other = libraryRepository.save(Library.builder().name("Other").owner(adminUser)
                .defaultBorrowDurationDays(14).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        assertThatThrownBy(() -> borrowService.getBorrowEvents(other.getId(), created.getId()))
                .isInstanceOf(BadRequestException.class);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> borrowService.getBorrowEvents(library.getId(), created.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── mapToBorrowDTO edge cases (fullName fallback, overdue flag) ─────────

    @Test
    @DisplayName("mapToBorrowDTO isOverdue is true for an APPROVED loan past its due date")
    void mapToBorrowDTO_overdueFlag() {
        Borrow overdue = saveRawBorrow(memberUser, BorrowType.PHYSICAL, BorrowStatus.APPROVED, bookCopy);
        overdue.setDueDate(LocalDateTime.now().minusDays(1));
        borrowRepository.save(overdue);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        List<BorrowDTO> borrows = borrowService.getUserBorrows(library.getId(), null, null);
        BorrowDTO dto = borrows.stream().filter(b -> b.getId().equals(overdue.getId())).findFirst().orElseThrow();
        assertThat(dto.getIsOverdue()).isTrue();
    }

    @Test
    @DisplayName("fullName falls back to email when both first and last name are blank")
    void fullName_fallsBackToEmail() {
        User noName = userRepository.save(User.builder()
                .email("noname@borrow.com").passwordHash("$2a$10$encoded")
                .firstName("").lastName("")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        addMembership(noName, LibraryMembershipRole.MEMBER, MembershipStatus.APPROVED);

        SecurityTestUtils.setSecurityContext(noName, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).deliveryAddress("addr").build());

        assertThat(created.getUserFullName()).isEqualTo("noname@borrow.com");
    }
}
