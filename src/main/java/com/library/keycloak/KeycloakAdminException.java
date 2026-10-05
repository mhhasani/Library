package com.library.keycloak;

/** A call to the Keycloak admin API failed (unreachable, rejected, or invalid input). */
public class KeycloakAdminException extends RuntimeException {

    private final int status;

    public KeycloakAdminException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /** HTTP status returned by Keycloak, or 0 when it could not be reached. */
    public int getStatus() {
        return status;
    }
}
