package com.library.security;

/** Names of the server-side session attributes that carry the login's security state. */
public final class SessionAttributes {

    /** Local user id of the authenticated account. */
    public static final String USER_ID = "library.userId";
    /** Client IP address and user-agent fingerprint the session is bound to. */
    public static final String BOUND_IP = "library.boundIp";
    public static final String BOUND_USER_AGENT = "library.boundUserAgent";
    /** When the user last proved their identity (epoch millis), for sensitive operations. */
    public static final String AUTH_TIME = "library.authTime";
    /** Last user-initiated request (epoch millis); background polling does not count. */
    public static final String LAST_ACTIVITY = "library.lastActivity";
    /** The post-login security notice has been acknowledged. */
    public static final String NOTICE_ACKNOWLEDGED = "library.noticeAcknowledged";
    /** Previous successful login, shown in the security notice. */
    public static final String PREVIOUS_LOGIN_AT = "library.previousLoginAt";
    public static final String PREVIOUS_LOGIN_IP = "library.previousLoginIp";
    /** ID token of the login, used as id_token_hint when logging out of Keycloak. */
    public static final String ID_TOKEN = "library.idToken";
    /** A re-authentication was requested; the next login keeps the acknowledged notice. */
    public static final String REAUTH_PENDING = "library.reauthPending";
    /** Validated in-app path to return to after login. */
    public static final String RETURN_TO = "library.returnTo";

    private SessionAttributes() {
    }
}
