import React from "react";
import { Link, Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AjaLogo from "../components/AjaLogo";
import { AuthDecoration } from "./LoginPage";
import "./AuthPages.css";

/** Accounts are created in the identity provider, which enforces the password policy. */
const RegisterPage = () => {
  const { register, isAuthenticated, loading } = useAuth();

  if (!loading && isAuthenticated) {
    return <Navigate to="/libraries" replace />;
  }

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-brand">
          <AjaLogo size={32} />
          <span className="auth-brand-name">کتابخانه هوشمند آجا</span>
        </div>
        <h2 className="auth-title">ایجاد حساب کاربری</h2>
        <p className="auth-subtitle">
          ثبت‌نام در سامانه‌ی احراز هویت متمرکز انجام می‌شود. رمز عبور باید دست‌کم ۸ نویسه و شامل حرف بزرگ،
          حرف کوچک، عدد و نویسه‌ی ویژه باشد و توالی (مانند abc یا ۱۲۳) نداشته باشد. پس از ثبت‌نام، فعال‌سازی
          ورود دومرحله‌ای با برنامه‌ی Authenticator لازم است.
        </p>

        <button type="button" className="auth-submit-btn" onClick={register}>
          📝 ثبت‌نام در سامانه‌ی احراز هویت
        </button>

        <p className="auth-footer-text">
          حساب کاربری دارید؟{" "}
          <Link to="/login" className="auth-link">وارد شوید</Link>
        </p>
      </div>
      <AuthDecoration />
    </div>
  );
};

export default RegisterPage;
