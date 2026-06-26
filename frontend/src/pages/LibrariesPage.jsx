import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { libraryAPI, libraryRequestAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import GlobalSearchBar from "../components/GlobalSearchBar";
import "../components/BorrowModal.css";
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
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const [publicLibraries, setPublicLibraries] = useState([]);
  const [userLibraries, setUserLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Library creation request
  const [showReqModal, setShowReqModal] = useState(false);
  const [reqForm, setReqForm] = useState({ name: "", description: "", defaultBorrowDurationDays: 14 });
  const [reqSaving, setReqSaving] = useState(false);

  const submitLibraryRequest = async () => {
    if (!reqForm.name.trim()) { setError("نام کتابخانه را وارد کنید"); return; }
    try {
      setReqSaving(true);
      await libraryRequestAPI.submit({
        name: reqForm.name.trim(),
        description: reqForm.description.trim(),
        defaultBorrowDurationDays: Number(reqForm.defaultBorrowDurationDays) || 14,
      });
      setShowReqModal(false);
      setReqForm({ name: "", description: "", defaultBorrowDurationDays: 14 });
      setSuccessMessage("درخواست ایجاد کتابخانه ثبت شد و پس از بررسی مدیر سیستم نتیجه اعلام می‌شود.");
      setTimeout(() => setSuccessMessage(""), 5000);
    } catch (err) {
      setError(err.response?.data?.message || "خطا در ثبت درخواست");
    } finally {
      setReqSaving(false);
    }
  };

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
          {isAuthenticated && !["SYSTEM_ADMIN", "SUPER_ADMIN"].includes(user?.systemRole) && (
            <button className="btn btn-accent" style={{ marginInlineStart: "auto" }} onClick={() => setShowReqModal(true)}>
              ➕ درخواست ایجاد کتابخانه
            </button>
          )}
        </div>
      </div>

      <div className="libraries-body">
        {error && <div className="error-message">{error}</div>}
        {successMessage && <div className="success-message">{successMessage}</div>}

        <div className="libraries-search">
          <GlobalSearchBar />
        </div>

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

      {showReqModal && (
        <div className="modal-overlay" onClick={() => setShowReqModal(false)}>
          <div className="modal-content" style={{ maxWidth: 480 }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>درخواست ایجاد کتابخانه</h2>
              <button className="modal-close-btn" onClick={() => setShowReqModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: "0.85rem", color: "#6b7280", marginBottom: "1rem" }}>
                این درخواست برای مدیر سیستم ارسال می‌شود. در صورت تأیید، کتابخانه با مالکیت شما ایجاد خواهد شد.
              </p>
              <div className="bm-form">
                <div className="bm-field">
                  <label>نام کتابخانه <span className="bm-req">*</span></label>
                  <input value={reqForm.name} onChange={(e) => setReqForm((f) => ({ ...f, name: e.target.value }))}
                    placeholder="مثال: کتابخانه دانشکده مهندسی" />
                </div>
                <div className="bm-field">
                  <label>توضیحات</label>
                  <textarea rows={3} value={reqForm.description}
                    onChange={(e) => setReqForm((f) => ({ ...f, description: e.target.value }))}
                    placeholder="معرفی کوتاه کتابخانه و حوزه‌ی منابع آن" />
                </div>
                <div className="bm-field">
                  <label>مدت امانت پیش‌فرض (روز)</label>
                  <input type="number" min={1} value={reqForm.defaultBorrowDurationDays}
                    onChange={(e) => setReqForm((f) => ({ ...f, defaultBorrowDurationDays: e.target.value }))} />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setShowReqModal(false)}>انصراف</button>
              <button className="btn btn-primary" onClick={submitLibraryRequest} disabled={reqSaving}>
                {reqSaving ? "در حال ثبت..." : "ثبت درخواست"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default LibrariesPage;
