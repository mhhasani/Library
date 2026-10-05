package com.library.security;

import com.library.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Turns a validated ID token into the session principal. The ID token already carries the
 * needed claims, so the userinfo endpoint is not called.
 */
@Service
public class AppOidcUserService extends OidcUserService {

    private final UserProvisioningService provisioning;

    public AppOidcUserService(UserProvisioningService provisioning) {
        this.provisioning = provisioning;
        setAccessibleScopes(Set.of());
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser identity = super.loadUser(request);
        User user = provisioning.resolve(identity);
        return new AppOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getSystemRole().name())),
                identity.getIdToken(), identity.getUserInfo(), user.getId());
    }
}
