package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.entity.Book;
import com.library.entity.LibraryMembership;
import com.library.entity.enums.BookCopyStatus;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/libraries/{libraryId}/stats")
@Tag(name = "Library Stats", description = "Detailed library statistics for admins")
@SecurityRequirement(name = "Bearer Authentication")
public class LibraryStatsController {

    @Autowired private BorrowRepository borrowRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private BookCopyRepository bookCopyRepository;

    @GetMapping("/most-borrowed")
    @Operation(summary = "Most borrowed books", description = "Top books by borrow count (admin only)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> mostBorrowed(
            @PathVariable Long libraryId,
            @RequestParam(defaultValue = "10") int limit) {
        requireAdmin(libraryId);
        List<Object[]> rows = borrowRepository.findMostBorrowedBooks(libraryId, PageRequest.of(0, limit));
        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bookId", r[0]);
            m.put("bookTitle", r[1]);
            m.put("borrowCount", r[2]);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Most borrowed books", result));
    }

    @GetMapping("/underused")
    @Operation(summary = "Underused books", description = "Books never borrowed (admin only)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> underused(@PathVariable Long libraryId) {
        requireAdmin(libraryId);
        List<Book> books = borrowRepository.findNeverBorrowedBooks(libraryId);
        List<Map<String, Object>> result = books.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bookId", b.getId());
            m.put("bookTitle", b.getTitle());
            m.put("author", b.getAuthor());
            long available = bookCopyRepository.countByBookIdAndStatus(b.getId(), BookCopyStatus.AVAILABLE);
            long total = bookCopyRepository.findByBookId(b.getId()).size();
            m.put("availableCopies", available);
            m.put("totalCopies", total);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Underused books", result));
    }

    @GetMapping("/user-activity")
    @Operation(summary = "User borrow activity", description = "Top active borrowers (admin only)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> userActivity(
            @PathVariable Long libraryId,
            @RequestParam(defaultValue = "10") int limit) {
        requireAdmin(libraryId);
        List<Object[]> rows = borrowRepository.findBorrowCountByUser(libraryId, PageRequest.of(0, limit));
        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", r[0]);
            m.put("userEmail", r[1]);
            m.put("borrowCount", r[2]);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("User borrow activity", result));
    }

    private void requireAdmin(Long libraryId) {
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> new ResourceNotFoundException("کتابخانه پیدا نشد"));
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(currentUserId, libraryId)
                .orElseThrow(() -> new UnauthorizedException("شما عضو این کتابخانه نیستید"));
        if (membership.getRole() != LibraryMembershipRole.ADMIN) {
            throw new UnauthorizedException("فقط مدیر کتابخانه می‌تواند آمار را ببیند");
        }
    }
}
