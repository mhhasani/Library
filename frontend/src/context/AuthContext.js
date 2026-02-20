import React, { createContext, useState, useContext, useEffect } from "react";
import { authAPI } from "../services/api";

const AuthContext = createContext();

// Helper function to extract error message
const getErrorMessage = (err) => {
  // Check for validation errors in the error field
  if (err.response?.data?.error) {
    return err.response.data.error;
  }
  // Fall back to message field
  if (err.response?.data?.message) {
    return err.response.data.message;
  }
  // Default error
  return "An error occurred";
};

const decodeJwtPayload = (token) => {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(
      base64.length + ((4 - (base64.length % 4)) % 4),
      "=",
    );
    return JSON.parse(atob(padded));
  } catch (err) {
    return null;
  }
};

const getStoredUser = () => {
  const raw = localStorage.getItem("user");
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (err) {
    return null;
  }
};

const buildUserFromAuth = (authData, token) => {
  const payload = token ? decodeJwtPayload(token) : null;
  return {
    id: authData?.userId || payload?.userId || null,
    email: authData?.email || payload?.sub || null,
    systemRole: authData?.systemRole || payload?.systemRole || null,
  };
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Check if user is logged in on mount
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (token) {
      const storedUser = getStoredUser();
      if (storedUser) {
        setUser(storedUser);
      } else {
        setUser(buildUserFromAuth(null, token));
      }
    }
    setLoading(false);
  }, []);

  const login = async (credentials) => {
    try {
      setLoading(true);
      const response = await authAPI.login(credentials);
      const authData = response.data?.data || response.data;
      const accessToken = authData?.accessToken || authData?.token;

      if (!accessToken) {
        throw new Error("No access token received from server");
      }

      localStorage.setItem("token", accessToken);
      if (authData?.refreshToken) {
        localStorage.setItem("refreshToken", authData.refreshToken);
      }

      const newUser = buildUserFromAuth(authData, accessToken);
      localStorage.setItem("user", JSON.stringify(newUser));
      setUser(newUser);
      setError(null);
      return { success: true };
    } catch (err) {
      const errorMsg = getErrorMessage(err);
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    try {
      setLoading(true);
      await authAPI.register(userData);
      return await login({
        email: userData.email,
        password: userData.password,
      });
    } catch (err) {
      const errorMsg = getErrorMessage(err);
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setUser(null);
    setError(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        error,
        login,
        register,
        logout,
        isAuthenticated: !!user,
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
