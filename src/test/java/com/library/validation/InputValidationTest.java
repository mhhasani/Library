package com.library.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.BookRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Central input validation and sanitization")
class InputValidationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "click javascript:alert(1)",
            "text with \u0007 bell"
    })
    @DisplayName("SafeText rejects markup, script patterns and control characters")
    void safeTextRejects(String value) {
        assertThat(SafeTextValidator.isSafe(value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "کلیدر — محمود دولت‌آبادی",
            "C++ & Java: a < b, b > c",
            "خط اول\nخط دوم\tبا تب",
            "O'Reilly \"Head First\" (2nd ed.)"
    })
    @DisplayName("SafeText accepts ordinary Persian/English text")
    void safeTextAccepts(String value) {
        assertThat(SafeTextValidator.isSafe(value)).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("A book with a script in its title is rejected before reaching the service")
    void storedXssRejected() throws Exception {
        BookRequest request = BookRequest.builder().title("<script>alert(1)</script>").author("A").build();
        mockMvc.perform(post("/v1/libraries/1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Control characters in JSON strings are stripped centrally")
    void controlCharactersStripped() throws Exception {
        BookRequest parsed = objectMapper.readValue(
                "{\"title\":\"Clean\\u0000Code\\u001b\",\"author\":\"A\"}", BookRequest.class);
        assertThat(parsed.getTitle()).isEqualTo("CleanCode");
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Missing or unknown enum values in admin requests are a 400, not a 500")
    void typedAdminBodies() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/5/role")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/v1/admin/users/5/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HACKED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Query parameters with markup or excessive length are rejected centrally")
    void queryParametersGuarded() throws Exception {
        mockMvc.perform(get("/v1/books/search").param("query", "<script>x</script>"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/books/search").param("query", "a".repeat(2001)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/books/search").param("query", "کتاب C++"))
                .andExpect(status().isOk());
    }
}
