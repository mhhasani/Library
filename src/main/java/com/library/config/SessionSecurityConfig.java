package com.library.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.audit.AuditService;
import com.library.repository.UserRepository;
import com.library.security.SessionSecurityFilter;
import com.library.settings.SecuritySettingsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionSecurityConfig {

    @Bean
    public SessionSecurityFilter sessionSecurityFilter(UserRepository userRepository,
                                                       SecuritySettingsService settingsService,
                                                       SessionProperties sessionProperties,
                                                       AuditService auditService,
                                                       ObjectMapper objectMapper) {
        return new SessionSecurityFilter(userRepository, settingsService, sessionProperties, auditService, objectMapper);
    }

    /** The filter runs inside the security chain only, never as a separate servlet filter. */
    @Bean
    public FilterRegistrationBean<SessionSecurityFilter> sessionSecurityFilterRegistration(SessionSecurityFilter filter) {
        FilterRegistrationBean<SessionSecurityFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
