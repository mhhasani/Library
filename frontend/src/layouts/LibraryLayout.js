import React, { useEffect, useState } from "react";
import { NavLink, Outlet, useParams } from "react-router-dom";
import { libraryAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import { LibraryContext } from "../context/LibraryContext";
import "./LibraryLayout.css";

const LibraryLayout = () => {
  const { libraryId } = useParams();
  const { isAuthenticated } = useAuth();
  const [libraryName, setLibraryName] = useState("");
  const [userRole, setUserRole] = useState(null);
  const [membershipStatus, setMembershipStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const requests = [libraryAPI.getLibrary(libraryId)];
        if (isAuthenticated) requests.push(libraryAPI.getLibraries());
        const [libRes, userLibsRes] = await Promise.all(requests);

        const lib = libRes.data?.data || libRes.data;
        const libs = isAuthenticated
          ? userLibsRes?.data?.data || userLibsRes?.data || []
          : [];
        const myLib = libs.find((l) => String(l.id) === String(libraryId));

        if (!cancelled) {
          setLibraryName(lib?.name || "");
          setUserRole(myLib?.userRole || null);
          setMembershipStatus(myLib?.userStatus || null);
        }
      } catch {
        // pages handle their own errors
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [libraryId, isAuthenticated]);

  if (!loading && isAuthenticated && membershipStatus !== "APPROVED") {
    return (
      <LibraryContext.Provider value={{ libraryId, libraryName, userRole, membershipStatus, loading }}>
        <div style={{ padding: "3rem", textAlign: "center", color: "var(--color-text-secondary)" }}>
          <div style={{ fontSize: "2.5rem", marginBottom: "1rem" }}>🔒</div>
          {membershipStatus === "PENDING" ? (
            <p>درخواست عضویت شما در این کتابخانه در انتظار تأیید است.</p>
          ) : membershipStatus === "REJECTED" ? (
            <p>درخواست عضویت شما در این کتابخانه رد شده است.</p>
          ) : (
            <p>شما عضو این کتابخانه نیستید.</p>
          )}
          <a href="/libraries" style={{ color: "var(--color-primary)", fontWeight: 600 }}>
            بازگشت به کتابخانه‌ها
          </a>
        </div>
      </LibraryContext.Provider>
    );
  }

  return (
    <LibraryContext.Provider value={{ libraryId, libraryName, userRole, membershipStatus, loading }}>
      {!loading && (
        <div className="library-subnav">
          <div className="library-subnav-inner">
            <span className="library-subnav-name">🏛️ {libraryName}</span>
            <div className="library-subnav-tabs">
              <NavLink
                to={`/libraries/${libraryId}/books`}
                className={({ isActive }) =>
                  `library-tab ${isActive ? "library-tab--active" : ""}`
                }
              >
                📚 کتاب‌ها
              </NavLink>
              <NavLink
                to={`/libraries/${libraryId}/borrows`}
                className={({ isActive }) =>
                  `library-tab ${isActive ? "library-tab--active" : ""}`
                }
              >
                📋 امانت‌های من
              </NavLink>
              {userRole === "ADMIN" && membershipStatus === "APPROVED" && (
                <NavLink
                  to={`/libraries/${libraryId}/admin`}
                  className={({ isActive }) =>
                    `library-tab library-tab--admin ${isActive ? "library-tab--active" : ""}`
                  }
                >
                  ⚙️ پنل مدیریت
                </NavLink>
              )}
            </div>
          </div>
        </div>
      )}
      <Outlet />
    </LibraryContext.Provider>
  );
};

export default LibraryLayout;
