package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.dto.NotificationDTO;
import com.library.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Notification Controller Tests")
class NotificationControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private NotificationService notificationService;

    // ── GET /v1/notifications ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Authenticated user lists notifications — 200")
    void list_authenticated_ok() throws Exception {
        NotificationDTO n = NotificationDTO.builder().id(1L).message("درخواست عضویت تأیید شد").read(false).build();
        when(notificationService.getMyNotifications()).thenReturn(List.of(n));

        mockMvc.perform(get("/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].read").value(false));
    }

    @Test
    @DisplayName("Unauthenticated request to GET /v1/notifications — 401")
    void list_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/notifications")).andExpect(status().isUnauthorized());
    }

    // ── GET /v1/notifications/paged ──────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Paged notifications with default paging — 200")
    void listPaged_defaultParams_ok() throws Exception {
        NotificationDTO n = NotificationDTO.builder().id(1L).message("پیام").read(false).build();
        when(notificationService.getMyNotificationsPaged(PageRequest.of(0, 15)))
                .thenReturn(new PageImpl<>(List.of(n), PageRequest.of(0, 15), 1));

        mockMvc.perform(get("/v1/notifications/paged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Paged notifications with explicit page/size — 200")
    void listPaged_explicitParams_ok() throws Exception {
        when(notificationService.getMyNotificationsPaged(PageRequest.of(2, 5)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/v1/notifications/paged").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Unauthenticated paged notifications — 401")
    void listPaged_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/notifications/paged")).andExpect(status().isUnauthorized());
    }

    // ── GET /v1/notifications/unread-count ───────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Unread count returned — 200")
    void unreadCount_ok() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(3L);

        mockMvc.perform(get("/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    @DisplayName("Unauthenticated unread count — 401")
    void unreadCount_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/notifications/unread-count")).andExpect(status().isUnauthorized());
    }

    // ── POST /v1/notifications/{id}/read ─────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Mark single notification read — 200")
    void markRead_ok() throws Exception {
        doNothing().when(notificationService).markRead(1L);

        mockMvc.perform(post("/v1/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Unauthenticated mark read — 401")
    void markRead_unauthenticated() throws Exception {
        mockMvc.perform(post("/v1/notifications/1/read")).andExpect(status().isUnauthorized());
    }

    // ── POST /v1/notifications/read-all ──────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Mark all notifications read — 200")
    void markAllRead_ok() throws Exception {
        doNothing().when(notificationService).markAllRead();

        mockMvc.perform(post("/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Unauthenticated mark all read — 401")
    void markAllRead_unauthenticated() throws Exception {
        mockMvc.perform(post("/v1/notifications/read-all")).andExpect(status().isUnauthorized());
    }
}
