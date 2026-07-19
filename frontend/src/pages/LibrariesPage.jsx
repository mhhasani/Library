import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { libraryAPI, libraryRequestAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import GlobalSearchBar from "../components/GlobalSearchBar";
import { toPersianNum } from "../utils/persian";
import "../components/BorrowModal.css";
import "./LibrariesPage.css";

const initialOf = (name = "") => (name.trim() ? name.trim()[0] : "؟");

// Deterministic gold/navy-tinted avatar hue picked from the library's name,
// so the same library always renders the same accent without extra data.
const avatarHue = (name = "") => {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) % 360;
  return hash;
};

const SkeletonCard = () => (
  <div className="library-card library-card-skeleton" aria-hidden="true">
    <div className="library-card-spine skel-spine" />
    <div className="library-card-body">
      <div className="skel skel-line" style={{ width: "70%", height: "1.1rem" }} />
      <div className="skel skel-line" style={{ width: "40%", height: "0.5rem", marginTop: "0.6rem" }} />
      <div className="skel skel-line" style={{ width: "95%" }} />
      <div className="skel skel-line" style={{ width: "55%" }} />
    </div>
  </div>
);

const LibraryCard = ({ library, mine, membershipInfo, isAuthenticated, onOpen, onManage, onJoin, index }) => {
  const isApproved = mine ? library.userStatus === "APPROVED" : membershipInfo?.status === "APPROVED";
  const isPending  = mine ? library.userStatus === "PENDING"  : membershipInfo?.status === "PENDING";
  const hue = avatarHue(library.name);

  return (
    <div
      className="library-card"
      style={{ "--i": index, "--spine-hue": hue }}
    >
      <div className="library-card-spine" aria-hidden="true">
        <span className="library-card-spine-lines" />
        <span className="library-card-monogram">{initialOf(library.name)}</span>
      </div>

      <div className="library-card-body">
        <h3 className="library-card-name">{library.name}</h3>
        <span className="library-card-rule" aria-hidden="true" />
        <p className="library-card-desc">{library.description || "توضیحی برای این کتابخانه ثبت نشده است."}</p>

        {!mine && library.ownerName && (
          <div className="library-card-owner">
            <span className="owner-dot" />
            مالک: {library.ownerName}
          </div>
        )}

        <div className="lib-actions-row">
          {isApproved ? (
            <>
              <button className="btn btn-primary btn-sm lib-action-btn" onClick={onOpen}>
                باز کردن کتابخانه
              </button>
              {mine && library.userRole === "ADMIN" && (
                <button className="btn btn-outline btn-sm lib-action-btn lib-action-icon" onClick={onManage} title="مدیریت کتابخانه">
                  مدیریت
                </button>
              )}
            </>
          ) : isPending ? (
            <div className="lib-member-status pending">
              <span className="pending-dot" /> در انتظار تأیید عضویت
            </div>
          ) : !mine ? (
            isAuthenticated ? (
              <button className="btn btn-outline btn-sm lib-action-btn" onClick={onJoin}>
                درخواست عضویت
              </button>
            ) : (
              <p className="lib-login-note">برای عضویت وارد حساب خود شوید</p>
            )
          ) : null}
        </div>
      </div>
    </div>
  );
};

const LibrariesPage = () => {
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const [publicLibraries, setPublicLibraries] = useState([]);
  const [userLibraries, setUserLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [activeTab, setActiveTab] = useState("mine");

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

  useEffect(() => {
    if (!isAuthenticated) setActiveTab("public");
  }, [isAuthenticated]);

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

  const approvedCount = userLibraries.filter((l) => l.userStatus === "APPROVED").length;
  const canRequestLibrary = isAuthenticated && !["SYSTEM_ADMIN", "SUPER_ADMIN"].includes(user?.systemRole);

  return (
    <div className="libraries-page">
      <header className="lib-hero">
        <div className="lib-hero-colonnade lib-hero-colonnade-start" aria-hidden="true" />
        <div className="lib-hero-colonnade lib-hero-colonnade-end" aria-hidden="true" />

        <div className="lib-hero-inner">
          <div className="lib-hero-text">
            <div className="lib-hero-emblem" aria-hidden="true">
              <svg viewBox="0 0 48 48" width="34" height="34">
                <path d="M24 4 L44 14 V16 H4 V14 Z" fill="currentColor" opacity="0.9" />
                <rect x="8" y="18" width="4" height="20" fill="currentColor" opacity="0.75" />
                <rect x="16" y="18" width="4" height="20" fill="currentColor" opacity="0.75" />
                <rect x="22" y="18" width="4" height="20" fill="currentColor" opacity="0.75" />
                <rect x="28" y="18" width="4" height="20" fill="currentColor" opacity="0.75" />
                <rect x="36" y="18" width="4" height="20" fill="currentColor" opacity="0.75" />
                <rect x="4" y="40" width="40" height="3" fill="currentColor" />
              </svg>
            </div>
            <span className="lib-hero-eyebrow">شبکه کتابخانه‌های سربازی</span>
            <h1 className="libraries-main-title">کتابخانه‌ها</h1>
            <span className="lib-hero-flourish" aria-hidden="true">
              <i /><span>❦</span><i />
            </span>
            <p className="libraries-main-sub">
              عضو کتابخانه‌ها شوید، به منابع دیجیتال و چاپی دسترسی پیدا کنید و امانت‌های خود را یک‌جا مدیریت کنید.
            </p>
          </div>

          <div className="lib-hero-stats">
            <div className="lib-stat">
              <strong>{loading ? "—" : toPersianNum(publicLibraries.length)}</strong>
              <span>کتابخانه عمومی</span>
            </div>
            {isAuthenticated && (
              <div className="lib-stat lib-stat-accent">
                <strong>{loading ? "—" : toPersianNum(approvedCount)}</strong>
                <span>عضویت فعال من</span>
              </div>
            )}
          </div>
        </div>

      </header>

      <div className="lib-hero-search-dock">
        <GlobalSearchBar />
      </div>

      <div className="libraries-body">
        {error && <div className="error-message">{error}</div>}
        {successMessage && <div className="success-message">{successMessage}</div>}

        <div className="lib-toolbar">
          {isAuthenticated && (
            <div className="lib-tabs" role="tablist">
              <button
                role="tab"
                aria-selected={activeTab === "mine"}
                className={`lib-tab ${activeTab === "mine" ? "active" : ""}`}
                onClick={() => setActiveTab("mine")}
              >
                کتابخانه‌های من
                {userLibraries.length > 0 && <span className="lib-tab-count">{toPersianNum(userLibraries.length)}</span>}
              </button>
              <button
                role="tab"
                aria-selected={activeTab === "public"}
                className={`lib-tab ${activeTab === "public" ? "active" : ""}`}
                onClick={() => setActiveTab("public")}
              >
                کتابخانه‌های عمومی
                {publicLibraries.length > 0 && <span className="lib-tab-count">{toPersianNum(publicLibraries.length)}</span>}
              </button>
            </div>
          )}

          {canRequestLibrary && (
            <button className="btn btn-accent lib-create-btn" onClick={() => setShowReqModal(true)}>
              <span className="lib-create-plus">+</span> درخواست ایجاد کتابخانه
            </button>
          )}
        </div>

        <div className="lib-divider" aria-hidden="true"><i /><span>&#10022;</span><i /></div>

        {loading ? (
          <div className="libraries-grid">
            {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}
          </div>
        ) : (
          <>
            {(!isAuthenticated || activeTab === "mine") && isAuthenticated && (
              <section className="lib-section">
                {userLibraries.length === 0 ? (
                  <div className="empty-state">
                    <span className="empty-icon-frame"><span className="empty-icon">🏛️</span></span>
                    <p>هنوز عضو هیچ کتابخانه‌ای نشده‌اید</p>
                    <button className="btn btn-outline btn-sm" onClick={() => setActiveTab("public")}>
                      مشاهده کتابخانه‌های عمومی
                    </button>
                  </div>
                ) : (
                  <div className="libraries-grid">
                    {userLibraries.map((library, i) => (
                      <LibraryCard
                        key={library.id}
                        library={library}
                        mine
                        isAuthenticated={isAuthenticated}
                        index={i}
                        onOpen={() => navigate(`/libraries/${library.id}/books`)}
                        onManage={() => navigate(`/libraries/${library.id}/admin`)}
                      />
                    ))}
                  </div>
                )}
              </section>
            )}

            {(!isAuthenticated || activeTab === "public") && (
              <section className="lib-section">
                {publicLibraries.length === 0 ? (
                  <div className="empty-state">
                    <span className="empty-icon-frame"><span className="empty-icon">🔍</span></span>
                    <p>هیچ کتابخانه عمومی یافت نشد</p>
                  </div>
                ) : (
                  <div className="libraries-grid">
                    {publicLibraries.map((library, i) => {
                      const membershipInfo = getUserLibraryStatus(library.id);
                      return (
                        <LibraryCard
                          key={library.id}
                          library={library}
                          mine={false}
                          membershipInfo={membershipInfo}
                          isAuthenticated={isAuthenticated}
                          index={i}
                          onOpen={() => navigate(`/libraries/${library.id}/books`)}
                          onJoin={() => handleRequestMembership(library.id)}
                        />
                      );
                    })}
                  </div>
                )}
              </section>
            )}
          </>
        )}
      </div>

      {showReqModal && (
        <div className="modal-overlay" onClick={() => setShowReqModal(false)}>
          <div className="modal-content" style={{ maxWidth: 480 }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>درخواست ایجاد کتابخانه</h2>
              <button className="modal-close-btn" onClick={() => setShowReqModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: "0.85rem", color: "var(--color-text-secondary)", marginBottom: "1rem" }}>
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
