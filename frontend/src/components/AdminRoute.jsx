import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const AdminRoute = ({ children }) => {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) {
    return <div className="loading">در حال بارگذاری...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  if (!["SYSTEM_ADMIN", "SUPER_ADMIN"].includes(user?.systemRole)) {
    return (
      <div style={{ padding: "3rem", textAlign: "center", color: "var(--color-danger)", fontSize: "1rem" }}>
        شما مجوز دسترسی به این صفحه را ندارید.
      </div>
    );
  }

  return children;
};

export default AdminRoute;
