package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.Map;

/**
 * The Keycloak client registration, built explicitly instead of by discovery: browsers are
 * sent to the public issuer URL, while the backend talks to Keycloak over the internal
 * network (token exchange and signing keys). ID tokens must still carry the public issuer.
 */
@Configuration
public class OidcClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OidcProperties oidc) {
        String publicRealm = oidc.issuerUri();
        String internalRealm = oidc.backchannelIssuerUri();
        ClientRegistration keycloak = ClientRegistration.withRegistrationId(OidcProperties.REGISTRATION_ID)
                .clientId(oidc.clientId())
                .clientSecret(oidc.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(oidc.publicUrl() + "/api/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(publicRealm + "/protocol/openid-connect/auth")
                .tokenUri(internalRealm + "/protocol/openid-connect/token")
                .jwkSetUri(internalRealm + "/protocol/openid-connect/certs")
                .userInfoUri(internalRealm + "/protocol/openid-connect/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .issuerUri(publicRealm)
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint", publicRealm + "/protocol/openid-connect/logout"))
                .clientName("Keycloak")
                .build();
        return new InMemoryClientRegistrationRepository(keycloak);
    }
}
