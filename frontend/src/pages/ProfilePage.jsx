import React, { useEffect, useState } from "react";
import { loginUrl, userAPI } from "../services/api";
import MyLibraryActivity from "../components/MyLibraryActivity";
import "./ProfilePage.css";

const extractError = (err) =>
  err.response?.data?.error || err.response?.data?.message || "خطا رخ داد";

const ROLE_LABELS = {
  SUPER_ADMIN: "ادمین اصلی",
  SYSTEM_ADMIN: "مدیر سیستم",
  USER: "کاربر عادی",
};

const TABS = [
  { key: "account", label: "👤 اطلاعات حساب" },
  { key: "activity", label: "📚 فعالیت کتابخانه‌ای" },
  { key: "password", label: "🔐 امنیت حساب" },
];

const ProfilePage = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [tab, setTab] = useState("account");

  // Edit profile state
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    phoneNumber: "",
    deliveryAddress: "",
    internalExtension: "",
  });
  const [saving, setSaving] = useState(false);
  const [editSuccess, setEditSuccess] = useState("");

  useEffect(() => {
    userAPI.getProfile()
      .then((res) => {
        const data = res.data?.data || res.data;
        setProfile(data);
        setForm({
          firstName: data.firstName || "",
          lastName: data.lastName || "",
          phoneNumber: data.phoneNumber || "",
          deliveryAddress: data.deliveryAddress || "",
          internalExtension: data.internalExtension || "",
        });
      })
      .catch(() => setError("خطا در بارگذاری پروفایل"))
      .finally(() => setLoading(false));
  }, []);

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    if (form.internalExtension && !/^\d{8}$/.test(form.internalExtension)) {
      setError("شماره تلفن داخلی باید ۸ رقم باشد");
      return;
    }
    try {
      setSaving(true);
      setError("");
      const res = await userAPI.updateProfile(form);
      const updated = res.data?.data || res.data;
      setProfile(updated);
      setEditing(false);
      setEditSuccess("پروفایل با موفقیت به‌روزرسانی شد");
      setTimeout(() => setEditSuccess(""), 3500);
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری پروفایل...</div>;

  const displayName = profile
    ? `${profile.firstName || ""} ${profile.lastName || ""}`.trim()
    : "";
  const initials = displayName
    ? displayName.split(" ").map((n) => n[0]).join("").slice(0, 2)
    : profile?.email?.charAt(0).toUpperCase();

  return (
    <div className="profile-page">
      <div className="profile-header">
        <div className="profile-header-inner">
          <h1 className="profile-main-title">پروفایل کاربری</h1>
          <p className="profile-main-sub">مشاهده و مدیریت اطلاعات حساب</p>
        </div>
      </div>

      <div className="profile-body">
        {/* Identity card — always visible */}
        <div className="profile-card" style={{ marginBottom: "1.5rem" }}>
          <div className="profile-avatar-section">
            <div className="profile-avatar">{initials}</div>
            <div className="profile-name-block">
              <h2 className="profile-display-name">{displayName || "کاربر"}</h2>
              <span className="profile-role-badge">
                {ROLE_LABELS[profile?.systemRole] || profile?.systemRole || "کاربر"}
              </span>
            </div>
          </div>
        </div>

        {/* Top-level tabs */}
        <div className="profile-tabs">
          {TABS.map((t) => (
            <button
              key={t.key}
              className={`profile-tab ${tab === t.key ? "profile-tab--active" : ""}`}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* Account tab */}
        {tab === "account" && (
        <div className="profile-card">
          {error && <div className="error-message" style={{ margin: "1rem 2rem 0" }}>{error}</div>}
          {editSuccess && <div className="success-message" style={{ margin: "1rem 2rem 0" }}>{editSuccess}</div>}

          <div className="profile-card-head">
            <h3 className="profile-section-title">👤 اطلاعات حساب</h3>
            {!editing && (
              <button
                className="btn btn-outline btn-sm"
                onClick={() => {
                  setEditing(true);
                  setError("");
                }}
              >
                ✏️ ویرایش
              </button>
            )}
          </div>

          <div className="profile-divider" />

          {editing ? (
            <form onSubmit={handleSaveProfile} style={{ padding: "1.5rem 2rem 2rem" }}>
              <div className="ap-form-grid">
                <div className="ap-form-group">
                  <label>نام *</label>
                  <input
                    name="firstName"
                    value={form.firstName}
                    onChange={handleFormChange}
                    required
                    placeholder="نام"
                  />
                </div>
                <div className="ap-form-group">
                  <label>نام خانوادگی *</label>
                  <input
                    name="lastName"
                    value={form.lastName}
                    onChange={handleFormChange}
                    required
                    placeholder="نام خانوادگی"
                  />
                </div>
                <div className="ap-form-group">
                  <label>شماره تلفن</label>
                  <input
                    name="phoneNumber"
                    value={form.phoneNumber}
                    onChange={handleFormChange}
                    placeholder="09121234567"
                  />
                </div>
                <div className="ap-form-group">
                  <label>شماره تلفن داخلی</label>
                  <input
                    name="internalExtension"
                    value={form.internalExtension}
                    onChange={(e) =>
                      setForm((prev) => ({
                        ...prev,
                        internalExtension: e.target.value.replace(/\D/g, "").slice(0, 8),
                      }))
                    }
                    inputMode="numeric"
                    maxLength={8}
                    placeholder="۸ رقم"
                  />
                </div>
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label>آدرس تحویل کتاب فیزیکی</label>
                  <textarea
                    name="deliveryAddress"
                    value={form.deliveryAddress}
                    onChange={handleFormChange}
                    rows={2}
                    placeholder="مثال: ساختمان فناوری اطلاعات، طبقه دوم، اتاق ۱۱۲"
                  />
                </div>
              </div>
              <div className="ap-modal-actions">
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  {saving ? "در حال ذخیره..." : "ذخیره تغییرات"}
                </button>
                <button
                  className="btn btn-outline"
                  type="button"
                  onClick={() => {
                    setEditing(false);
                    setForm({
                      firstName: profile.firstName || "",
                      lastName: profile.lastName || "",
                      phoneNumber: profile.phoneNumber || "",
                      deliveryAddress: profile.deliveryAddress || "",
                      internalExtension: profile.internalExtension || "",
                    });
                    setError("");
                  }}
                >
                  انصراف
                </button>
              </div>
            </form>
          ) : (
            <div className="profile-info-grid">
              <div className="profile-info-item">
                <span className="info-icon">📧</span>
                <div>
                  <span className="info-label">ایمیل</span>
                  <span className="info-value">{profile?.email}</span>
                </div>
              </div>

              {profile?.phoneNumber && (
                <div className="profile-info-item">
                  <span className="info-icon">📱</span>
                  <div>
                    <span className="info-label">شماره تلفن</span>
                    <span className="info-value">{profile.phoneNumber}</span>
                  </div>
                </div>
              )}

              {profile?.internalExtension && (
                <div className="profile-info-item">
                  <span className="info-icon">☎️</span>
                  <div>
                    <span className="info-label">تلفن داخلی</span>
                    <span className="info-value">{profile.internalExtension}</span>
                  </div>
                </div>
              )}

              {profile?.deliveryAddress && (
                <div className="profile-info-item">
                  <span className="info-icon">📍</span>
                  <div>
                    <span className="info-label">آدرس تحویل</span>
                    <span className="info-value">{profile.deliveryAddress}</span>
                  </div>
                </div>
              )}

              {profile?.createdAt && (
                <div className="profile-info-item">
                  <span className="info-icon">📅</span>
                  <div>
                    <span className="info-label">عضو از</span>
                    <span className="info-value">
                      {new Date(profile.createdAt).toLocaleDateString("fa-IR")}
                    </span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
        )}

        {/* Library activity tab: my borrows / downloads / favorites */}
        {tab === "activity" && (
        <div className="profile-card" style={{ padding: "1.5rem 2rem" }}>
          <MyLibraryActivity />
        </div>
        )}

        {/* Account security tab: credentials are managed by the identity provider */}
        {tab === "password" && (
        <div className="profile-card" style={{ padding: "1.5rem 2rem 2rem" }}>
          <h3 style={{ fontSize: "1rem", fontWeight: 700, color: "var(--color-text)", margin: 0 }}>
            🔐 امنیت حساب
          </h3>
          <p style={{ fontSize: "0.85rem", color: "var(--color-text-muted)", lineHeight: 1.9 }}>
            رمز عبور و ورود دومرحله‌ای در سامانه‌ی احراز هویت متمرکز مدیریت می‌شوند. برای تغییر رمز، رمز فعلی
            را وارد می‌کنید و رمز جدید باید دست‌کم ۴ نویسه‌ی تازه داشته باشد و با رمزهای اخیر یکسان نباشد.
          </p>
          <div className="ap-modal-actions" style={{ justifyContent: "flex-start" }}>
            <a className="btn btn-primary" href={loginUrl({ action: "password", returnTo: "/profile" })}>
              تغییر رمز عبور
            </a>
            <a className="btn btn-outline" href={loginUrl({ action: "otp", returnTo: "/profile" })}>
              تنظیم برنامه‌ی ورود دومرحله‌ای
            </a>
          </div>
        </div>
        )}
      </div>
    </div>
  );
};

export default ProfilePage;
