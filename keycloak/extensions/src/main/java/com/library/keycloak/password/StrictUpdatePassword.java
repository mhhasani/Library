package com.library.keycloak.password;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.requiredactions.UpdatePassword;
import org.keycloak.events.Errors;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces the built-in UPDATE_PASSWORD required action (same id, higher order) so every
 * password change — first login with an admin-assigned password, expiry, or a user's own
 * request — additionally requires:
 * <ol>
 *   <li>the current password, and</li>
 *   <li>at least {@value #MIN_NEW_CHARACTERS} characters in the new password that were not
 *       in the current one.</li>
 * </ol>
 * Length, complexity, history and expiry are enforced by the realm password policy.
 */
public class StrictUpdatePassword extends UpdatePassword {

    public static final int MIN_NEW_CHARACTERS = 4;

    static final String CURRENT_FIELD = "password-current";
    static final String NEW_FIELD = "password-new";
    /** Field the stock template uses for password-policy errors on the new password. */
    static final String NEW_PASSWORD_ERROR_FIELD = "password";
    static final String INVALID_CURRENT_MESSAGE = "libraryInvalidCurrentPassword";
    static final String TOO_FEW_NEW_CHARS_MESSAGE = "libraryTooFewNewCharacters";

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> form = context.getHttpRequest().getDecodedFormParameters();
        if (form.containsKey("cancel-aia")) {
            super.processAction(context);
            return;
        }

        String current = form.getFirst(CURRENT_FIELD);
        String next = form.getFirst(NEW_FIELD);
        UserModel user = context.getUser();

        if (current == null || current.isEmpty()
                || !user.credentialManager().isValid(UserCredentialModel.password(current))) {
            reject(context, CURRENT_FIELD, INVALID_CURRENT_MESSAGE);
            return;
        }
        if (next != null && newCharacterCount(current, next) < MIN_NEW_CHARACTERS) {
            reject(context, NEW_PASSWORD_ERROR_FIELD, TOO_FEW_NEW_CHARS_MESSAGE);
            return;
        }
        super.processAction(context);
    }

    /** Characters of {@code next} not covered by the characters of {@code current} (as multisets). */
    static int newCharacterCount(String current, String next) {
        Map<Integer, Integer> available = new HashMap<>();
        current.codePoints().forEach(c -> available.merge(c, 1, Integer::sum));
        int fresh = 0;
        for (int c : next.codePoints().toArray()) {
            int left = available.getOrDefault(c, 0);
            if (left > 0) {
                available.put(c, left - 1);
            } else {
                fresh++;
            }
        }
        return fresh;
    }

    private static void reject(RequiredActionContext context, String field, String message) {
        context.getEvent().error(Errors.PASSWORD_REJECTED);
        Response challenge = context.form()
                .addError(new FormMessage(field, message))
                .createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
        context.challenge(challenge);
    }

    /** Wins over the built-in provider registered under the same id. */
    @Override
    public int order() {
        return 100;
    }
}
