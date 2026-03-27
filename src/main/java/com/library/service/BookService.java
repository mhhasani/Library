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
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        // Check if user is library admin
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can create books");
        }

        LibrarySubject subject = resolveSubject(libraryId, request.getSubjectId());

        Book book = Book.builder()
                .library(library)
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publicationYear(request.getPublicationYear())
                .subject(subject)
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
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));
        if (!membership.getStatus().equals(MembershipStatus.APPROVED)) {
            throw new UnauthorizedException("User membership is not approved");
        }
    }

    public BookDTO getBookById(Long libraryId, Long bookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));
        requireApprovedMembership(currentUserId, libraryId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
        }

        return mapToBookDTO(book);
    }

    public Page<BookDTO> getLibraryBooks(Long libraryId, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));
        requireApprovedMembership(currentUserId, libraryId);

        return bookRepository.findByLibraryId(libraryId, pageable)
                .map(this::mapToBookDTO);
    }

    public Page<BookDTO> searchBooks(Long libraryId, String query, Pageable pageable) {
        return advancedSearchBooks(libraryId, query, null, null, null, pageable);
    }

    public Page<BookDTO> advancedSearchBooks(Long libraryId, String query, Long subjectId,
                                              Integer yearFrom, Integer yearTo, Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));
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
                predicates.add(cb.equal(root.get("subject").get("id"), subjectId));
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

    private LibrarySubject resolveSubject(Long libraryId, Long subjectId) {
        if (subjectId == null) return null;
        LibrarySubject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));
        if (!subject.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Subject does not belong to this library");
        }
        return subject;
    }

    public BookDTO updateBook(Long libraryId, Long bookId, BookRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can update books");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setSubject(resolveSubject(libraryId, request.getSubjectId()));
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
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can delete books");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
        }

        bookRepository.delete(book);
        log.info("Book deleted: {} from library {}", book.getTitle(), libraryId);
    }

    public void addBookCopies(Long libraryId, Long bookId, Integer numberOfCopies) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Library not found with id: " + libraryId));

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this library"));

        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can add book copies");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
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

        return BookDTO.builder()
                .id(book.getId())
                .libraryId(book.getLibrary().getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publicationYear(book.getPublicationYear())
                .subjectId(book.getSubject() != null ? book.getSubject().getId() : null)
                .subjectName(book.getSubject() != null ? book.getSubject().getName() : null)
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
