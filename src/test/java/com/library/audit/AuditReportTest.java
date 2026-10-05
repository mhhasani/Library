package com.library.audit;

import com.library.BaseIntegrationTest;
import com.library.entity.enums.AuditAction;
import com.library.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Audit reports: access control, filtering, labeled export, login auditing")
class AuditReportTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditService auditService;
    @Autowired private AuditLogRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        auditService.record(AuditEntry.failure(AuditAction.LOGIN).actorEmail("intruder@x.ir").build());
        auditService.record(AuditEntry.success(AuditAction.LOGIN).actorEmail("=HYPERLINK(\"evil\")").build());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular users cannot read the audit trail")
    void regularUserForbidden() throws Exception {
        mockMvc.perform(get("/v1/admin/audit-logs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admins can filter by outcome")
    void adminFiltersByOutcome() throws Exception {
        mockMvc.perform(get("/v1/admin/audit-logs").param("outcome", "FAILURE").param("action", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].actorEmail").value("intruder@x.ir"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Export carries the classification label and neutralises spreadsheet formulas")
    void exportIsLabeledAndSafe() throws Exception {
        byte[] body = mockMvc.perform(get("/v1/admin/audit-logs/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        String csv = new String(body, StandardCharsets.UTF_8);

        assertThat(csv.lines().findFirst().orElseThrow()).contains("طبقه‌بندی").contains("IP").contains("زمان");
        assertThat(csv).contains("\"'=HYPERLINK");
        assertThat(csv).doesNotContain(",\"=HYPERLINK");
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Chain verification endpoint reports a valid chain")
    void verifyEndpoint() throws Exception {
        mockMvc.perform(post("/v1/admin/audit-logs/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @DisplayName("A failed login attempt is recorded with the presented identity")
    void failedLoginIsAudited() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@x.ir\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(repository.findByAction(AuditAction.LOGIN.name()))
                .anyMatch(r -> "nobody@x.ir".equals(r.getActorEmail()) && r.getOutcome().name().equals("FAILURE"));
    }

    @Test
    @DisplayName("Anonymous access to a protected endpoint is recorded")
    void unauthenticatedAccessIsAudited() throws Exception {
        mockMvc.perform(get("/v1/admin/users")).andExpect(status().isUnauthorized());

        assertThat(repository.findByAction(AuditAction.AUTHENTICATION_REQUIRED.name()))
                .anyMatch(r -> r.getDetails().contains("/v1/admin/users"));
    }
}
