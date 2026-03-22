import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./HomePage.css";

const features = [
  {
    icon: "📖",
    title: "مرور کتاب‌ها",
    desc: "دسترسی به هزاران کتاب از مجموعه گسترده کتابخانه‌های عضو",
  },
  {
    icon: "📤",
    title: "امانت کتاب",
    desc: "کتاب امانت بگیرید و با سیستم هوشمند، تاریخ بازگشت را پیگیری کنید",
  },
  {
    icon: "🏛️",
    title: "مدیریت کتابخانه",
    desc: "عضویت در کتابخانه‌های مختلف و مدیریت آسان منابع کتابخانه",
  },
  {
    icon: "👤",
    title: "پروفایل شخصی",
    desc: "مدیریت حساب کاربری و مشاهده تاریخچه امانت‌های خود",
  },
];

const HomePage = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="home-page">
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-bg" />
        <div className="hero-content">
          <div className="hero-badge">سامانه هوشمند مدیریت کتابخانه</div>
          <h1 className="hero-title">
            دروازه‌ای به دنیای
            <span className="hero-title-accent"> کتاب و دانش</span>
          </h1>
          <p className="hero-subtitle">
            به هزاران کتاب دسترسی داشته باشید، امانت بگیرید و تجربه خواندن را
            لذت‌بخش‌تر کنید
          </p>
          <div className="hero-actions">
            <Link to="/libraries" className="btn btn-accent btn-lg">
              مشاهده کتابخانه‌ها
            </Link>
            {!isAuthenticated ? (
              <Link to="/register" className="btn btn-lg hero-btn-outline">
                ثبت‌نام رایگان
              </Link>
            ) : (
              <Link to="/libraries" className="btn btn-lg hero-btn-outline">
                مرور کتابخانه‌ها
              </Link>
            )}
          </div>
          <div className="hero-stats">
            <div className="stat">
              <span className="stat-num">۱۰۰+</span>
              <span className="stat-label">کتابخانه عضو</span>
            </div>
            <div className="stat-divider" />
            <div className="stat">
              <span className="stat-num">۵۰۰۰+</span>
              <span className="stat-label">عنوان کتاب</span>
            </div>
            <div className="stat-divider" />
            <div className="stat">
              <span className="stat-num">۲۰۰۰+</span>
              <span className="stat-label">کاربر فعال</span>
            </div>
          </div>
        </div>
        <div className="hero-illustration">
          <div className="books-stack">
            <div className="book-item b1">📚</div>
            <div className="book-item b2">📖</div>
            <div className="book-item b3">📗</div>
            <div className="book-item b4">📘</div>
            <div className="book-item b5">📕</div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="features-container">
          <div className="features-header">
            <h2 className="features-title">چرا سامانه کتابخانه؟</h2>
            <p className="features-subtitle">همه چیزی که برای مدیریت مطالعه‌تان نیاز دارید</p>
          </div>
          <div className="features-grid">
            {features.map((f, i) => (
              <div className="feature-card" key={i}>
                <div className="feature-icon-wrap">
                  <span className="feature-icon">{f.icon}</span>
                </div>
                <h3 className="feature-title">{f.title}</h3>
                <p className="feature-desc">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      {!isAuthenticated && (
        <section className="cta-section">
          <div className="cta-card">
            <h2>همین الان شروع کنید</h2>
            <p>ثبت‌نام رایگان و دسترسی فوری به تمام امکانات</p>
            <div className="cta-actions">
              <Link to="/register" className="btn btn-accent btn-lg">
                ثبت‌نام رایگان
              </Link>
              <Link to="/login" className="btn btn-lg cta-btn-outline">
                ورود به حساب
              </Link>
            </div>
          </div>
        </section>
      )}
    </div>
  );
};

export default HomePage;
