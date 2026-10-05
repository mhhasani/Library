package com.library.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sends the browser back to the SPA's login page with a short reason code; details of the
 * failure are only recorded in the audit trail (by the authentication event listener).
 */
@Component
public class OidcLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = exception instanceof OAuth2AuthenticationException oauth
                && UserProvisioningService.ACCOUNT_INACTIVE.equals(oauth.getError().getErrorCode())
                ? "inactive" : "failed";
        response.sendRedirect("/login?error=" + code);
    }
}
