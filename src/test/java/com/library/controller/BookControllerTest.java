package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.AddBookCopyRequest;
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

    // ---------- consolidated create/update/patch with assets (multipart) ----------

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("POST multipart: creates a book with cover + digital + copy count in one call")
    void testCreateBookWithAssets_success() throws Exception {
        when(bookService.createBookWithAssets(eq(1L), any(BookRequest.class), any(), any(), any(), any()))
                .thenReturn(bookDTO);

        org.springframework.mock.web.MockMultipartFile bookPart = new org.springframework.mock.web.MockMultipartFile(
                "book", "", "application/json", objectMapper.writeValueAsBytes(bookRequest));
        org.springframework.mock.web.MockMultipartFile coverPart = new org.springframework.mock.web.MockMultipartFile(
                "cover", "cover.jpg", "image/jpeg", "img".getBytes());
        org.springframework.mock.web.MockMultipartFile digitalPart = new org.springframework.mock.web.MockMultipartFile(
                "digital", "book.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books")
                        .file(bookPart).file(coverPart).file(digitalPart)
                        .param("digitalVersionName", "v1")
                        .param("copyCount", "3"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("POST multipart: book part alone (no assets) still creates the book")
    void testCreateBookWithAssets_noAssets() throws Exception {
        when(bookService.createBookWithAssets(eq(1L), any(BookRequest.class), any(), any(), any(), any()))
                .thenReturn(bookDTO);

        org.springframework.mock.web.MockMultipartFile bookPart = new org.springframework.mock.web.MockMultipartFile(
                "book", "", "application/json", objectMapper.writeValueAsBytes(bookRequest));

        mockMvc.perform(multipart("/v1/libraries/1/books").file(bookPart))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("PUT multipart: full update with cover + copy count in one call")
    void testUpdateBookWithAssets_success() throws Exception {
        when(bookService.updateBookWithAssets(eq(1L), eq(1L), any(BookRequest.class), any(), any(), any(), any()))
                .thenReturn(bookDTO);

        org.springframework.mock.web.MockMultipartFile bookPart = new org.springframework.mock.web.MockMultipartFile(
                "book", "", "application/json", objectMapper.writeValueAsBytes(bookRequest));
        org.springframework.mock.web.MockMultipartFile coverPart = new org.springframework.mock.web.MockMultipartFile(
                "cover", "cover.jpg", "image/jpeg", "img".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1")
                        .file(bookPart).file(coverPart)
                        .param("copyCount", "4")
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("PATCH multipart: only copy count supplied, no book JSON part at all")
    void testPatchBook_assetsOnlyNoMetadataPart() throws Exception {
        when(bookService.patchBookWithAssets(eq(1L), eq(1L), isNull(), any(), any(), any(), eq(5)))
                .thenReturn(bookDTO);

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, "/v1/libraries/1/books/1")
                        .param("copyCount", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("PATCH multipart: partial book JSON part (only publicationYear) plus no assets")
    void testPatchBook_partialMetadataOnly() throws Exception {
        when(bookService.patchBookWithAssets(eq(1L), eq(1L), any(BookRequest.class), any(), any(), any(), isNull()))
                .thenReturn(bookDTO);

        BookRequest partial = BookRequest.builder().publicationYear(2099).build();
        org.springframework.mock.web.MockMultipartFile bookPart = new org.springframework.mock.web.MockMultipartFile(
                "book", "", "application/json", objectMapper.writeValueAsBytes(partial));

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, "/v1/libraries/1/books/1")
                        .file(bookPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("PATCH multipart: non-admin gets 401")
    void testPatchBook_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("فقط مدیر کتابخانه می‌تواند کتاب‌ها را ویرایش کند"))
                .when(bookService).patchBookWithAssets(eq(1L), eq(1L), isNull(), any(), any(), any(), eq(5));

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, "/v1/libraries/1/books/1")
                        .param("copyCount", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH multipart: unauthenticated gets 401")
    void testPatchBook_unauthenticated() throws Exception {
        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, "/v1/libraries/1/books/1")
                        .param("copyCount", "5"))
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

    @Test
    @DisplayName("Should return 401 when unauthenticated user gets a book")
    void testGetBookUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/books/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── search ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should search books with query and filters")
    void testSearchBooksSuccess() throws Exception {
        Page<BookDTO> bookPage = new PageImpl<>(Arrays.asList(bookDTO), PageRequest.of(0, 10), 1);
        when(bookService.advancedSearchBooks(eq(1L), eq("Clean"), eq(2L), eq(2000), eq(2020), any()))
                .thenReturn(bookPage);

        mockMvc.perform(get("/v1/libraries/1/books/search")
                .param("query", "Clean")
                .param("subjectId", "2")
                .param("yearFrom", "2000")
                .param("yearTo", "2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should search books with no parameters")
    void testSearchBooksNoParams() throws Exception {
        Page<BookDTO> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        when(bookService.advancedSearchBooks(eq(1L), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/v1/libraries/1/books/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user searches books")
    void testSearchBooksUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/books/search"))
                .andExpect(status().isUnauthorized());
    }

    // ── update ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should update book successfully")
    void testUpdateBookSuccess() throws Exception {
        BookDTO updated = BookDTO.builder()
                .id(1L)
                .title("Clean Code 2nd Edition")
                .author("Robert C. Martin")
                .build();
        when(bookService.updateBook(eq(1L), eq(1L), any(BookRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/v1/libraries/1/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Clean Code 2nd Edition"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 400 when updating book with invalid request")
    void testUpdateBookInvalidRequest() throws Exception {
        BookRequest invalidRequest = BookRequest.builder().title("").author("").build();

        mockMvc.perform(put("/v1/libraries/1/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when updating a nonexistent book")
    void testUpdateBookNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found"))
                .when(bookService).updateBook(eq(1L), eq(999L), any(BookRequest.class));

        mockMvc.perform(put("/v1/libraries/1/books/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin updates book")
    void testUpdateBookUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can update books"))
                .when(bookService).updateBook(eq(1L), eq(1L), any(BookRequest.class));

        mockMvc.perform(put("/v1/libraries/1/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user updates book")
    void testUpdateBookUnauthenticated() throws Exception {
        mockMvc.perform(put("/v1/libraries/1/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ── delete ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should delete book successfully")
    void testDeleteBookSuccess() throws Exception {
        mockMvc.perform(delete("/v1/libraries/1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Book deleted successfully"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when deleting a nonexistent book")
    void testDeleteBookNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found"))
                .when(bookService).deleteBook(1L, 999L);

        mockMvc.perform(delete("/v1/libraries/1/books/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin deletes book")
    void testDeleteBookUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can delete books"))
                .when(bookService).deleteBook(1L, 1L);

        mockMvc.perform(delete("/v1/libraries/1/books/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user deletes book")
    void testDeleteBookUnauthenticated() throws Exception {
        mockMvc.perform(delete("/v1/libraries/1/books/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── add copies ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should add book copies successfully")
    void testAddBookCopiesSuccess() throws Exception {
        AddBookCopyRequest request = AddBookCopyRequest.builder().numberOfCopies(5).build();

        mockMvc.perform(post("/v1/libraries/1/books/1/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Book copies added successfully"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 400 when adding a non-positive number of copies")
    void testAddBookCopiesInvalidRequest() throws Exception {
        AddBookCopyRequest request = AddBookCopyRequest.builder().numberOfCopies(-1).build();

        mockMvc.perform(post("/v1/libraries/1/books/1/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when adding copies to a nonexistent book")
    void testAddBookCopiesNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found"))
                .when(bookService).addBookCopies(1L, 999L, 5);
        AddBookCopyRequest request = AddBookCopyRequest.builder().numberOfCopies(5).build();

        mockMvc.perform(post("/v1/libraries/1/books/999/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin adds book copies")
    void testAddBookCopiesUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can add copies"))
                .when(bookService).addBookCopies(1L, 1L, 5);
        AddBookCopyRequest request = AddBookCopyRequest.builder().numberOfCopies(5).build();

        mockMvc.perform(post("/v1/libraries/1/books/1/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user adds book copies")
    void testAddBookCopiesUnauthenticated() throws Exception {
        AddBookCopyRequest request = AddBookCopyRequest.builder().numberOfCopies(5).build();

        mockMvc.perform(post("/v1/libraries/1/books/1/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── set copy count ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should set book copy count successfully")
    void testSetBookCopyCountSuccess() throws Exception {
        BookDTO updated = BookDTO.builder().id(1L).title("Clean Code").build();
        when(bookService.setBookCopyCount(1L, 1L, 10)).thenReturn(updated);

        mockMvc.perform(put("/v1/libraries/1/books/1/copies/count")
                .param("count", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 400 when setting copy count below loaned copies")
    void testSetBookCopyCountBadRequest() throws Exception {
        doThrow(new BadRequestException("Cannot go below copies on loan"))
                .when(bookService).setBookCopyCount(1L, 1L, 0);

        mockMvc.perform(put("/v1/libraries/1/books/1/copies/count")
                .param("count", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when setting copy count for nonexistent book")
    void testSetBookCopyCountNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Book not found"))
                .when(bookService).setBookCopyCount(1L, 999L, 10);

        mockMvc.perform(put("/v1/libraries/1/books/999/copies/count")
                .param("count", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin sets copy count")
    void testSetBookCopyCountUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can set copy count"))
                .when(bookService).setBookCopyCount(1L, 1L, 10);

        mockMvc.perform(put("/v1/libraries/1/books/1/copies/count")
                .param("count", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user sets copy count")
    void testSetBookCopyCountUnauthenticated() throws Exception {
        mockMvc.perform(put("/v1/libraries/1/books/1/copies/count")
                .param("count", "10"))
                .andExpect(status().isUnauthorized());
    }
}
