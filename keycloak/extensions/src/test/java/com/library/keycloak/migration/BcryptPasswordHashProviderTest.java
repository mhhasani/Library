package com.library.keycloak.migration;

import org.junit.jupiter.api.Test;
import org.keycloak.models.credential.PasswordCredentialModel;

import static org.junit.jupiter.api.Assertions.*;

class BcryptPasswordHashProviderTest {

    private final BcryptPasswordHashProvider provider = new BcryptPasswordHashProvider();

    /** Hash of "User1234!" as produced by the application's former Spring BCryptPasswordEncoder. */
    private static final String MIGRATED = "$2a$10$X7pZ9Cqc8gnA5zWGqnQqeOL4tsL.Yr9/4PAlAKxQrLxdvOS.bterK";

    @Test
    void verifiesMigratedHashes() {
        PasswordCredentialModel credential = PasswordCredentialModel.createFromValues("bcrypt", new byte[0], 10, MIGRATED);
        assertTrue(provider.verify("User1234!", credential));
        assertFalse(provider.verify("wrong", credential));
    }

    @Test
    void acceptsTheOtherBcryptPrefixes() {
        for (String prefix : new String[]{"$2b$", "$2y$"}) {
            String hash = prefix + MIGRATED.substring(4);
            PasswordCredentialModel credential = PasswordCredentialModel.createFromValues("bcrypt", new byte[0], 10, hash);
            assertTrue(provider.verify("User1234!", credential), prefix);
        }
    }

    @Test
    void alwaysAsksForRehash() {
        PasswordCredentialModel credential = PasswordCredentialModel.createFromValues("bcrypt", new byte[0], 10, MIGRATED);
        assertFalse(provider.policyCheck(null, credential));
    }

    @Test
    void malformedHashNeverVerifies() {
        PasswordCredentialModel credential = PasswordCredentialModel.createFromValues("bcrypt", new byte[0], 10, "not-a-hash");
        assertFalse(provider.verify("anything", credential));
    }
}
