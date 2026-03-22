import React, { useEffect, useState, useCallback } from "react";
import { libraryAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import "./LibrariesPage.css";

const statusLabel = {
  ACTIVE: "فعال",
  PENDING: "در انتظار تأیید",
  REJECTED: "رد شده",
  SUSPENDED: "معلق",
};

const roleLabel = {
  PATRON: "اعضا",
  LIBRARIAN: "کتابدار",
  ADMIN: "مدیر",
};

const LibrariesPage = () => {
  const { isAuthenticated, user } = useAuth();
  const [publicLibraries, setPublicLibraries] = useState([]);
  const [userLibraries, setUserLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    autoMembershipApproval: false,
    defaultBorrowDurationDays: 14,
  });
  const [activeLibraryId, setActiveLibraryId] = useState(
    localStorage.getItem("activeLibraryId") || "",
  );
  const [activeLibraryName, setActiveLibraryName] = useState(
    localStorage.getItem("activeLibraryName") || "",
  );
  const [showCreateForm, setShowCreateForm] = useState(false);

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
    } catch (err) {
      setError(err.response?.data?.message || "خطا در ارسال درخواست عضویت");
    }
  };

  const handleSetActiveLibrary = (library) => {
    localStorage.setItem("activeLibraryId", library.id);
    localStorage.setItem("activeLibraryName", library.name);
    setActiveLibraryId(String(library.id));
    setActiveLibraryName(library.name);
    setSuccessMessage(`کتابخانه فعال به «${library.name}» تغییر کرد`);
    setTimeout(() => setSuccessMessage(""), 3000);
  };

  const getUserLibraryStatus = (libraryId) => {
    const membership = userLibraries.find((lib) => lib.id === libraryId);
    if (!membership) return null;
    return { role: membership.userRole, status: membership.userStatus };
  };

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleCreateLibrary = async (e) => {
    e.preventDefault();
    try {
      setError("");
      setSuccessMessage("");
      await libraryAPI.createLibrary({
        name: formData.name,
        description: formData.description || undefined,
        autoMembershipApproval: formData.autoMembershipApproval,
        defaultBorrowDurationDays: Number(formData.defaultBorrowDurationDays),
      });
      setSuccessMessage("کتابخانه با موفقیت ایجاد شد");
      setFormData({ name: "", description: "", autoMembershipApproval: false, defaultBorrowDurationDays: 14 });
      setShowCreateForm(false);
      fetchLibraries();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در ایجاد کتابخانه");
    }
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
          {isAuthenticated && user?.systemRole === "SYSTEM_ADMIN" && (
            <button
              className="btn btn-accent"
              onClick={() => setShowCreateForm(!showCreateForm)}
            >
              {showCreateForm ? "× بستن فرم" : "+ ایجاد کتابخانه"}
            </button>
          )}
        </div>
      </div>

      <div className="libraries-body">
        {error && <div className="error-message">{error}</div>}
        {successMessage && <div className="success-message">{successMessage}</div>}

        {activeLibraryId && (
          <div className="active-library-banner">
            <span>🏛️</span>
            کتابخانه فعال: <strong>{activeLibraryName}</strong>
          </div>
        )}

        {/* Create Library Form */}
        {isAuthenticated && user?.systemRole === "SYSTEM_ADMIN" && showCreateForm && (
          <div className="create-library-card">
            <h3 className="create-library-title">ایجاد کتابخانه جدید</h3>
            <form onSubmit={handleCreateLibrary}>
              <div className="form-group">
                <label htmlFor="name">نام کتابخانه</label>
                <input id="name" name="name" type="text" value={formData.name} onChange={handleFormChange} required placeholder="مثلاً: کتابخانه مرکزی" />
              </div>
              <div className="form-group">
                <label htmlFor="description">توضیحات</label>
                <textarea id="description" name="description" rows="3" value={formData.description} onChange={handleFormChange} placeholder="توضیح مختصری درباره کتابخانه..." />
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label htmlFor="defaultBorrowDurationDays">مدت امانت پیش‌فرض (روز)</label>
                  <input id="defaultBorrowDurationDays" name="defaultBorrowDurationDays" type="number" min="1" value={formData.defaultBorrowDurationDays} onChange={handleFormChange} />
                </div>
                <div className="form-group checkbox-group">
                  <label className="checkbox-label">
                    <input type="checkbox" name="autoMembershipApproval" checked={formData.autoMembershipApproval} onChange={handleFormChange} />
                    <span>تأیید خودکار عضویت</span>
                  </label>
                </div>
              </div>
              <div className="form-actions">
                <button className="btn btn-primary" type="submit">ایجاد کتابخانه</button>
                <button className="btn btn-ghost" type="button" onClick={() => setShowCreateForm(false)}>انصراف</button>
              </div>
            </form>
          </div>
        )}

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
                  <div key={library.id} className={`library-card ${String(library.id) === String(activeLibraryId) ? "library-card-active" : ""}`}>
                    {String(library.id) === String(activeLibraryId) && (
                      <div className="active-indicator">فعال</div>
                    )}
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
                    <button className="btn btn-primary btn-sm lib-action-btn" onClick={() => handleSetActiveLibrary(library)}>
                      فعال کردن
                    </button>
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
                const isMember = !!membershipInfo;
                const isPending = membershipInfo?.status === "PENDING";
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
                      isMember ? (
                        <div className={`lib-member-status ${isPending ? "pending" : "member"}`}>
                          {isPending ? "⏳ درخواست در انتظار تأیید" : "✓ عضو هستید"}
                        </div>
                      ) : (
                        <button className="btn btn-outline btn-sm lib-action-btn" onClick={() => handleRequestMembership(library.id)}>
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
