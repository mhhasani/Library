import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
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
      navigate("/books");
    } else {
      setError(result.error);
    }
    setLoading(false);
  };

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-brand">
          <span className="auth-brand-icon">📚</span>
          <span className="auth-brand-name">سامانه کتابخانه</span>
        </div>
        <h2 className="auth-title">ورود به حساب</h2>
        <p className="auth-subtitle">خوش برگشتید! لطفاً اطلاعات خود را وارد کنید</p>

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
              placeholder="example@email.com"
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">رمز عبور</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              placeholder="••••••••"
            />
          </div>
          <button type="submit" disabled={loading} className="auth-submit-btn">
            {loading ? (
              <span className="btn-loading">
                <span className="btn-spinner" /> در حال ورود...
              </span>
            ) : (
              "ورود"
            )}
          </button>
        </form>

        <p className="auth-footer-text">
          حساب کاربری ندارید؟{" "}
          <Link to="/register" className="auth-link">ثبت‌نام کنید</Link>
        </p>
      </div>

      <div className="auth-decoration">
        <div className="auth-deco-circle c1" />
        <div className="auth-deco-circle c2" />
        <div className="auth-deco-circle c3" />
        <div className="auth-deco-text">
          <span>📖</span>
          <p>دروازه‌ای به دنیای کتاب و دانش</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
