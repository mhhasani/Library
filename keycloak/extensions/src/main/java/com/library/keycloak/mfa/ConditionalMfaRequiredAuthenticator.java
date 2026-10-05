package com.library.keycloak.mfa;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;

/**
 * Condition for the OTP sub-flow: true when the realm requires a second factor for
 * everyone (realm attribute {@value #REALM_ATTRIBUTE}, managed from the application's
 * security settings), or when the user has already configured an OTP device.
 *
 * With MFA required, a user without a device is sent through device registration
 * (CONFIGURE_TOTP) by the OTP step itself.
 */
public class ConditionalMfaRequiredAuthenticator implements ConditionalAuthenticator {

    public static final String REALM_ATTRIBUTE = "libraryMfaRequired";

    static final ConditionalMfaRequiredAuthenticator SINGLETON = new ConditionalMfaRequiredAuthenticator();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        return isMfaRequired(context.getRealm())
                || (user != null && user.credentialManager().isConfiguredFor(OTPCredentialModel.TYPE));
    }

    static boolean isMfaRequired(RealmModel realm) {
        return Boolean.parseBoolean(realm.getAttribute(REALM_ATTRIBUTE));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // A condition has no form to submit
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Nothing to set up for a condition
    }

    @Override
    public void close() {
        // Stateless
    }
}
