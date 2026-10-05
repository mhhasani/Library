package com.library.keycloak;

import com.library.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Temporary passwords: super admin only")
class TemporaryPasswordTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private IdentityAccountService identityAccountService;

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("A system admin cannot set passwords (could otherwise take over a super admin)")
    void systemAdminForbidden() throws Exception {
        mockMvc.perform(post("/v1/admin/users/2/temporary-password")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Temp#Pass2468x\"}"))
                .andExpect(status().isForbidden());
        verify(identityAccountService, never()).assignTemporaryPassword(anyLong(), anyString());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("The super admin can assign one")
    void superAdminAllowed() throws Exception {
        mockMvc.perform(post("/v1/admin/users/2/temporary-password")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Temp#Pass2468x\"}"))
                .andExpect(status().isOk());
        verify(identityAccountService).assignTemporaryPassword(2L, "Temp#Pass2468x");
    }
}
