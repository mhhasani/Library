package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.dto.BookDTO;
import com.library.dto.BorrowDTO;
import com.library.entity.enums.BorrowStatus;
import com.library.entity.enums.BorrowType;
import com.library.service.BorrowService;
import com.library.service.FavoriteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Me Controller Tests")
class MeControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private BorrowService borrowService;
    @MockBean  private FavoriteService favoriteService;

    // ── GET /v1/me/borrows ────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Authenticated user gets own borrows — 200")
    void myBorrows_authenticated_ok() throws Exception {
        BorrowDTO borrow = BorrowDTO.builder().id(1L).borrowType(BorrowType.PHYSICAL)
                .status(BorrowStatus.APPROVED).build();
        Page<BorrowDTO> page = new PageImpl<>(List.of(borrow), PageRequest.of(0, 10), 1);
        when(borrowService.getMyBorrowsPaged(isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/me/borrows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"));
    }

    @Test
    @WithMockUser
    @DisplayName("Filter borrows by PHYSICAL type — 200")
    void myBorrows_filterByType_ok() throws Exception {
        Page<BorrowDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(borrowService.getMyBorrowsPaged(eq(BorrowType.PHYSICAL), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/me/borrows").param("type", "PHYSICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("Unauthenticated request to GET /v1/me/borrows — 401")
    void myBorrows_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/me/borrows")).andExpect(status().isUnauthorized());
    }

    // ── GET /v1/me/favorites/ids ──────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Authenticated user gets favorite IDs — 200")
    void favoriteIds_ok() throws Exception {
        when(favoriteService.getFavoriteBookIds()).thenReturn(List.of(10L, 20L));

        mockMvc.perform(get("/v1/me/favorites/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(10))
                .andExpect(jsonPath("$.data[1]").value(20));
    }

    @Test
    @DisplayName("Unauthenticated request to GET /v1/me/favorites/ids — 401")
    void favoriteIds_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/me/favorites/ids")).andExpect(status().isUnauthorized());
    }

    // ── POST /v1/me/favorites/{bookId} ────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Toggle favorite — favorited = true — 200")
    void toggleFavorite_adds_ok() throws Exception {
        when(favoriteService.toggle(5L)).thenReturn(true);

        mockMvc.perform(post("/v1/me/favorites/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Toggle favorite — favorited = false (removed) — 200")
    void toggleFavorite_removes_ok() throws Exception {
        when(favoriteService.toggle(5L)).thenReturn(false);

        mockMvc.perform(post("/v1/me/favorites/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    @DisplayName("Unauthenticated toggle favorite — 401")
    void toggleFavorite_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(post("/v1/me/favorites/5")).andExpect(status().isUnauthorized());
    }

    // ── GET /v1/me/favorites ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Get paginated favorites — 200")
    void favorites_ok() throws Exception {
        BookDTO book = BookDTO.builder().id(10L).title("کتاب اول").author("نویسنده").build();
        Page<BookDTO> page = new PageImpl<>(List.of(book), PageRequest.of(0, 12), 1);
        when(favoriteService.getFavorites(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/me/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("کتاب اول"));
    }

    @Test
    @DisplayName("Unauthenticated GET /v1/me/favorites — 401")
    void favorites_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/me/favorites")).andExpect(status().isUnauthorized());
    }
}
