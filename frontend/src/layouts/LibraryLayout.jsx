import React, { useEffect, useState } from "react";
import { NavLink, Navigate, Outlet, useParams } from "react-router-dom";
import { libraryAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import { LibraryContext } from "../context/LibraryContext";
import "./LibraryLayout.css";

const LibraryLayout = () => {
  const { libraryId } = useParams();
  const { isAuthenticated, loading: authLoading } = useAuth();
  const [libraryName, setLibraryName] = useState("");
  const [borrowDuration, setBorrowDuration] = useState(14);
  const [userRole, setUserRole] = useState(null);
  const [membershipStatus, setMembershipStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [joining, setJoining] = useState(false);
  const [joinMsg, setJoinMsg] = useState("");

  const handleJoin = async () => {
    try {
      setJoining(true);
      await libraryAPI.requestMembership(libraryId);
      setMembershipStatus("PENDING");
      setJoinMsg("درخواست عضویت شما ثبت شد و در انتظار تأیید است.");
    } catch (err) {
      setJoinMsg(err.response?.data?.message || "خطا در ثبت درخواست عضویت");
    } finally {
      setJoining(false);
    }
  };

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
          setBorrowDuration(lib?.defaultBorrowDurationDays || 14);
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

  if (!authLoading && !isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!loading && isAuthenticated && membershipStatus !== "APPROVED") {
    return (
      <LibraryContext.Provider value={{ libraryId, libraryName, borrowDuration, userRole, membershipStatus, loading }}>
        <div style={{ padding: "3rem", textAlign: "center", color: "var(--color-text-secondary)" }}>
          <div style={{ fontSize: "2.5rem", marginBottom: "1rem" }}>🔒</div>
          {membershipStatus === "PENDING" ? (
            <p>درخواست عضویت شما در این کتابخانه در انتظار تأیید است.</p>
          ) : membershipStatus === "REJECTED" ? (
            <p>درخواست عضویت شما در این کتابخانه رد شده است.</p>
          ) : (
            <>
              <p style={{ marginBottom: "1rem" }}>
                برای مشاهده‌ی کتاب‌ها و امانت در این کتابخانه ابتدا باید عضو شوید.
              </p>
              <button className="btn btn-accent" onClick={handleJoin} disabled={joining}>
                {joining ? "در حال ثبت..." : "➕ درخواست عضویت"}
              </button>
            </>
          )}
          {joinMsg && <p style={{ marginTop: "1rem", color: "var(--color-primary)", fontWeight: 600 }}>{joinMsg}</p>}
          <div style={{ marginTop: "1.25rem" }}>
            <a href="/libraries" style={{ color: "var(--color-primary)", fontWeight: 600 }}>
              بازگشت به کتابخانه‌ها
            </a>
          </div>
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
                📚 فعالیت من
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
