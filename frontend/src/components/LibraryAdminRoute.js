import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const LibraryAdminRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <div className="loading">در حال بارگذاری...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  const activeLibraryId = localStorage.getItem("activeLibraryId");
  const activeLibraryRole = localStorage.getItem("activeLibraryRole");
  const activeLibraryStatus = localStorage.getItem("activeLibraryStatus");

  if (
    !activeLibraryId ||
    activeLibraryRole !== "ADMIN" ||
    activeLibraryStatus !== "APPROVED"
  ) {
    return (
      <div
        style={{
          padding: "3rem",
          textAlign: "center",
          color: "var(--color-danger)",
          fontSize: "1rem",
          lineHeight: "2",
        }}
      >
        <div style={{ fontSize: "2.5rem", marginBottom: "1rem" }}>🔒</div>
        <p>برای دسترسی به پنل کتابدار، ابتدا یک کتابخانه را که در آن نقش <strong>مدیر</strong> دارید انتخاب کنید.</p>
        <a href="/libraries" style={{ color: "var(--color-primary)", fontWeight: 600 }}>
          رفتن به صفحه کتابخانه‌ها
        </a>
      </div>
    );
  }

  return children;
};

export default LibraryAdminRoute;
