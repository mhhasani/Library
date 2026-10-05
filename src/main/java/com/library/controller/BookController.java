package com.library.controller;

import com.library.dto.AddBookCopyRequest;
import com.library.dto.ApiResponse;
import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.entity.enums.SensitiveOperation;
import com.library.security.RequiresRecentAuthentication;
import com.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/v1/libraries/{libraryId}/books")
@Tag(name = "Books", description = "Book management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class BookController {

    @Autowired
    private BookService bookService;

    @PostMapping
    @Operation(summary = "Create a new book", description = "Create a new book in a library (admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Book created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "دسترسی غیرمجاز"
            )
    })
    public ResponseEntity<ApiResponse<BookDTO>> createBook(
            @PathVariable Long libraryId,
            @Valid @RequestBody BookRequest request) {
        log.info("Creating book in library: {}", libraryId);
        BookDTO book = bookService.createBook(libraryId, request);
        return new ResponseEntity<>(
                ApiResponse.success("Book created successfully", book),
                HttpStatus.CREATED
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new book with assets in one call",
            description = "Create a book and, optionally in the same request, set its cover image, " +
                    "digital PDF, and physical copy count — instead of separate follow-up calls.")
    public ResponseEntity<ApiResponse<BookDTO>> createBookWithAssets(
            @PathVariable Long libraryId,
            @Valid @RequestPart("book") BookRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart(value = "digital", required = false) MultipartFile digital,
            @RequestParam(value = "digitalVersionName", required = false) String digitalVersionName,
            @RequestParam(value = "copyCount", required = false) Integer copyCount) {
        log.info("Creating book with assets in library: {}", libraryId);
        BookDTO book = bookService.createBookWithAssets(libraryId, request, cover, digital, digitalVersionName, copyCount);
        return new ResponseEntity<>(
                ApiResponse.success("Book created successfully", book),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details", description = "Retrieve book information")
    public ResponseEntity<ApiResponse<BookDTO>> getBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId) {
        log.info("Getting book: {} from library: {}", bookId, libraryId);
        BookDTO book = bookService.getBookById(libraryId, bookId);
        return ResponseEntity.ok(ApiResponse.success("Book retrieved successfully", book));
    }

    @GetMapping
    @Operation(summary = "Get library books", description = "Retrieve all books in a library")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> getBooks(
            @PathVariable Long libraryId,
            Pageable pageable) {
        log.info("Getting books from library: {}", libraryId);
        Page<BookDTO> books = bookService.getLibraryBooks(libraryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Books retrieved successfully", books));
    }

    @GetMapping("/search")
    @Operation(summary = "Search books", description = "Advanced search: query (title/author/publisher), subjectId, yearFrom, yearTo")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> searchBooks(
            @PathVariable Long libraryId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            Pageable pageable) {
        log.info("Searching books in library: {} query={} subjectId={} year={}-{}", libraryId, query, subjectId, yearFrom, yearTo);
        Page<BookDTO> books = bookService.advancedSearchBooks(libraryId, query, subjectId, yearFrom, yearTo, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", books));
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "Update book", description = "Update book details (admin only)")
    public ResponseEntity<ApiResponse<BookDTO>> updateBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @Valid @RequestBody BookRequest request) {
        log.info("Updating book: {} in library: {}", bookId, libraryId);
        BookDTO book = bookService.updateBook(libraryId, bookId, request);
        return ResponseEntity.ok(ApiResponse.success("Book updated successfully", book));
    }

    @PutMapping(value = "/{bookId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update book with assets in one call",
            description = "Full metadata replace, plus optionally in the same request set cover image, " +
                    "digital PDF, and physical copy count — instead of separate follow-up calls.")
    public ResponseEntity<ApiResponse<BookDTO>> updateBookWithAssets(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @Valid @RequestPart("book") BookRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart(value = "digital", required = false) MultipartFile digital,
            @RequestParam(value = "digitalVersionName", required = false) String digitalVersionName,
            @RequestParam(value = "copyCount", required = false) Integer copyCount) {
        log.info("Updating book with assets: {} in library: {}", bookId, libraryId);
        BookDTO book = bookService.updateBookWithAssets(libraryId, bookId, request, cover, digital, digitalVersionName, copyCount);
        return ResponseEntity.ok(ApiResponse.success("Book updated successfully", book));
    }

    @PatchMapping(value = "/{bookId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Partially update a book",
            description = "Only supplied fields/assets are changed — everything (metadata part, cover, " +
                    "digital PDF, copy count) is independently optional.")
    public ResponseEntity<ApiResponse<BookDTO>> patchBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @RequestPart(value = "book", required = false) BookRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart(value = "digital", required = false) MultipartFile digital,
            @RequestParam(value = "digitalVersionName", required = false) String digitalVersionName,
            @RequestParam(value = "copyCount", required = false) Integer copyCount) {
        log.info("Patching book: {} in library: {}", bookId, libraryId);
        BookDTO book = bookService.patchBookWithAssets(libraryId, bookId, request, cover, digital, digitalVersionName, copyCount);
        return ResponseEntity.ok(ApiResponse.success("Book updated successfully", book));
    }

    @DeleteMapping("/{bookId}")
    @RequiresRecentAuthentication(SensitiveOperation.BOOK_DELETE)
    @Operation(summary = "Delete book", description = "Delete a book (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId) {
        log.info("Deleting book: {} from library: {}", bookId, libraryId);
        bookService.deleteBook(libraryId, bookId);
        return ResponseEntity.ok(ApiResponse.success("Book deleted successfully"));
    }

    @PostMapping("/{bookId}/copies")
    @Operation(summary = "Add physical book copies", description = "Add physical copies of a book (admin only)")
    public ResponseEntity<ApiResponse<Void>> addBookCopies(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @Valid @RequestBody AddBookCopyRequest request) {
        log.info("Adding {} copies for book: {} in library: {}", request.getNumberOfCopies(), bookId, libraryId);
        bookService.addBookCopies(libraryId, bookId, request.getNumberOfCopies());
        return new ResponseEntity<>(
                ApiResponse.success("Book copies added successfully"),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{bookId}/copies/count")
    @Operation(summary = "Set total physical copies",
            description = "Set the exact number of physical copies (cannot go below copies currently on loan)")
    public ResponseEntity<ApiResponse<BookDTO>> setBookCopyCount(
            @PathVariable Long libraryId,
            @PathVariable Long bookId,
            @RequestParam int count) {
        BookDTO book = bookService.setBookCopyCount(libraryId, bookId, count);
        return ResponseEntity.ok(ApiResponse.success("تعداد نسخه‌ها به‌روزرسانی شد", book));
    }

    @GetMapping("/deleted")
    @Operation(summary = "List soft-deleted books", description = "List books that have been soft-deleted (admin only), for the restore panel")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> getDeletedBooks(
            @PathVariable Long libraryId,
            Pageable pageable) {
        Page<BookDTO> books = bookService.getDeletedBooks(libraryId, pageable);
        return ResponseEntity.ok(ApiResponse.success("کتاب‌های حذف‌شده", books));
    }

    @PostMapping("/{bookId}/restore")
    @Operation(summary = "Restore a soft-deleted book", description = "Makes a previously deleted book visible/borrowable again (admin only)")
    public ResponseEntity<ApiResponse<BookDTO>> restoreBook(
            @PathVariable Long libraryId,
            @PathVariable Long bookId) {
        log.info("Restoring book: {} in library: {}", bookId, libraryId);
        BookDTO book = bookService.restoreBook(libraryId, bookId);
        return ResponseEntity.ok(ApiResponse.success("کتاب با موفقیت بازگردانی شد", book));
    }
}
