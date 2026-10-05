package com.library.keycloak.mfa;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class ConditionalMfaRequiredAuthenticatorFactory implements ConditionalAuthenticatorFactory {

    public static final String PROVIDER_ID = "library-condition-mfa-required";

    private static final Requirement[] REQUIREMENT_CHOICES = {Requirement.REQUIRED, Requirement.DISABLED};

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalMfaRequiredAuthenticator.SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Condition - MFA required or configured";
    }

    @Override
    public String getHelpText() {
        return "Matches when the realm attribute '" + ConditionalMfaRequiredAuthenticator.REALM_ATTRIBUTE
                + "' is true or the user already has an OTP device.";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
