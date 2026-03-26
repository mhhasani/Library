import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import PasswordInput from "../components/PasswordInput";
import "./AuthPages.css";

const RegisterPage = () => {
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirmPassword: "",
    phoneNumber: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (formData.password !== formData.confirmPassword) {
      setError("رمز عبور و تکرار آن مطابقت ندارند");
      return;
    }
    setLoading(true);
    const result = await register({
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      password: formData.password,
      phoneNumber: formData.phoneNumber,
    });
    if (result.success) {
      navigate("/libraries");
    } else {
      setError(result.error);
    }
    setLoading(false);
  };

  return (
    <div className="auth-page auth-page-register">
      <div className="auth-panel auth-panel-wide">
        <div className="auth-brand">
          <span className="auth-brand-icon">📚</span>
          <span className="auth-brand-name">سامانه کتابخانه</span>
        </div>
        <h2 className="auth-title">ایجاد حساب کاربری</h2>
        <p className="auth-subtitle">ثبت‌نام رایگان و دسترسی فوری به کتابخانه‌ها</p>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-row-2">
            <div className="form-group">
              <label htmlFor="firstName">نام</label>
              <input
                type="text"
                id="firstName"
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                required
                placeholder="علی"
              />
            </div>
            <div className="form-group">
              <label htmlFor="lastName">نام خانوادگی</label>
              <input
                type="text"
                id="lastName"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                required
                placeholder="محمدی"
              />
            </div>
          </div>

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
            <label htmlFor="phoneNumber">شماره تلفن <span className="optional-label">(اختیاری)</span></label>
            <input
              type="tel"
              id="phoneNumber"
              name="phoneNumber"
              value={formData.phoneNumber}
              onChange={handleChange}
              placeholder="09123456789"
            />
          </div>

          <div className="form-row-2">
            <div className="form-group">
              <label htmlFor="password">رمز عبور</label>
              <PasswordInput
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                placeholder="••••••••"
                autoComplete="new-password"
              />
            </div>
            <div className="form-group">
              <label htmlFor="confirmPassword">تکرار رمز عبور</label>
              <PasswordInput
                id="confirmPassword"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                required
                placeholder="••••••••"
                autoComplete="new-password"
              />
            </div>
          </div>

          <button type="submit" disabled={loading} className="auth-submit-btn">
            {loading ? (
              <span className="btn-loading">
                <span className="btn-spinner" /> در حال ثبت‌نام...
              </span>
            ) : (
              "ثبت‌نام"
            )}
          </button>
        </form>

        <p className="auth-footer-text">
          حساب کاربری دارید؟{" "}
          <Link to="/login" className="auth-link">وارد شوید</Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;
