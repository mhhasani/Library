package com.library.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Post-login redirect targets (open-redirect protection)")
class LibraryAuthorizationRequestResolverTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/system/users", "/libraries/3/books?page=2"})
    void acceptsInAppPaths(String path) {
        assertThat(LibraryAuthorizationRequestResolver.safeReturnPath(path)).isEqualTo(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "//evil.example/x", "/\\evil.example", "javascript:alert(1)",
            "relative/path", "/x\r\nSet-Cookie: a=b"})
    void rejectsEverythingElse(String path) {
        assertThat(LibraryAuthorizationRequestResolver.safeReturnPath(path)).isNull();
    }
}
