package com.library.service;

import com.library.dto.DigitalBookDTO;
import com.library.entity.Book;
import com.library.entity.DigitalBook;
import com.library.entity.FileResource;
import com.library.entity.User;
import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.BorrowRepository;
import com.library.repository.BookRepository;
import com.library.repository.DigitalBookRepository;
import com.library.repository.FileResourceRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DigitalBookService {

    private static final String FORMAT = "PDF";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/octet-stream"
    );

    @Autowired private DigitalBookRepository digitalBookRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private FileResourceRepository fileResourceRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private BorrowRepository borrowRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StorageService storageService;

    public DigitalBookDTO uploadDigitalBook(Long libraryId, Long bookId, MultipartFile file, String versionName) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // Check admin
        var membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this library"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can upload digital books");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
        }

        // Validate content type
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only PDF files are accepted");
        }

        // One PDF per book
        if (digitalBookRepository.findByBookIdAndFileFormat(bookId, FORMAT).isPresent()) {
            throw new BadRequestException("A PDF version already exists for this book. Delete it first.");
        }

        // Compute checksum for deduplication
        String checksum = computeChecksum(file);
        FileResource fileResource = fileResourceRepository.findByChecksumSha256(checksum)
                .orElseGet(() -> {
                    String storedPath = storageService.store(file, "digital-books");
                    User uploader = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    return fileResourceRepository.save(FileResource.builder()
                            .originalFilename(file.getOriginalFilename())
                            .storedFilename(storedPath.substring(storedPath.lastIndexOf("/") + 1))
                            .filePath(storedPath)
                            .fileSizeBytes(file.getSize())
                            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                            .checksumSha256(checksum)
                            .uploadedBy(uploader)
                            .build());
                });

        DigitalBook digitalBook = DigitalBook.builder()
                .book(book)
                .fileResource(fileResource)
                .fileFormat(FORMAT)
                .versionName(versionName)
                .build();

        digitalBook = digitalBookRepository.save(digitalBook);
        log.info("Uploaded digital book: bookId={} format=PDF", bookId);
        return toDTO(digitalBook);
    }

    @Transactional(readOnly = true)
    public List<DigitalBookDTO> listDigitalBooks(Long libraryId, Long bookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this library"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
        }

        return digitalBookRepository.findByBookId(bookId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Resource downloadDigitalBook(Long digitalBookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        DigitalBook digitalBook = digitalBookRepository.findById(digitalBookId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital book not found: " + digitalBookId));

        Long bookId = digitalBook.getBook().getId();
        Long libraryId = digitalBook.getBook().getLibrary().getId();

        // Admins can always download
        boolean isAdmin = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .map(m -> m.getRole() == LibraryMembershipRole.ADMIN)
                .orElse(false);

        if (!isAdmin) {
            // Regular users: must have an active APPROVED DIGITAL borrow for the book (any format)
            boolean hasAccess = borrowRepository.findActiveBorrowByUserAndBookAndType(
                            currentUserId, bookId, com.library.entity.enums.BorrowType.DIGITAL).stream()
                    .anyMatch(b -> b.getStatus() == BorrowStatus.APPROVED && b.getReturnDate() == null);
            if (!hasAccess) {
                throw new UnauthorizedException("You do not have an approved digital borrow for this book");
            }
        }

        return storageService.load(digitalBook.getFileResource().getFilePath());
    }

    public void deleteDigitalBook(Long libraryId, Long bookId, Long digitalBookId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        var membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this library"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can delete digital books");
        }

        DigitalBook digitalBook = digitalBookRepository.findById(digitalBookId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital book not found: " + digitalBookId));
        if (!digitalBook.getBook().getId().equals(bookId)) {
            throw new BadRequestException("Digital book does not belong to this book");
        }

        // Only delete file if no other digital book references it
        FileResource fileResource = digitalBook.getFileResource();
        digitalBookRepository.delete(digitalBook);

        long otherRefs = digitalBookRepository.countByFileResourceId(fileResource.getId());
        if (otherRefs == 0) {
            storageService.delete(fileResource.getFilePath());
            fileResourceRepository.delete(fileResource);
        }

        log.info("Deleted digital book: id={}", digitalBookId);
    }

    public String getContentType(Long digitalBookId) {
        DigitalBook digitalBook = digitalBookRepository.findById(digitalBookId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital book not found: " + digitalBookId));
        return digitalBook.getFileResource().getContentType();
    }

    public String getOriginalFilename(Long digitalBookId) {
        DigitalBook digitalBook = digitalBookRepository.findById(digitalBookId)
                .orElseThrow(() -> new ResourceNotFoundException("Digital book not found: " + digitalBookId));
        return digitalBook.getFileResource().getOriginalFilename();
    }

    private DigitalBookDTO toDTO(DigitalBook db) {
        return DigitalBookDTO.builder()
                .id(db.getId())
                .bookId(db.getBook().getId())
                .fileFormat(db.getFileFormat())
                .versionName(db.getVersionName())
                .originalFilename(db.getFileResource().getOriginalFilename())
                .fileSizeBytes(db.getFileResource().getFileSizeBytes())
                .createdAt(db.getCreatedAt())
                .build();
    }

    private String computeChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute file checksum", e);
        }
    }
}
