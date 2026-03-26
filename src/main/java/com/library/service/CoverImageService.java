package com.library.service;

import com.library.entity.Book;
import com.library.entity.FileResource;
import com.library.entity.User;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.BookRepository;
import com.library.repository.FileResourceRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class CoverImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    @Autowired private BookRepository bookRepository;
    @Autowired private FileResourceRepository fileResourceRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StorageService storageService;

    public void uploadCoverImage(Long libraryId, Long bookId, MultipartFile file) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        var membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("Not a member of this library"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("Only library admins can upload cover images");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image type. Allowed: JPEG, PNG, WEBP, GIF");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        if (!book.getLibrary().getId().equals(libraryId)) {
            throw new BadRequestException("Book does not belong to this library");
        }

        // Delete old cover image file if exists and not referenced by other books
        if (book.getCoverImage() != null) {
            FileResource old = book.getCoverImage();
            book.setCoverImage(null);
            bookRepository.save(book);
            boolean stillUsed = bookRepository.existsByCoverImageId(old.getId());
            if (!stillUsed) {
                storageService.delete(old.getFilePath());
                fileResourceRepository.delete(old);
            }
        }

        String checksum = computeChecksum(file);
        User uploader = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FileResource fileResource = fileResourceRepository.findByChecksumSha256(checksum)
                .orElseGet(() -> {
                    String storedPath = storageService.store(file, "covers");
                    return fileResourceRepository.save(FileResource.builder()
                            .originalFilename(file.getOriginalFilename())
                            .storedFilename(storedPath.substring(storedPath.lastIndexOf("/") + 1))
                            .filePath(storedPath)
                            .fileSizeBytes(file.getSize())
                            .contentType(contentType)
                            .checksumSha256(checksum)
                            .uploadedBy(uploader)
                            .build());
                });

        book.setCoverImage(fileResource);
        bookRepository.save(book);
        log.info("Cover image uploaded for book: {}", bookId);
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
