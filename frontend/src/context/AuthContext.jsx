import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { authAPI, loginUrl } from "../services/api";

const AuthContext = createContext();

/** User-activity events that keep the screen unlocked. */
const ACTIVITY_EVENTS = ["mousedown", "keydown", "touchstart", "scroll"];
const IDLE_CHECK_MS = 30_000;

/**
 * Authentication state comes only from the server session (GET /v1/auth/session); nothing
 * about the user is persisted in the browser. Login and registration happen at Keycloak.
 */
export const AuthProvider = ({ children }) => {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const lastActivity = useRef(Date.now());

  const refreshSession = useCallback(async () => {
    try {
      const res = await authAPI.session();
      const data = res.data?.data;
      setSession(data?.authenticated ? data : null);
      return data;
    } catch {
      setSession(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshSession();
  }, [refreshSession]);

  // The server ended the session (idle timeout, other login, revoked account): leave the
  // protected screens immediately so no data stays visible.
  useEffect(() => {
    const onEnded = (e) => {
      setSession(null);
      const reason = e.detail === "SESSION_INVALID" ? "invalid" : "expired";
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = `/login?session=${reason}`;
      }
    };
    const onNoticeRequired = () => refreshSession();
    window.addEventListener("session-ended", onEnded);
    window.addEventListener("security-notice-required", onNoticeRequired);
    return () => {
      window.removeEventListener("session-ended", onEnded);
      window.removeEventListener("security-notice-required", onNoticeRequired);
    };
  }, [refreshSession]);

  // Client-side idle lock mirroring the server's idle timeout (the server stays authoritative).
  useEffect(() => {
    if (!session?.idleTimeoutMinutes) return undefined;
    const touch = () => { lastActivity.current = Date.now(); };
    ACTIVITY_EVENTS.forEach((evt) => window.addEventListener(evt, touch, { passive: true }));
    const timer = setInterval(() => {
      if (Date.now() - lastActivity.current > session.idleTimeoutMinutes * 60_000) {
        window.dispatchEvent(new CustomEvent("session-ended", { detail: "SESSION_EXPIRED" }));
      }
    }, IDLE_CHECK_MS);
    return () => {
      ACTIVITY_EVENTS.forEach((evt) => window.removeEventListener(evt, touch));
      clearInterval(timer);
    };
  }, [session?.idleTimeoutMinutes]);

  const login = useCallback((returnTo) => {
    window.location.href = loginUrl({ returnTo });
  }, []);

  const register = useCallback(() => {
    window.location.href = loginUrl({ register: true });
  }, []);

  /** Ends the application session, then the identity-provider session. */
  const logout = useCallback(async () => {
    let target = "/";
    try {
      const res = await authAPI.logout();
      target = res.data?.data?.logoutUrl || "/";
    } catch {
      /* session already gone */
    }
    setSession(null);
    window.location.href = target;
  }, []);

  const acknowledgeNotice = useCallback(async () => {
    await authAPI.acknowledgeNotice();
    await refreshSession();
  }, [refreshSession]);

  const user = session?.user || null;

  return (
    <AuthContext.Provider
      value={{
        user,
        session,
        loading,
        isAuthenticated: !!user,
        noticeRequired: !!user && !session?.noticeAcknowledged,
        login,
        register,
        logout,
        acknowledgeNotice,
        refreshSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
};
