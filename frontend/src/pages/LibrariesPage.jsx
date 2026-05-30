import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { libraryAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import "./LibrariesPage.css";

const statusLabel = {
  ACTIVE:    "فعال",
  APPROVED:  "تأیید شده",
  PENDING:   "در انتظار تأیید",
  REJECTED:  "رد شده",
  SUSPENDED: "معلق",
};

const roleLabel = {
  ADMIN:     "مدیر",
  MEMBER:    "عضو",
  PATRON:    "عضو",
  LIBRARIAN: "کتابدار",
};

const LibrariesPage = () => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [publicLibraries, setPublicLibraries] = useState([]);
  const [userLibraries, setUserLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const fetchLibraries = useCallback(async () => {
    try {
      setLoading(true);
      const publicRes = await libraryAPI.getPublicLibraries();
      setPublicLibraries(publicRes.data.data || publicRes.data);
      if (isAuthenticated) {
        const userRes = await libraryAPI.getLibraries();
        setUserLibraries(userRes.data.data || userRes.data);
      }
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری کتابخانه‌ها");
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => { fetchLibraries(); }, [fetchLibraries]);

  const handleRequestMembership = async (libraryId) => {
    try {
      await libraryAPI.requestMembership(libraryId);
      setSuccessMessage("درخواست عضویت با موفقیت ارسال شد");
      setTimeout(() => setSuccessMessage(""), 4000);
      fetchLibraries();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در ارسال درخواست عضویت");
    }
  };

  const getUserLibraryStatus = (libraryId) => {
    const membership = userLibraries.find((lib) => lib.id === libraryId);
    if (!membership) return null;
    return { role: membership.userRole, status: membership.userStatus };
  };

  if (loading) return <div className="loading">در حال بارگذاری کتابخانه‌ها...</div>;

  return (
    <div className="libraries-page">
      <div className="libraries-header">
        <div className="libraries-header-inner">
          <div>
            <h1 className="libraries-main-title">کتابخانه‌ها</h1>
            <p className="libraries-main-sub">عضو کتابخانه‌ها شوید و به منابع دسترسی پیدا کنید</p>
          </div>
        </div>
      </div>

      <div className="libraries-body">
        {error && <div className="error-message">{error}</div>}
        {successMessage && <div className="success-message">{successMessage}</div>}

        {/* Your Libraries */}
        {isAuthenticated && (
          <section className="lib-section">
            <h2 className="lib-section-title">📚 کتابخانه‌های من</h2>
            {userLibraries.length === 0 ? (
              <div className="empty-state">
                <span className="empty-icon">🏛️</span>
                <p>هنوز عضو هیچ کتابخانه‌ای نشده‌اید</p>
              </div>
            ) : (
              <div className="libraries-grid">
                {userLibraries.map((library) => (
                  <div key={library.id} className="library-card">
                    <div className="library-card-icon">🏛️</div>
                    <h3 className="library-card-name">{library.name}</h3>
                    {library.description && (
                      <p className="library-card-desc">{library.description}</p>
                    )}
                    <div className="library-card-meta">
                      <span className="lib-badge lib-badge-role">
                        {roleLabel[library.userRole] || library.userRole}
                      </span>
                      <span className="lib-badge lib-badge-status">
                        {statusLabel[library.userStatus] || library.userStatus}
                      </span>
                    </div>
                    {library.userStatus === "APPROVED" && (
                      <div className="lib-actions-row">
                        <button
                          className="btn btn-primary btn-sm lib-action-btn"
                          onClick={() => navigate(`/libraries/${library.id}/books`)}
                        >
                          باز کردن
                        </button>
                        {library.userRole === "ADMIN" && (
                          <button
                            className="btn btn-outline btn-sm lib-action-btn"
                            onClick={() => navigate(`/libraries/${library.id}/admin`)}
                          >
                            ⚙️ مدیریت
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {/* Public Libraries */}
        <section className="lib-section">
          <h2 className="lib-section-title">🌐 کتابخانه‌های عمومی</h2>
          {publicLibraries.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🔍</span>
              <p>هیچ کتابخانه عمومی یافت نشد</p>
            </div>
          ) : (
            <div className="libraries-grid">
              {publicLibraries.map((library) => {
                const membershipInfo = getUserLibraryStatus(library.id);
                const isPending = membershipInfo?.status === "PENDING";
                const isApproved = membershipInfo?.status === "APPROVED";
                return (
                  <div key={library.id} className="library-card">
                    <div className="library-card-icon">🏛️</div>
                    <h3 className="library-card-name">{library.name}</h3>
                    {library.description && (
                      <p className="library-card-desc">{library.description}</p>
                    )}
                    {library.ownerName && (
                      <div className="library-card-meta">
                        <span className="lib-badge lib-badge-owner">👤 {library.ownerName}</span>
                      </div>
                    )}
                    {isAuthenticated ? (
                      isApproved ? (
                        <button
                          className="btn btn-primary btn-sm lib-action-btn"
                          onClick={() => navigate(`/libraries/${library.id}/books`)}
                        >
                          باز کردن
                        </button>
                      ) : isPending ? (
                        <div className="lib-member-status pending">
                          ⏳ درخواست در انتظار تأیید
                        </div>
                      ) : (
                        <button
                          className="btn btn-outline btn-sm lib-action-btn"
                          onClick={() => handleRequestMembership(library.id)}
                        >
                          درخواست عضویت
                        </button>
                      )
                    ) : (
                      <p className="lib-login-note">برای عضویت وارد شوید</p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default LibrariesPage;
