package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.service.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Library Controller Tests")
class LibraryControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LibraryService libraryService;

    private LibraryRequest libraryRequest;
    private LibraryDTO libraryDTO;

    @BeforeEach
    void setUp() {
        libraryRequest = LibraryRequest.builder()
                .name("City Library")
                .description("Main city library")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(14)
                .build();

        libraryDTO = LibraryDTO.builder()
                .id(1L)
                .name("City Library")
                .description("Main city library")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(14)
                .isActive(true)
                .build();
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should create library successfully")
    void testCreateLibrarySuccess() throws Exception {
        when(libraryService.createLibrary(any(LibraryRequest.class))).thenReturn(libraryDTO);

        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Library created successfully"))
                .andExpect(jsonPath("$.data.name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 403 when regular user tries to create library")
    void testCreateLibraryForbiddenForRegularUser() throws Exception {
        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should return 400 when library name is blank")
    void testCreateLibraryInvalidRequest() throws Exception {
        LibraryRequest invalidRequest = LibraryRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get library by id successfully")
    void testGetLibrarySuccess() throws Exception {
        when(libraryService.getLibraryById(1L)).thenReturn(libraryDTO);

        mockMvc.perform(get("/v1/libraries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when library not found")
    void testGetLibraryNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Library not found"))
                .when(libraryService).getLibraryById(999L);

        mockMvc.perform(get("/v1/libraries/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get user libraries successfully")
    void testGetUserLibrariesSuccess() throws Exception {
        List<LibraryDTO> libraries = Arrays.asList(libraryDTO);
        when(libraryService.getUserLibraries()).thenReturn(libraries);

        mockMvc.perform(get("/v1/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get user libraries empty list")
    void testGetUserLibrariesEmpty() throws Exception {
        when(libraryService.getUserLibraries()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/v1/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Should get active libraries publicly")
    void testGetActiveLibrariesPublic() throws Exception {
        List<LibraryDTO> libraries = Arrays.asList(libraryDTO);
        when(libraryService.getAllActiveLibraries()).thenReturn(libraries);

        mockMvc.perform(get("/v1/libraries/public/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should update library successfully")
    void testUpdateLibrarySuccess() throws Exception {
        LibraryDTO updatedLibrary = LibraryDTO.builder()
                .id(1L)
                .name("City Library Updated")
                .description("Updated description")
                .build();

        when(libraryService.updateLibrary(1L, libraryRequest)).thenReturn(updatedLibrary);

        mockMvc.perform(put("/v1/libraries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("City Library Updated"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when user not authorized to update")
    void testUpdateLibraryUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can update library"))
                .when(libraryService).updateLibrary(1L, libraryRequest);

        mockMvc.perform(put("/v1/libraries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void testCreateLibraryUnAuthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isUnauthorized());
    }
}
