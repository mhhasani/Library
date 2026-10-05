package com.library.service;

import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.enums.BookCopyStatus;
import com.library.entity.enums.ClassificationLevel;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.entity.LibrarySubject;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.DigitalBookRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.LibrarySubjectRepository;
import com.library.security.ClassificationGuard;
import com.library.util.SecurityUtils;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private DigitalBookRepository digitalBookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private LibrarySubjectRepository subjectRepository;

    @Autowired
    private CoverImageService coverImageService;

    @Autowired
    private DigitalBookService digitalBookService;

    @Autowired
    private ClassificationGuard classificationGuard;

    public BookDTO createBook(Long libraryId, BookRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        // Check if user is library admin
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند کتاب اضافه کند");
        }

        List<LibrarySubject> subjects = resolveSubjects(libraryId, request.getSubjectIds());
        classificationGuard.assertCanAssign(request.getClassification());

        Book book = Book.builder()
                .library(library)
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publicationYear(request.getPublicationYear())
                .subjects(subjects)
                .description(request.getDescription())
                .autoDigitalBorrowEnabled(request.getAutoDigitalBorrowEnabled() != null ? request.getAutoDigitalBorrowEnabled() : false)
                .classification(request.getClassification() != null
                        ? request.getClassification() : ClassificationLevel.UNCLASSIFIED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        book = bookRepository.save(book);
        log.info("Book created: {} in library {}", book.getTitle(), libraryId);

        return mapToBookDTO(book);
    }

    private void requireApprovedMembership(Long currentUserId, Long libraryId) {
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (!membership.getStatus().equals(MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("عضویت شما هنوز تأیید نشده است");
        }
    }

    public BookDTO getBookById(Long libraryId, Long bookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));
        requireApprovedMembership(currentUserId, libraryId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }
        if (book.getDeletedAt() != null) {
            throw new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId);
        }
        classificationGuard.assertCanRead(book);

        return mapToBookDTO(book);
    }

    public Page<BookDTO> getLibraryBooks(Long libraryId, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));
        requireApprovedMembership(currentUserId, libraryId);

        return bookRepository.findByLibraryIdAndDeletedAtIsNullAndClassificationIn(
                        libraryId, classificationGuard.readableLevels(), pageable)
                .map(this::mapToBookDTO);
    }

    public Page<BookDTO> searchBooks(Long libraryId, String query, Pageable pageable) {
        return advancedSearchBooks(libraryId, query, null, null, null, pageable);
    }

    /** Public cross-library search over all ACTIVE libraries (no membership required). */
    public Page<BookDTO> globalSearch(String query, Pageable pageable) {
        String normalizedQuery = (query != null && !query.isBlank()) ? query.trim().toLowerCase() : null;
        Set<ClassificationLevel> readable = classificationGuard.readableLevels();

        Specification<Book> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("library").get("isActive")));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(root.get("classification").in(readable));
            if (normalizedQuery != null) {
                String pattern = "%" + normalizedQuery + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("publisher"), "")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookRepository.findAll(spec, pageable).map(this::mapToBookDTO);
    }

    public Page<BookDTO> advancedSearchBooks(Long libraryId, String query, Long subjectId,
                                              Integer yearFrom, Integer yearTo, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));
        requireApprovedMembership(currentUserId, libraryId);

        String normalizedQuery = (query != null && !query.isBlank()) ? query.trim().toLowerCase() : null;
        Set<ClassificationLevel> readable = classificationGuard.readableLevels();

        Specification<Book> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("library").get("id"), libraryId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(root.get("classification").in(readable));
            if (normalizedQuery != null) {
                String pattern = "%" + normalizedQuery + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("publisher"), "")), pattern)
                ));
            }
            if (subjectId != null) {
                var subjectsJoin = root.join("subjects", JoinType.INNER);
                predicates.add(cb.equal(subjectsJoin.get("id"), subjectId));
                cq.distinct(true);
            }
            if (yearFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publicationYear"), yearFrom));
            }
            if (yearTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publicationYear"), yearTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookRepository.findAll(spec, pageable).map(this::mapToBookDTO);
    }

    private List<LibrarySubject> resolveSubjects(Long libraryId, List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) return Collections.emptyList();
        return subjectIds.stream().map(id -> {
            LibrarySubject subject = subjectRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("موضوعی با این شناسه پیدا نشد: " + id));
            if (!subject.getLibrary().getId().equals(libraryId)) {
                throw new BadRequestException("این موضوع مربوط به این کتابخانه نیست");
            }
            return subject;
        }).collect(Collectors.toList());
    }

    /** Loads a book for an admin-only edit operation, applying the shared existence/authorization checks. */
    private Book loadBookForAdminEdit(Long libraryId, Long bookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند کتاب‌ها را ویرایش کند");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }
        if (book.getDeletedAt() != null) {
            throw new BadRequestException("این کتاب حذف شده است؛ ابتدا آن را بازگردانید");
        }
        classificationGuard.assertCanRead(book);
        return book;
    }

    /** Same admin/existence/library checks as {@link #loadBookForAdminEdit}, but does NOT
     *  reject soft-deleted books — used by delete/restore/list-deleted which must operate
     *  on (or return) books regardless of their deleted state. */
    private Book loadBookForAdminAction(Long libraryId, Long bookId, String forbiddenMessage) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException(forbiddenMessage);
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }
        classificationGuard.assertCanRead(book);
        return book;
    }

    public BookDTO updateBook(Long libraryId, Long bookId, BookRequest request) {
        Book book = loadBookForAdminEdit(libraryId, bookId);

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setSubjects(resolveSubjects(libraryId, request.getSubjectIds()));
        book.setDescription(request.getDescription());
        book.setAutoDigitalBorrowEnabled(request.getAutoDigitalBorrowEnabled());
        applyClassification(book, request.getClassification());
        book.setUpdatedAt(LocalDateTime.now());

        book = bookRepository.save(book);
        log.info("Book updated: {} in library {}", book.getTitle(), libraryId);

        return mapToBookDTO(book);
    }

    /** Changes the book's label when one is given; a missing label keeps the current one. */
    private void applyClassification(Book book, ClassificationLevel classification) {
        if (classification != null) {
            classificationGuard.assertCanAssign(classification);
            book.setClassification(classification);
        }
    }

    /** Partial update: only fields present (non-null) in {@code request} are changed. */
    public BookDTO patchBook(Long libraryId, Long bookId, BookRequest request) {
        Book book = loadBookForAdminEdit(libraryId, bookId);

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BadRequestException("عنوان کتاب را وارد کنید");
            }
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            if (request.getAuthor().isBlank()) {
                throw new BadRequestException("نام نویسنده را وارد کنید");
            }
            book.setAuthor(request.getAuthor());
        }
        if (request.getPublisher() != null) book.setPublisher(request.getPublisher());
        if (request.getPublicationYear() != null) book.setPublicationYear(request.getPublicationYear());
        if (request.getSubjectIds() != null) book.setSubjects(resolveSubjects(libraryId, request.getSubjectIds()));
        if (request.getDescription() != null) book.setDescription(request.getDescription());
        if (request.getAutoDigitalBorrowEnabled() != null) book.setAutoDigitalBorrowEnabled(request.getAutoDigitalBorrowEnabled());
        applyClassification(book, request.getClassification());
        book.setUpdatedAt(LocalDateTime.now());

        book = bookRepository.save(book);
        log.info("Book patched: {} in library {}", book.getTitle(), libraryId);

        return mapToBookDTO(book);
    }

    /**
     * Applies whichever optional assets are present in one call — used by the create/update/patch
     * "with assets" entry points so the admin edit modal can do everything in a single request
     * instead of separately calling the cover/digital/copy-count endpoints.
     */
    private void applyBookAssets(Long libraryId, Long bookId, MultipartFile cover, MultipartFile digital,
                                  String digitalVersionName, Integer copyCount) {
        if (cover != null && !cover.isEmpty()) {
            coverImageService.uploadCoverImage(libraryId, bookId, cover);
        }
        if (digital != null && !digital.isEmpty()) {
            digitalBookService.uploadDigitalBook(libraryId, bookId, digital, digitalVersionName);
        }
        if (copyCount != null) {
            setBookCopyCount(libraryId, bookId, copyCount);
        }
    }

    /** Create a book and, in the same call, optionally set its cover, digital PDF, and copy count. */
    public BookDTO createBookWithAssets(Long libraryId, BookRequest request, MultipartFile cover,
                                         MultipartFile digital, String digitalVersionName, Integer copyCount) {
        BookDTO created = createBook(libraryId, request);
        applyBookAssets(libraryId, created.getId(), cover, digital, digitalVersionName, copyCount);
        return getBookById(libraryId, created.getId());
    }

    /** Full update of a book's metadata plus, in the same call, optionally its cover/digital/copy count. */
    public BookDTO updateBookWithAssets(Long libraryId, Long bookId, BookRequest request, MultipartFile cover,
                                         MultipartFile digital, String digitalVersionName, Integer copyCount) {
        updateBook(libraryId, bookId, request);
        applyBookAssets(libraryId, bookId, cover, digital, digitalVersionName, copyCount);
        return getBookById(libraryId, bookId);
    }

    /**
     * Partial update: {@code request} is optional (may be entirely absent if only assets are being
     * changed), and any provided assets are applied — all independently optional.
     */
    public BookDTO patchBookWithAssets(Long libraryId, Long bookId, BookRequest request, MultipartFile cover,
                                        MultipartFile digital, String digitalVersionName, Integer copyCount) {
        if (request != null) {
            patchBook(libraryId, bookId, request);
        } else {
            // Still enforce the admin/existence guard even when only assets are supplied.
            loadBookForAdminEdit(libraryId, bookId);
        }
        applyBookAssets(libraryId, bookId, cover, digital, digitalVersionName, copyCount);
        return getBookById(libraryId, bookId);
    }

    /** Soft-delete: marks the book as deleted (hidden from browsing/search/borrowing/history
     *  everywhere) without removing its row, so existing FK references stay intact and an
     *  admin can restore it later via {@link #restoreBook}. */
    public void deleteBook(Long libraryId, Long bookId) {
        Book book = loadBookForAdminAction(libraryId, bookId, "فقط مدیر کتابخانه می‌تواند کتاب حذف کند");

        if (book.getDeletedAt() != null) {
            throw new BadRequestException("این کتاب قبلاً حذف شده است");
        }

        book.setDeletedAt(LocalDateTime.now());
        bookRepository.save(book);
        log.info("Book soft-deleted: {} from library {}", book.getTitle(), libraryId);
    }

    /** Restores a previously soft-deleted book, making it visible/borrowable again everywhere. */
    public BookDTO restoreBook(Long libraryId, Long bookId) {
        Book book = loadBookForAdminAction(libraryId, bookId, "فقط مدیر کتابخانه می‌تواند کتاب بازگرداند");

        if (book.getDeletedAt() == null) {
            throw new BadRequestException("این کتاب حذف نشده است");
        }

        book.setDeletedAt(null);
        book = bookRepository.save(book);
        log.info("Book restored: {} in library {}", book.getTitle(), libraryId);

        return mapToBookDTO(book);
    }

    /** Lists soft-deleted books for the admin's "بازگردانی" panel. */
    public Page<BookDTO> getDeletedBooks(Long libraryId, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند کتاب‌های حذف‌شده را ببیند");
        }

        return bookRepository.findByLibraryIdAndDeletedAtIsNotNullAndClassificationIn(
                        libraryId, classificationGuard.readableLevels(), pageable)
                .map(this::mapToBookDTO);
    }

    public void addBookCopies(Long libraryId, Long bookId, Integer numberOfCopies) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند نسخه اضافه کند");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }

        List<BookCopy> existingCopies = bookCopyRepository.findByBookId(bookId);
        int nextCopyNumber = existingCopies.size() + 1;

        for (int i = 0; i < numberOfCopies; i++) {
            BookCopy copy = BookCopy.builder()
                    .book(book)
                    .library(library)
                    .copyNumber(nextCopyNumber++)
                    .status(BookCopyStatus.AVAILABLE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            bookCopyRepository.save(copy);
        }

        log.info("Added {} copies for book {} in library {}", numberOfCopies, bookId, libraryId);
    }

    /**
     * Set the total number of physical copies to {@code targetCount}. Increases by adding new copies
     * or decreases by removing AVAILABLE copies — never below the number currently on loan.
     */
    public BookDTO setBookCopyCount(Long libraryId, Long bookId, int targetCount) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN && !SecurityUtils.hasRole("SYSTEM_ADMIN")) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند تعداد نسخه‌ها را تغییر دهد");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId));
        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }
        if (targetCount < 0) {
            throw new BadRequestException("تعداد نسخه نامعتبر است");
        }

        List<BookCopy> all = bookCopyRepository.findByBookId(bookId);
        int total = all.size();
        long onLoan = all.stream().filter(c -> c.getStatus() != BookCopyStatus.AVAILABLE).count();

        if (targetCount < onLoan) {
            throw new BadRequestException(String.format(
                    "در حال حاضر %d نسخه از این کتاب در امانت است؛ تعداد نسخه‌ها نمی‌تواند کمتر از %d باشد.",
                    onLoan, onLoan));
        }

        if (targetCount > total) {
            addBookCopies(libraryId, bookId, targetCount - total);
        } else if (targetCount < total) {
            int toRemove = total - targetCount;
            // Only copies with no borrow history at all can be hard-deleted — an AVAILABLE
            // copy that was borrowed and later returned is still referenced by that Borrow
            // row (book_copy_id), and deleting it would violate the FK constraint.
            List<BookCopy> removable = all.stream()
                    .filter(c -> c.getStatus() == BookCopyStatus.AVAILABLE)
                    .filter(c -> borrowRepository.findByBookCopyId(c.getId()).isEmpty())
                    .sorted((a, b) -> Integer.compare(b.getCopyNumber(), a.getCopyNumber()))
                    .limit(toRemove)
                    .collect(Collectors.toList());
            if (removable.size() < toRemove) {
                throw new BadRequestException(
                        "امکان کاهش به این تعداد نیست: برخی نسخه‌های موجود سابقه‌ی امانت دارند و قابل حذف نیستند.");
            }
            bookCopyRepository.deleteAll(removable);
            log.info("Removed {} available copies for book {} in library {}", removable.size(), bookId, libraryId);
        }

        Book refreshed = bookRepository.findById(bookId).orElseThrow();
        return mapToBookDTO(refreshed);
    }

    public BookDTO mapToBookDTO(Book book) {
        long totalCopies = bookCopyRepository.findByBookId(book.getId()).size();
        long availableCopies = bookCopyRepository.countByBookIdAndStatus(book.getId(), BookCopyStatus.AVAILABLE);
        boolean hasDigitalVersions = !digitalBookRepository.findByBookId(book.getId()).isEmpty();

        String coverImageUrl = null;
        Long coverImageFileResourceId = null;
        if (book.getCoverImage() != null) {
            coverImageFileResourceId = book.getCoverImage().getId();
            coverImageUrl = "/api/v1/files/" + book.getCoverImage().getId();
        }

        List<Long> subjectIds = book.getSubjects() != null
                ? book.getSubjects().stream().map(LibrarySubject::getId).collect(Collectors.toList())
                : Collections.emptyList();
        List<String> subjectNames = book.getSubjects() != null
                ? book.getSubjects().stream().map(LibrarySubject::getName).collect(Collectors.toList())
                : Collections.emptyList();

        return BookDTO.builder()
                .id(book.getId())
                .libraryId(book.getLibrary().getId())
                .libraryName(book.getLibrary().getName())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publicationYear(book.getPublicationYear())
                .subjectIds(subjectIds)
                .subjectNames(subjectNames)
                .description(book.getDescription())
                .coverImageUrl(coverImageUrl)
                .coverImageFileResourceId(coverImageFileResourceId)
                .autoDigitalBorrowEnabled(book.getAutoDigitalBorrowEnabled())
                .classification(book.getClassification())
                .availableCopiesCount(availableCopies)
                .totalCopiesCount(totalCopies)
                .hasDigitalVersions(hasDigitalVersions)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .deletedAt(book.getDeletedAt())
                .build();
    }
}
