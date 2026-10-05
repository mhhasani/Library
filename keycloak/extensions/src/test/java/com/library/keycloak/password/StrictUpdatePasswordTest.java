package com.library.keycloak.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrictUpdatePasswordTest {

    @Test
    void countsCharactersNotPresentInTheCurrentPassword() {
        assertEquals(0, StrictUpdatePassword.newCharacterCount("Secret#2024", "Secret#2024"));
        assertEquals(1, StrictUpdatePassword.newCharacterCount("Secret#2024", "Secret#2025"));
        assertEquals(4, StrictUpdatePassword.newCharacterCount("Secret#2024", "Secret#9876"));
        // Reordering the same characters gives nothing new
        assertEquals(0, StrictUpdatePassword.newCharacterCount("abcd", "dcba"));
        // Repeats count only beyond what the old password had
        assertEquals(2, StrictUpdatePassword.newCharacterCount("aab", "aaaab"));
    }
}
