import React from "react";
import { useAuth } from "../context/AuthContext";
import "./ProfilePage.css";

const ROLE_LABELS = {
  SYSTEM_ADMIN: "مدیر سیستم",
  USER: "کاربر عادی",
};

const ProfilePage = () => {
  const { user } = useAuth();

  if (!user) {
    return <div className="loading">در حال بارگذاری پروفایل...</div>;
  }

  const initials = user.name
    ? user.name.split(" ").map((n) => n[0]).join("").slice(0, 2)
    : user.email?.charAt(0).toUpperCase();

  return (
    <div className="profile-page">
      <div className="profile-header">
        <div className="profile-header-inner">
          <h1 className="profile-main-title">پروفایل کاربری</h1>
          <p className="profile-main-sub">مشاهده و مدیریت اطلاعات حساب</p>
        </div>
      </div>

      <div className="profile-body">
        <div className="profile-card">
          {/* Avatar */}
          <div className="profile-avatar-section">
            <div className="profile-avatar">{initials}</div>
            <div className="profile-name-block">
              <h2 className="profile-display-name">{user.name || "کاربر"}</h2>
              <span className="profile-role-badge">
                {ROLE_LABELS[user.systemRole] || user.systemRole || "کاربر"}
              </span>
            </div>
          </div>

          <div className="profile-divider" />

          {/* Info Rows */}
          <div className="profile-info-grid">
            <div className="profile-info-item">
              <span className="info-icon">📧</span>
              <div>
                <span className="info-label">ایمیل</span>
                <span className="info-value">{user.email}</span>
              </div>
            </div>

            {user.name && (
              <div className="profile-info-item">
                <span className="info-icon">👤</span>
                <div>
                  <span className="info-label">نام کامل</span>
                  <span className="info-value">{user.name}</span>
                </div>
              </div>
            )}

            {user.role && (
              <div className="profile-info-item">
                <span className="info-icon">🎭</span>
                <div>
                  <span className="info-label">نقش سیستمی</span>
                  <span className="info-value">{ROLE_LABELS[user.role] || user.role}</span>
                </div>
              </div>
            )}

            {user.createdAt && (
              <div className="profile-info-item">
                <span className="info-icon">📅</span>
                <div>
                  <span className="info-label">عضو از</span>
                  <span className="info-value">
                    {new Date(user.createdAt).toLocaleDateString("fa-IR")}
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
