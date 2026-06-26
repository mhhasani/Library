package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.LibrarySubjectDTO;
import com.library.exception.UnauthorizedException;
import com.library.service.LibrarySubjectService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Library Subject Controller Tests")
class LibrarySubjectControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private LibrarySubjectService subjectService;

    // ── GET /v1/libraries/{id}/subjects ───────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Member lists subjects — 200")
    void getSubjects_ok() throws Exception {
        LibrarySubjectDTO s = LibrarySubjectDTO.builder().id(1L).libraryId(1L).name("رمان").bookCount(5L).build();
        when(subjectService.getSubjects(1L)).thenReturn(List.of(s));

        mockMvc.perform(get("/v1/libraries/1/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("رمان"));
    }

    @Test
    @DisplayName("Unauthenticated GET subjects — 401")
    void getSubjects_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/subjects")).andExpect(status().isUnauthorized());
    }

    // ── POST /v1/libraries/{id}/subjects ──────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Admin creates subject — 201")
    void createSubject_admin_created() throws Exception {
        LibrarySubjectDTO created = LibrarySubjectDTO.builder().id(2L).libraryId(1L).name("علمی").bookCount(0L).build();
        when(subjectService.createSubject(1L, "علمی")).thenReturn(created);

        mockMvc.perform(post("/v1/libraries/1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "علمی"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("علمی"));
    }

    @Test
    @WithMockUser
    @DisplayName("Non-admin create subject throws 401")
    void createSubject_nonAdmin_unauthorized() throws Exception {
        when(subjectService.createSubject(anyLong(), any()))
                .thenThrow(new UnauthorizedException("فقط مدیر کتابخانه"));

        mockMvc.perform(post("/v1/libraries/1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "علمی"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated POST subjects — 401")
    void createSubject_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "علمی"))))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /v1/libraries/{id}/subjects/{subjectId} ────────────────────

    @Test
    @WithMockUser
    @DisplayName("Admin deletes subject — 200")
    void deleteSubject_admin_ok() throws Exception {
        doNothing().when(subjectService).deleteSubject(1L, 2L);

        mockMvc.perform(delete("/v1/libraries/1/subjects/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Non-admin delete subject throws 401")
    void deleteSubject_nonAdmin_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("فقط مدیر کتابخانه"))
                .when(subjectService).deleteSubject(1L, 2L);

        mockMvc.perform(delete("/v1/libraries/1/subjects/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated DELETE subject — 401")
    void deleteSubject_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(delete("/v1/libraries/1/subjects/2")).andExpect(status().isUnauthorized());
    }
}
