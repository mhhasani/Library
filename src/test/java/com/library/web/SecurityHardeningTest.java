package com.library.web;

import com.library.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Security hardening: paging limits, error responses, headers")
class SecurityHardeningTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Oversized page size is rejected before reaching the controller")
    void oversizedPageSize_rejected() throws Exception {
        mockMvc.perform(get("/v1/books/search").param("query", "x").param("size", "100000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Negative / non-numeric page is rejected")
    void invalidPage_rejected() throws Exception {
        mockMvc.perform(get("/v1/books/search").param("query", "x").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/books/search").param("query", "x").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Page size within the limit is accepted")
    void normalPageSize_accepted() throws Exception {
        mockMvc.perform(get("/v1/books/search").param("query", "x").param("size", "24"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("Malformed JSON body yields a generic 400 without parser details")
    void malformedJson_genericError() throws Exception {
        mockMvc.perform(post("/v1/library-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Jackson"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    @WithMockUser
    @DisplayName("Unknown path returns a JSON 404, never a directory listing")
    void unknownPath_json404() throws Exception {
        mockMvc.perform(get("/v1/does-not-exist/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Security headers are present on API responses")
    void securityHeaders_present() throws Exception {
        mockMvc.perform(get("/v1/stats"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().exists("X-Request-Id"));
    }
}
