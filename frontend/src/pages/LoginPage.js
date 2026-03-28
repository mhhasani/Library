import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import PasswordInput from "../components/PasswordInput";
import AjaLogo from "../components/AjaLogo";
import "./AuthPages.css";

const LoginPage = () => {
  const [formData, setFormData] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    const result = await login(formData);
    if (result.success) {
      navigate("/libraries");
    } else {
      setError(result.error);
    }
    setLoading(false);
  };

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-brand">
          <AjaLogo size={32} />
          <span className="auth-brand-name">کتابخانه هوشمند آجا</span>
        </div>
        <h2 className="auth-title">ورود به سامانه</h2>
        <p className="auth-subtitle">خوش آمدید — لطفاً اطلاعات کاربری خود را وارد کنید</p>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="email">ایمیل</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              placeholder="example@army.ir"
              autoComplete="email"
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">رمز عبور</label>
            <PasswordInput
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              placeholder="••••••••"
              autoComplete="current-password"
            />
          </div>
          <button type="submit" disabled={loading} className="auth-submit-btn">
            {loading ? (
              <span className="btn-loading">
                <span className="btn-spinner" /> در حال ورود...
              </span>
            ) : (
              "ورود به سامانه"
            )}
          </button>
        </form>

        <p className="auth-footer-text">
          حساب کاربری ندارید؟{" "}
          <Link to="/register" className="auth-link">ثبت‌نام کنید</Link>
        </p>
      </div>

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
    </div>
  );
};

export default LoginPage;
