package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BookDTO;
import com.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/books")
@Tag(name = "Global Book Search", description = "Public cross-library book search")
public class GlobalBookController {

    @Autowired
    private BookService bookService;

    @GetMapping("/search")
    @Operation(summary = "Search books across all libraries",
            description = "Public search over all active libraries; returns books with their library name")
    public ResponseEntity<ApiResponse<Page<BookDTO>>> globalSearch(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        Page<BookDTO> result = bookService.globalSearch(query, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", result));
    }
}
