package com.library.keycloak.migration;

import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Verifies passwords migrated from the application's former local accounts, which were
 * stored as bcrypt hashes. Only used to read those imported credentials: policyCheck()
 * always reports them as outdated, so Keycloak re-hashes the password with the realm's
 * default algorithm on the first successful login.
 */
public class BcryptPasswordHashProvider implements PasswordHashProvider {

    private static final int COST = 12;

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        return false;
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
        return PasswordCredentialModel.createFromValues(BcryptPasswordHashProviderFactory.ID, new byte[0], COST, hash);
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        String hash = credential.getPasswordSecretData().getValue();
        try {
            return hash != null && BCrypt.checkpw(rawPassword, normalize(hash));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** jBCrypt understands the $2a$ prefix; $2b$/$2y$ hashes are computed identically. */
    static String normalize(String hash) {
        return hash.startsWith("$2b$") || hash.startsWith("$2y$") ? "$2a$" + hash.substring(4) : hash;
    }

    @Override
    public void close() {
    }
}
