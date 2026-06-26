package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BookDTO;
import com.library.dto.BorrowDTO;
import com.library.entity.enums.BorrowType;
import com.library.service.BorrowService;
import com.library.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Current-user, cross-library views: borrows, downloads, and favorites — shown in the profile. */
@RestController
@RequestMapping("/v1/me")
@Tag(name = "Me", description = "Current user's cross-library borrows, downloads and favorites")
@SecurityRequirement(name = "Bearer Authentication")
public class MeController {

    @Autowired private BorrowService borrowService;
    @Autowired private FavoriteService favoriteService;

    @GetMapping("/borrows")
    @Operation(summary = "My borrows across all libraries", description = "Paginated + searchable; filter by type")
    public ResponseEntity<ApiResponse<Page<BorrowDTO>>> myBorrows(
            @RequestParam(required = false) BorrowType type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("My borrows retrieved",
                borrowService.getMyBorrowsPaged(type, search, pageable)));
    }

    @PostMapping("/favorites/{bookId}")
    @Operation(summary = "Toggle a book favorite")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleFavorite(@PathVariable Long bookId) {
        boolean favorited = favoriteService.toggle(bookId);
        return ResponseEntity.ok(ApiResponse.success("Favorite toggled", Map.of("favorited", favorited)));
    }

    @GetMapping("/favorites/ids")
    @Operation(summary = "IDs of my favorite books")
    public ResponseEntity<ApiResponse<List<Long>>> favoriteIds() {
        return ResponseEntity.ok(ApiResponse.success("Favorite ids", favoriteService.getFavoriteBookIds()));
    }

    @GetMapping("/favorites")
    @Operation(summary = "My favorite books (paginated + searchable)")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> favorites(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved",
                favoriteService.getFavorites(search, pageable)));
    }
}
