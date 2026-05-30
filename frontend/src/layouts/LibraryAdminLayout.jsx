import React, { useEffect, useState } from "react";
import { Navigate, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { libraryAPI } from "../services/api";
import { LibraryContext } from "../context/LibraryContext";
import AdminLayout from "./AdminLayout";

const LibraryAdminLayout = () => {
  const { libraryId } = useParams();
  const { isAuthenticated, loading: authLoading } = useAuth();
  const [libraryName, setLibraryName] = useState("");
  const [userRole, setUserRole] = useState(null);
  const [membershipStatus, setMembershipStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) { setLoading(false); return; }
    let cancelled = false;
    (async () => {
      try {
        const [libRes, userLibsRes] = await Promise.all([
          libraryAPI.getLibrary(libraryId),
          libraryAPI.getLibraries(),
        ]);
        const lib = libRes.data?.data || libRes.data;
        const libs = userLibsRes.data?.data || userLibsRes.data || [];
        const myLib = libs.find((l) => String(l.id) === String(libraryId));
        if (!cancelled) {
          setLibraryName(lib?.name || "");
          setUserRole(myLib?.userRole || null);
          setMembershipStatus(myLib?.userStatus || null);
        }
      } catch {
        // ignore – guard will block
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [libraryId, isAuthenticated]);

  const navItems = [
    { to: `/libraries/${libraryId}/admin`,         end: true,  icon: "📊", label: "داشبورد" },
    { to: `/libraries/${libraryId}/admin/books`,   end: false, icon: "📖", label: "مدیریت کتاب‌ها" },
    { to: `/libraries/${libraryId}/admin/members`, end: false, icon: "👥", label: "اعضا و درخواست‌ها" },
    { to: `/libraries/${libraryId}/admin/borrows`, end: false, icon: "📋", label: "امانت‌ها" },
  ];

  if (authLoading || loading) {
    return <div className="loading">در حال بارگذاری...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  if (userRole !== "ADMIN" || membershipStatus !== "APPROVED") {
    return (
      <div style={{ padding: "3rem", textAlign: "center", color: "var(--color-danger)" }}>
        <div style={{ fontSize: "2.5rem", marginBottom: "1rem" }}>🔒</div>
        <p>شما به عنوان مدیر این کتابخانه دسترسی ندارید.</p>
        <a href="/libraries" style={{ color: "var(--color-primary)", fontWeight: 600 }}>
          بازگشت به کتابخانه‌ها
        </a>
      </div>
    );
  }

  return (
    <LibraryContext.Provider
      value={{ libraryId, libraryName, userRole, membershipStatus, loading: false }}
    >
      <AdminLayout
        title="پنل کتابدار"
        subtitle={libraryName}
        navItems={navItems}
        backTo="/libraries"
      />
    </LibraryContext.Provider>
  );
};

export default LibraryAdminLayout;
