import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const AdminRoute = ({ children }) => {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  if (user?.systemRole !== "SYSTEM_ADMIN") {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        You are not authorized to access this page.
      </div>
    );
  }

  return children;
};

export default AdminRoute;
