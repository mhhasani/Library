package com.library.controller;

import com.library.dto.AddBookCopyRequest;
import com.library.dto.ApiResponse;
import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{bookId}")
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
}
