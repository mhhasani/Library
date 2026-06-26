package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.StatsDTO;
import com.library.entity.enums.BorrowStatus;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/stats")
@Tag(name = "Stats", description = "Public system statistics")
public class StatsController {

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @GetMapping
    @Operation(summary = "Get system statistics", description = "Returns public statistics about the system")
    public ResponseEntity<ApiResponse<StatsDTO>> getStats() {
        StatsDTO stats = StatsDTO.builder()
                .totalLibraries(libraryRepository.countByIsActive(true))
                .totalBooks(bookRepository.count())
                .totalUsers(userRepository.count())
                .activeBorrows(borrowRepository.countByStatus(BorrowStatus.APPROVED))
                .activeUsers(userRepository.countByAccountStatus(com.library.entity.enums.AccountStatus.ACTIVE))
                .build();
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved successfully", stats));
    }
}
