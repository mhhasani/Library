package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.entity.FileResource;
import com.library.exception.UnauthorizedException;
import com.library.repository.FileResourceRepository;
import com.library.service.DigitalBookService;
import com.library.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * FileController serves both public cover images and gated digital-book PDFs from the
 * same /v1/files/{id} endpoint (permitAll at the security-filter level), so the borrow
 * -approval gate has to be enforced in the controller itself via DigitalBookService.
 * assertFileAccess. These tests cover that gate directly, since it's the fix for the
 * bug where digital book PDFs were downloadable by anyone who guessed a fileId.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("File Controller Tests")
class FileControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FileResourceRepository fileResourceRepository;
    @MockBean private StorageService storageService;
    @MockBean private DigitalBookService digitalBookService;

    private FileResource sampleFile() {
        return FileResource.builder()
                .id(1L)
                .originalFilename("cover.jpg")
                .storedFilename("uuid.jpg")
                .filePath("covers/uuid.jpg")
                .fileSizeBytes(1024L)
                .contentType("image/jpeg")
                .checksumSha256("abc123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Anonymous request for a cover image (not gated) — returns 200")
    void serveFile_publicCoverImage_anonymousAllowed() throws Exception {
        when(fileResourceRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleFile()));
        when(storageService.load(anyString())).thenReturn(new ByteArrayResource("img".getBytes()));
        doNothing().when(digitalBookService).assertFileAccess(1L);

        mockMvc.perform(get("/v1/files/1"))
                .andExpect(status().isOk());

        verify(digitalBookService).assertFileAccess(1L);
    }

    @Test
    @DisplayName("Anonymous request for a gated digital-book file — returns 401, file never loaded")
    void serveFile_gatedDigitalBookFile_anonymousDenied() throws Exception {
        when(fileResourceRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleFile()));
        doThrow(new UnauthorizedException("شما دانلود تأییدشده‌ای برای این کتاب ندارید"))
                .when(digitalBookService).assertFileAccess(1L);

        mockMvc.perform(get("/v1/files/1"))
                .andExpect(status().isUnauthorized());

        verify(storageService, never()).load(anyString());
    }

    @Test
    @WithMockUser(username = "member@library.com", roles = "USER")
    @DisplayName("Authenticated member without approved borrow for a gated file — returns 401")
    void serveFile_gatedDigitalBookFile_memberWithoutBorrowDenied() throws Exception {
        when(fileResourceRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleFile()));
        doThrow(new UnauthorizedException("شما دانلود تأییدشده‌ای برای این کتاب ندارید"))
                .when(digitalBookService).assertFileAccess(1L);

        mockMvc.perform(get("/v1/files/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member@library.com", roles = "USER")
    @DisplayName("Authenticated member with approved borrow for a gated file — returns 200")
    void serveFile_gatedDigitalBookFile_memberWithBorrowAllowed() throws Exception {
        when(fileResourceRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleFile()));
        when(storageService.load(anyString())).thenReturn(new ByteArrayResource("pdf".getBytes()));
        doNothing().when(digitalBookService).assertFileAccess(1L);

        mockMvc.perform(get("/v1/files/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Invalid stored content type falls back to application/octet-stream")
    void serveFile_invalidContentType_fallsBackToOctetStream() throws Exception {
        FileResource badContentType = FileResource.builder()
                .id(2L).originalFilename("mystery").storedFilename("uuid.bin")
                .filePath("covers/uuid.bin").fileSizeBytes(10L)
                .contentType("not-a-real-mime-type").checksumSha256("xyz")
                .createdAt(LocalDateTime.now()).build();
        when(fileResourceRepository.findById(2L)).thenReturn(java.util.Optional.of(badContentType));
        when(storageService.load(anyString())).thenReturn(new ByteArrayResource("data".getBytes()));
        doNothing().when(digitalBookService).assertFileAccess(2L);

        mockMvc.perform(get("/v1/files/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"));
    }

    @Test
    @DisplayName("Unknown fileId — returns 404, access check never reached")
    void serveFile_unknownId_returnsNotFound() throws Exception {
        when(fileResourceRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/v1/files/99"))
                .andExpect(status().isNotFound());

        verify(digitalBookService, never()).assertFileAccess(anyLong());
    }
}
