package com.library.config;

import com.library.audit.AuditingSecurityHandlers;
import com.library.security.AppOidcUserService;
import com.library.security.CsrfCookieFilter;
import com.library.security.LibraryAuthorizationRequestResolver;
import com.library.security.OidcLoginFailureHandler;
import com.library.security.OidcLoginSuccessHandler;
import com.library.security.OidcLogoutHandler;
import com.library.security.SessionSecurityFilter;
import com.library.security.SpaCsrfTokenRequestHandler;
import com.library.web.MdcUserFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Authentication is delegated to Keycloak (OpenID Connect, authorization code + PKCE). The
 * application is the OIDC client: tokens stay on the server and the browser only holds an
 * encrypted, HttpOnly session cookie. Authorization (roles, library membership,
 * classification) remains in the application.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    /**
     * The SPA is served from the same origin as the API (nginx proxies /api), so no cross-origin
     * access is needed in production. Extra origins (e.g. a dev server) must be listed explicitly.
     */
    @Value("${app.security.cors-allowed-origins:}")
    private List<String> corsAllowedOrigins;

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy(
            "ROLE_SUPER_ADMIN > ROLE_SYSTEM_ADMIN\n" +
            "ROLE_SYSTEM_ADMIN > ROLE_USER"
        );
        return hierarchy;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy());
        return handler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins.stream().filter(o -> !o.isBlank()).toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "X-XSRF-TOKEN", "X-Requested-With",
                SessionSecurityFilter.BACKGROUND_HEADER));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository registrations,
                                           AppOidcUserService oidcUserService,
                                           OidcLoginSuccessHandler loginSuccessHandler,
                                           OidcLoginFailureHandler loginFailureHandler,
                                           OidcLogoutHandler logoutHandler,
                                           SessionSecurityFilter sessionSecurityFilter,
                                           SessionProperties sessionProperties,
                                           AuditingSecurityHandlers auditingSecurityHandlers) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .path("/")
                .secure(sessionProperties.cookieSecure())
                .sameSite(sessionProperties.cookieSameSite()));

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .requireCsrfProtectionMatcher(cookieAuthenticatedWrite(sessionProperties.cookieName())))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.changeSessionId()))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'; form-action 'self'"))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()"))
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(auditingSecurityHandlers)
                .accessDeniedHandler(auditingSecurityHandlers)
            )
            .oauth2Login(login -> login
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(new LibraryAuthorizationRequestResolver(registrations)))
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler))
            .logout(logout -> logout
                .logoutUrl("/v1/auth/logout")
                .addLogoutHandler(logoutHandler)
                .logoutSuccessHandler(logoutHandler)
                .invalidateHttpSession(true)
                .deleteCookies(sessionProperties.cookieName()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v1/auth/**").permitAll()
                .requestMatchers("/v1/libraries/public/**").permitAll()
                .requestMatchers("/v1/books/search").permitAll()
                .requestMatchers("/v1/stats").permitAll()
                .requestMatchers("/v1/files/**").permitAll()
                // Only reachable when SWAGGER_ENABLED=true (springdoc is off by default)
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .addFilterAfter(sessionSecurityFilter, SecurityContextHolderFilter.class)
            .addFilterBefore(new MdcUserFilter(), AuthorizationFilter.class);

        return http.build();
    }

    /**
     * CSRF applies to state-changing requests that carry the session cookie, i.e. exactly the
     * requests a forged cross-site form or script could make with the user's ambient credentials.
     */
    private static RequestMatcher cookieAuthenticatedWrite(String sessionCookieName) {
        return (HttpServletRequest request) -> {
            if (SAFE_METHODS.contains(request.getMethod())) {
                return false;
            }
            Cookie[] cookies = request.getCookies();
            return cookies != null && Arrays.stream(cookies).anyMatch(c -> sessionCookieName.equals(c.getName()));
        };
    }
}
