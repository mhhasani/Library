package com.library.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.UpdateSecuritySettingsRequest;
import com.library.entity.enums.SensitiveOperation;
import com.library.keycloak.KeycloakRealmSync;
import com.library.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Security settings: ranges validated by the API and by the database")
class SecuritySettingsTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuditLogRepository auditLogs;
    @MockBean private KeycloakRealmSync realmSync;

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Defaults satisfy the baseline (lockout after 5, idle 30 min, history 3, MFA on)")
    void defaults() throws Exception {
        mockMvc.perform(get("/v1/admin/security-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxFailedLogins").value(5))
                .andExpect(jsonPath("$.data.sessionIdleMinutes").value(30))
                .andExpect(jsonPath("$.data.passwordHistory").value(3))
                .andExpect(jsonPath("$.data.mfaRequired").value(true));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Only the super admin may change them")
    void systemAdminCannotChange() throws Exception {
        mockMvc.perform(put("/v1/admin/security-settings").contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(5, 30, 3))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Out-of-range values are rejected (failed logins 1..6, idle 15..30 min, history 1..3)")
    void outOfRangeRejected() throws Exception {
        for (UpdateSecuritySettingsRequest bad : new UpdateSecuritySettingsRequest[]{
                request(7, 30, 3), request(0, 30, 3), request(5, 10, 3), request(5, 31, 3), request(5, 30, 4)}) {
            mockMvc.perform(put("/v1/admin/security-settings").contentType(MediaType.APPLICATION_JSON)
                            .content(json(bad)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("A valid change is stored, pushed to the identity provider and audited")
    void validChangeApplied() throws Exception {
        long auditBefore = auditLogs.count();
        mockMvc.perform(put("/v1/admin/security-settings").contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(3, 20, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxFailedLogins").value(3))
                .andExpect(jsonPath("$.data.sessionIdleMinutes").value(20));
        assertThat(auditLogs.count()).isGreaterThan(auditBefore);
        // restore defaults for other tests
        mockMvc.perform(put("/v1/admin/security-settings").contentType(MediaType.APPLICATION_JSON)
                .content(json(request(5, 30, 3))));
    }

    @Test
    @DisplayName("The database rejects out-of-range values written directly (e.g. 100 failed logins)")
    void databaseEnforcesRanges() {
        assertThatThrownBy(() -> jdbc.update("UPDATE security_settings SET max_failed_logins = 100 WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE security_settings SET session_idle_minutes = 120 WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO security_settings (id, sensitive_operations) VALUES (2, '')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static UpdateSecuritySettingsRequest request(int failedLogins, int idle, int history) {
        return new UpdateSecuritySettingsRequest(failedLogins, 15, 720, idle, history, 90, true, 5,
                Set.of(SensitiveOperation.USER_ROLE_CHANGE, SensitiveOperation.SECURITY_SETTINGS_CHANGE));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
