package com.library.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.library.security.AppUserDetails;

import java.util.Map;
import java.util.Set;

@UtilityClass
public class SecurityUtils {

    /**
     * Mirrors the ROLE_SUPER_ADMIN > ROLE_SYSTEM_ADMIN > ROLE_USER hierarchy declared in
     * SecurityConfig's RoleHierarchy bean. That bean only affects Spring-evaluated
     * {@code @PreAuthorize} expressions; a raw {@code hasRole(role)} check against the
     * authenticated user's actual granted authority does NOT expand higher roles down to
     * lower ones, so it must be done here too or a SUPER_ADMIN fails every manual
     * "hasRole(SYSTEM_ADMIN)" check in the services.
     */
    private static final Map<String, Set<String>> ROLE_HIERARCHY = Map.of(
            "SYSTEM_ADMIN", Set.of("SYSTEM_ADMIN", "SUPER_ADMIN"),
            "USER", Set.of("USER", "SYSTEM_ADMIN", "SUPER_ADMIN")
    );

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails) {
            return ((AppUserDetails) authentication.getPrincipal()).getId();
        }
        return null;
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return null;
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        Set<String> acceptable = ROLE_HIERARCHY.getOrDefault(role, Set.of(role));
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> acceptable.stream().anyMatch(r -> auth.getAuthority().equals("ROLE_" + r)));
    }
}
