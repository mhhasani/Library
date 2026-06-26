package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.dto.BookDTO;
import com.library.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Global Book Controller Tests")
class GlobalBookControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private BookService bookService;

    @Test
    @DisplayName("GET /v1/books/search is public — no auth required — 200")
    void globalSearch_noAuth_ok() throws Exception {
        BookDTO book = BookDTO.builder().id(1L).title("Clean Code").author("Martin").build();
        Page<BookDTO> page = new PageImpl<>(List.of(book), PageRequest.of(0, 24), 1);
        when(bookService.globalSearch(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/books/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Search with query param — 200")
    void globalSearch_withQuery_ok() throws Exception {
        BookDTO book = BookDTO.builder().id(2L).title("Clean Code").author("Martin").build();
        Page<BookDTO> page = new PageImpl<>(List.of(book), PageRequest.of(0, 24), 1);
        when(bookService.globalSearch(eq("clean"), any())).thenReturn(page);

        mockMvc.perform(get("/v1/books/search").param("query", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Clean Code"));
    }

    @Test
    @DisplayName("Search returns empty page when no results — 200")
    void globalSearch_empty_ok() throws Exception {
        Page<BookDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0, 24), 0);
        when(bookService.globalSearch(eq("xyz"), any())).thenReturn(empty);

        mockMvc.perform(get("/v1/books/search").param("query", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
