package com.library.keycloak.captcha;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;

/**
 * The standard username/password form with an offline image captcha.
 *
 * The expected answer is kept server-side in the authentication session (never sent to
 * the browser) and is single-use: every rendering of the form draws a new captcha, and a
 * submitted answer is consumed whether it was right or wrong. A wrong answer is recorded
 * as a failed login event and the password is not even checked.
 */
public class CaptchaUsernamePasswordForm extends UsernamePasswordForm {

    static final String FORM_FIELD = "captcha";
    static final String SESSION_NOTE = "library.captcha.answer";
    static final String TEMPLATE_ATTRIBUTE = "captchaImage";
    static final String INVALID_CAPTCHA_MESSAGE = "libraryInvalidCaptcha";

    private AuthenticationFlowContext flowContext;

    public CaptchaUsernamePasswordForm(KeycloakSession session) {
        super(session);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        this.flowContext = context;
        super.authenticate(context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        this.flowContext = context;
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            super.action(context);
            return;
        }

        String expected = context.getAuthenticationSession().getAuthNote(SESSION_NOTE);
        context.getAuthenticationSession().removeAuthNote(SESSION_NOTE);
        if (!CaptchaImage.matches(expected, formData.getFirst(FORM_FIELD))) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.form().setFormData(formData);
            Response challenge = challenge(context, INVALID_CAPTCHA_MESSAGE, FORM_FIELD);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }
        super.action(context);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        attachCaptcha(context, context.form());
        return super.challenge(context, formData);
    }

    @Override
    protected Response createLoginForm(LoginFormsProvider form) {
        attachCaptcha(flowContext, form);
        return super.createLoginForm(form);
    }

    private static void attachCaptcha(AuthenticationFlowContext context, LoginFormsProvider form) {
        CaptchaImage captcha = CaptchaImage.generate();
        context.getAuthenticationSession().setAuthNote(SESSION_NOTE, captcha.answer());
        form.setAttribute(TEMPLATE_ATTRIBUTE, captcha.dataUri());
    }
}
