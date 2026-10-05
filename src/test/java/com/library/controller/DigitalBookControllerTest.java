package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.dto.DigitalBookDTO;
import com.library.entity.enums.ClassificationLevel;
import com.library.exception.BadRequestException;
import com.library.exception.UnauthorizedException;
import com.library.service.CoverImageService;
import com.library.service.DigitalBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Digital Book Controller Tests")
class DigitalBookControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DigitalBookService digitalBookService;
    @MockBean private CoverImageService coverImageService;

    private DigitalBookDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleDTO = DigitalBookDTO.builder()
                .id(1L).bookId(1L)
                .fileFormat("PDF")
                .originalFilename("book.pdf")
                .fileSizeBytes(2048L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── Cover Image ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Admin can upload cover image — returns 200")
    void uploadCoverImage_success() throws Exception {
        doNothing().when(coverImageService).uploadCoverImage(anyLong(), anyLong(), any());

        MockMultipartFile image = new MockMultipartFile(
                "file", "cover.jpg", MediaType.IMAGE_JPEG_VALUE, "fake image".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1/cover").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cover image uploaded successfully"));
    }

    @Test
    @DisplayName("Unauthenticated request to upload cover image — returns 401")
    void uploadCoverImage_unauthenticated() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "file", "cover.jpg", MediaType.IMAGE_JPEG_VALUE, "fake image".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1/cover").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Non-admin upload cover image — returns 401 from service")
    void uploadCoverImage_nonAdmin_returnsUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can upload cover images"))
                .when(coverImageService).uploadCoverImage(anyLong(), anyLong(), any());

        MockMultipartFile image = new MockMultipartFile(
                "file", "cover.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1/cover").file(image))
                .andExpect(status().isUnauthorized());
    }

    // ── Upload Digital Book ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Admin uploads digital book — returns 201")
    void uploadDigitalBook_success() throws Exception {
        when(digitalBookService.uploadDigitalBook(anyLong(), anyLong(), any(), any()))
                .thenReturn(sampleDTO);

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1/digital")
                        .file(pdf)
                        .param("fileFormat", "PDF"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileFormat").value("PDF"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("Unauthenticated upload digital book — returns 401")
    void uploadDigitalBook_unauthenticated() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1/digital")
                        .file(pdf).param("fileFormat", "PDF"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Non-admin upload digital book — returns 401")
    void uploadDigitalBook_nonAdmin_returnsUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can upload digital books"))
                .when(digitalBookService).uploadDigitalBook(anyLong(), anyLong(), any(), any());

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1/digital")
                        .file(pdf).param("fileFormat", "PDF"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Upload duplicate format — returns 400")
    void uploadDigitalBook_duplicateFormat_returns400() throws Exception {
        doThrow(new BadRequestException("A PDF version already exists for this book."))
                .when(digitalBookService).uploadDigitalBook(anyLong(), anyLong(), any(), any());

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "book.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/v1/libraries/1/books/1/digital")
                        .file(pdf))
                .andExpect(status().isBadRequest());
    }

    // ── List Digital Books ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("List digital books — returns 200 with list")
    void listDigitalBooks_success() throws Exception {
        when(digitalBookService.listDigitalBooks(1L, 1L)).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/v1/libraries/1/books/1/digital"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fileFormat").value("PDF"));
    }

    @Test
    @DisplayName("Unauthenticated list digital books — returns 401")
    void listDigitalBooks_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/books/1/digital"))
                .andExpect(status().isUnauthorized());
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Download digital book — returns 200 with file content")
    void downloadDigitalBook_success() throws Exception {
        when(digitalBookService.downloadDigitalBook(1L))
                .thenReturn(new ByteArrayResource("pdf content".getBytes()));
        when(digitalBookService.getContentType(1L)).thenReturn("application/pdf");
        when(digitalBookService.getOriginalFilename(1L)).thenReturn("book.pdf");
        when(digitalBookService.getClassification(1L)).thenReturn(ClassificationLevel.CONFIDENTIAL);

        mockMvc.perform(get("/v1/libraries/1/books/1/digital/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''book.pdf")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Output-Label", containsString("Classification: CONFIDENTIAL")))
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Download without approved borrow — returns 401")
    void downloadDigitalBook_noApprovedBorrow_returnsUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("You do not have an approved digital borrow for this book"))
                .when(digitalBookService).downloadDigitalBook(1L);

        mockMvc.perform(get("/v1/libraries/1/books/1/digital/1/download"))
                .andExpect(status().isUnauthorized());
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Admin deletes digital book — returns 200")
    void deleteDigitalBook_success() throws Exception {
        doNothing().when(digitalBookService).deleteDigitalBook(1L, 1L, 1L);

        mockMvc.perform(delete("/v1/libraries/1/books/1/digital/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Digital book deleted successfully"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Non-admin delete digital book — returns 401")
    void deleteDigitalBook_nonAdmin_returnsUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can delete digital books"))
                .when(digitalBookService).deleteDigitalBook(1L, 1L, 1L);

        mockMvc.perform(delete("/v1/libraries/1/books/1/digital/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated delete digital book — returns 401")
    void deleteDigitalBook_unauthenticated() throws Exception {
        mockMvc.perform(delete("/v1/libraries/1/books/1/digital/1"))
                .andExpect(status().isUnauthorized());
    }
}
