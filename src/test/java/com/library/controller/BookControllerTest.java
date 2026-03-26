package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.BookDTO;
import com.library.dto.BookRequest;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Book Controller Tests")
class BookControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    private BookRequest bookRequest;
    private BookDTO bookDTO;

    @BeforeEach
    void setUp() {
        bookRequest = BookRequest.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .publisher("Prentice Hall")
                .publicationYear(2008)
                .description("A handbook of agile software craftsmanship")
                .autoDigitalBorrowEnabled(false)
                .build();

        bookDTO = BookDTO.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .publisher("Prentice Hall")
                .publicationYear(2008)
                .description("A handbook of agile software craftsmanship")
                .coverImageUrl("http://example.com/clean-code.jpg")
                .autoDigitalBorrowEnabled(false)
                .build();
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should create book successfully")
    void testCreateBookSuccess() throws Exception {
        when(bookService.createBook(1L, bookRequest)).thenReturn(bookDTO);

        mockMvc.perform(post("/v1/libraries/1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Book created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 400 when creating book with invalid request")
    void testCreateBookInvalidRequest() throws Exception {
        BookRequest invalidRequest = BookRequest.builder()
                .title("")
                .author("")
                .build();

        mockMvc.perform(post("/v1/libraries/1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when library not found")
    void testCreateBookLibraryNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Library not found"))
                .when(bookService).createBook(999L, bookRequest);

        mockMvc.perform(post("/v1/libraries/999/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when user is not admin")
    void testCreateBookUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can create books"))
                .when(bookService).createBook(1L, bookRequest);

        mockMvc.perform(post("/v1/libraries/1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get book by id successfully")
    void testGetBookSuccess() throws Exception {
        when(bookService.getBookById(1L, 1L)).thenReturn(bookDTO);

        mockMvc.perform(get("/v1/libraries/1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when book not found")
    void testGetBookNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found"))
                .when(bookService).getBookById(1L, 999L);

        mockMvc.perform(get("/v1/libraries/1/books/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get all books with pagination")
    void testGetBooksSuccess() throws Exception {
        Page<BookDTO> bookPage = new PageImpl<>(Arrays.asList(bookDTO), PageRequest.of(0, 10), 1);
        when(bookService.getLibraryBooks(eq(1L), any())).thenReturn(bookPage);

        mockMvc.perform(get("/v1/libraries/1/books")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get books with empty result")
    void testGetBooksEmpty() throws Exception {
        Page<BookDTO> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        when(bookService.getLibraryBooks(eq(1L), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/v1/libraries/1/books")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("Should return 401 when user is not authenticated")
    void testCreateBookUnAuthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isUnauthorized());
    }
}
