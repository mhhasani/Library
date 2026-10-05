package com.library.keycloak.captcha;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.KeycloakSession;

/** Registers {@link CaptchaUsernamePasswordForm} as a selectable browser-flow step. */
public class CaptchaUsernamePasswordFormFactory extends UsernamePasswordFormFactory {

    public static final String PROVIDER_ID = "library-captcha-password-form";

    @Override
    public Authenticator create(KeycloakSession session) {
        return new CaptchaUsernamePasswordForm(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Username Password Form with Captcha";
    }

    @Override
    public String getHelpText() {
        return "Validates username and password after an offline image captcha.";
    }
}
