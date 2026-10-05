package com.library.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.io.Serial;
import java.util.Collection;

/** OIDC principal stored in the session, linked to the local user row. */
public class AppOidcUser extends DefaultOidcUser {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;

    public AppOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
                       OidcUserInfo userInfo, Long userId) {
        super(authorities, idToken, userInfo, "email");
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
