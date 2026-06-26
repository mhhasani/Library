package com.library.service;

import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.entity.Book;
import com.library.entity.BookCopy;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.enums.BookCopyStatus;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.entity.LibrarySubject;
import com.library.repository.BookCopyRepository;
import com.library.repository.BookRepository;
import com.library.repository.DigitalBookRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.LibrarySubjectRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private DigitalBookRepository digitalBookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private LibrarySubjectRepository subjectRepository;

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

        Book book = Book.builder()
                .library(library)
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publicationYear(request.getPublicationYear())
                .subjects(subjects)
                .description(request.getDescription())
                .autoDigitalBorrowEnabled(request.getAutoDigitalBorrowEnabled() != null ? request.getAutoDigitalBorrowEnabled() : false)
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

        return mapToBookDTO(book);
    }

    public Page<BookDTO> getLibraryBooks(Long libraryId, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));
        requireApprovedMembership(currentUserId, libraryId);

        return bookRepository.findByLibraryId(libraryId, pageable)
                .map(this::mapToBookDTO);
    }

    public Page<BookDTO> searchBooks(Long libraryId, String query, Pageable pageable) {
        return advancedSearchBooks(libraryId, query, null, null, null, pageable);
    }

    /** Public cross-library search over all ACTIVE libraries (no membership required). */
    public Page<BookDTO> globalSearch(String query, Pageable pageable) {
        String normalizedQuery = (query != null && !query.isBlank()) ? query.trim().toLowerCase() : null;

        Specification<Book> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("library").get("isActive")));
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

        Specification<Book> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("library").get("id"), libraryId));
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

    public BookDTO updateBook(Long libraryId, Long bookId, BookRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        Library library = libraryRepository.findById(libraryId)
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

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setSubjects(resolveSubjects(libraryId, request.getSubjectIds()));
        book.setDescription(request.getDescription());
        book.setAutoDigitalBorrowEnabled(request.getAutoDigitalBorrowEnabled());
        book.setUpdatedAt(LocalDateTime.now());

        book = bookRepository.save(book);
        log.info("Book updated: {} in library {}", book.getTitle(), libraryId);

        return mapToBookDTO(book);
    }

    public void deleteBook(Long libraryId, Long bookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه‌ای با این شناسه پیدا نشد: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند کتاب حذف کند");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابی با این شناسه پیدا نشد: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("این کتاب مربوط به این کتابخانه نیست");
        }

        bookRepository.delete(book);
        log.info("Book deleted: {} from library {}", book.getTitle(), libraryId);
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
            // remove AVAILABLE copies with the highest copy numbers first
            List<BookCopy> removable = all.stream()
                    .filter(c -> c.getStatus() == BookCopyStatus.AVAILABLE)
                    .sorted((a, b) -> Integer.compare(b.getCopyNumber(), a.getCopyNumber()))
                    .limit(toRemove)
                    .collect(Collectors.toList());
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
                .availableCopiesCount(availableCopies)
                .totalCopiesCount(totalCopies)
                .hasDigitalVersions(hasDigitalVersions)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
