package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.dto.StatsDTO;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Stats Controller Tests")
class StatsControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private LibraryRepository libraryRepository;
    @MockBean private BookRepository bookRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private BorrowRepository borrowRepository;

    @Test
    @DisplayName("GET /v1/stats is public — no auth required — 200")
    void getStats_noAuth_ok() throws Exception {
        when(libraryRepository.countByIsActive(true)).thenReturn(5L);
        when(bookRepository.count()).thenReturn(100L);
        when(userRepository.count()).thenReturn(50L);
        when(borrowRepository.countByStatus(any())).thenReturn(10L);
        when(userRepository.countByAccountStatus(any())).thenReturn(40L);

        mockMvc.perform(get("/v1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLibraries").value(5))
                .andExpect(jsonPath("$.data.totalBooks").value(100))
                .andExpect(jsonPath("$.data.totalUsers").value(50))
                .andExpect(jsonPath("$.data.activeBorrows").value(10))
                .andExpect(jsonPath("$.data.activeUsers").value(40));
    }
}
