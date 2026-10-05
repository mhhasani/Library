import React from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AjaLogo from "../components/AjaLogo";
import "./AuthPages.css";

/** Why the user landed on the login page (set by the backend or the session handling). */
const NOTICES = {
  "error:inactive": { type: "error", text: "حساب کاربری شما فعال نیست. با مدیر سامانه تماس بگیرید." },
  "error:failed": { type: "error", text: "ورود ناموفق بود. دوباره تلاش کنید." },
  "session:expired": { type: "info", text: "به دلیل عدم فعالیت، نشست شما پایان یافت. لطفاً دوباره وارد شوید." },
  "session:invalid": { type: "info", text: "نشست شما پایان یافت (ورود از دستگاه یا مرورگر دیگر). لطفاً دوباره وارد شوید." },
};

const LoginPage = () => {
  const { login, isAuthenticated, loading } = useAuth();
  const [params] = useSearchParams();

  if (!loading && isAuthenticated) {
    return <Navigate to="/libraries" replace />;
  }

  const notice = NOTICES[`error:${params.get("error")}`] || NOTICES[`session:${params.get("session")}`];

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-brand">
          <AjaLogo size={32} />
          <span className="auth-brand-name">کتابخانه هوشمند آجا</span>
        </div>
        <h2 className="auth-title">ورود به سامانه</h2>
        <p className="auth-subtitle">
          ورود از طریق سامانه‌ی احراز هویت متمرکز انجام می‌شود: رمز عبور، کد امنیتی تصویر و رمز یک‌بار مصرف.
        </p>

        {notice && (
          <div className={notice.type === "error" ? "error-message" : "success-message"}>{notice.text}</div>
        )}

        <button type="button" className="auth-submit-btn" onClick={() => login()}>
          🔐 ورود از طریق سامانه‌ی احراز هویت
        </button>

        <p className="auth-footer-text">
          حساب کاربری ندارید؟{" "}
          <Link to="/register" className="auth-link">ثبت‌نام کنید</Link>
        </p>
      </div>
      <AuthDecoration />
    </div>
  );
};

export const AuthDecoration = () => (
  <div className="auth-decoration">
    <div className="auth-radar" aria-hidden="true" />
    <div className="auth-deco-circle c1" />
    <div className="auth-deco-circle c2" />
    <div className="auth-deco-circle c3" />
    <div className="auth-deco-text">
      <div className="auth-deco-emblem">
        <AjaLogo size={80} />
      </div>
      <div className="auth-deco-title">کتابخانه هوشمند آجا</div>
      <p>سامانه یکپارچه مدیریت منابع علمی و تخصصی نیروهای مسلح جمهوری اسلامی ایران</p>
      <div className="auth-deco-badges">
        <span className="auth-deco-badge">📚 هزاران کتاب</span>
        <span className="auth-deco-badge">🔒 دسترسی امن</span>
        <span className="auth-deco-badge">💻 نسخه دیجیتال</span>
      </div>
    </div>
  </div>
);

export default LoginPage;
