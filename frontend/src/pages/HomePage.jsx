import React, { useEffect, useState, useRef } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { statsAPI } from "../services/api";
import GlobalSearchBar from "../components/GlobalSearchBar";
import "./HomePage.css";

const features = [
  {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
        <line x1="10" y1="9" x2="16" y2="9"/>
        <line x1="10" y1="13" x2="14" y2="13"/>
      </svg>
    ),
    title: "منابع علمی و تخصصی",
    desc: "دسترسی به هزاران منبع علمی، نظامی و تخصصی از مجموعه کتابخانه‌های ارتش",
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="16 3 21 3 21 8"/>
        <line x1="4" y1="20" x2="21" y2="3"/>
        <polyline points="21 16 21 21 16 21"/>
        <line x1="15" y1="15" x2="21" y2="21"/>
      </svg>
    ),
    title: "امانت هوشمند",
    desc: "درخواست امانت، تمدید و پیگیری دیجیتال — بدون مراجعه حضوری",
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
        <line x1="8" y1="21" x2="16" y2="21"/>
        <line x1="12" y1="17" x2="12" y2="21"/>
      </svg>
    ),
    title: "منابع دیجیتال",
    desc: "مطالعه نسخه‌های دیجیتال کتاب‌ها و دسترسی آنلاین در هر زمان و مکان",
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
      </svg>
    ),
    title: "مدیریت کتابخانه",
    desc: "مدیریت کامل اعضا، موجودی و گزارش‌های تحلیلی برای مدیران کتابخانه",
  },
];

function useCountUp(target, duration = 1800) {
  const [count, setCount] = useState(0);
  const startedRef = useRef(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!target) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !startedRef.current) {
          startedRef.current = true;
          const start = Date.now();
          const tick = () => {
            const elapsed = Date.now() - start;
            const progress = Math.min(elapsed / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            setCount(Math.round(eased * target));
            if (progress < 1) requestAnimationFrame(tick);
          };
          requestAnimationFrame(tick);
        }
      },
      { threshold: 0.5 }
    );
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, [target, duration]);

  return { count, ref };
}

const StatItem = ({ value, label }) => {
  const { count, ref } = useCountUp(value);
  return (
    <div className="stat" ref={ref}>
      <span className="stat-num">{count.toLocaleString("fa-IR")}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
};

const HomePage = () => {
  const { isAuthenticated } = useAuth();
  const [stats, setStats] = useState(null);

  useEffect(() => {
    statsAPI.getStats()
      .then((res) => setStats(res.data?.data || null))
      .catch(() => {});
  }, []);

  return (
    <div className="home-page">
      {/* ── Hero ── */}
      <section className="hero">
        <div className="hero-bg" />
        <div className="hero-pattern" />

        {/* Military radar — purely decorative */}
        <div className="hero-radar" aria-hidden="true">
          <div className="radar-ring radar-ring-lg" />
          <div className="radar-ring radar-ring-md" />
          <div className="radar-ring radar-ring-sm" />
          <div className="radar-sweep" />
        </div>

        <div className="hero-layout">
          <div className="hero-content">
            <div className="hero-badge">
              <span className="hero-badge-dot" />
              کتابخانه هوشمند آجا
            </div>

            <h1 className="hero-title">
              دانش،
              <span className="hero-title-accent"> قدرت</span>
              <br />و مقاومت ملی
            </h1>

            <p className="hero-subtitle">
              سامانه یکپارچه مدیریت کتابخانه‌های نیروهای مسلح — منابع علمی،
              نظامی و تخصصی را جستجو، امانت و مطالعه کنید.
            </p>

            <GlobalSearchBar />

            <div className="hero-actions">
              <Link to="/libraries" className="btn btn-accent btn-lg hero-cta">
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

            {stats && (
              <div className="hero-stats">
                <StatItem value={stats.totalLibraries} label="کتابخانه فعال" />
                <div className="stat-divider" />
                <StatItem value={stats.totalBooks} label="عنوان کتاب" />
                <div className="stat-divider" />
                <StatItem value={stats.totalUsers} label="کاربر ثبت‌نام‌شده" />
              </div>
            )}
          </div>

          <div className="hero-emblem-wrap" aria-hidden="true">
            <div className="hero-emblem">
              <div className="emblem-ring emblem-ring-1" />
              <div className="emblem-ring emblem-ring-2" />
              <div className="emblem-ring emblem-ring-3" />
              <div className="emblem-core">
                <svg viewBox="0 0 80 90" fill="none">
                  <path d="M40 2L4 18v22C4 65 20 83 40 90c20-7 36-25 36-50V18L40 2z" fill="rgba(200,160,76,0.12)" stroke="rgba(200,160,76,0.5)" strokeWidth="1.5"/>
                  <path d="M40 14L14 26v16C14 60 26 74 40 80c14-6 26-20 26-38V26L40 14z" fill="rgba(200,160,76,0.08)" stroke="rgba(200,160,76,0.35)" strokeWidth="1"/>
                  <polygon points="40,28 43,38 53,38 45,44 48,54 40,48 32,54 35,44 27,38 37,38" fill="rgba(232,201,108,0.85)"/>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Mission Band ── */}
      <div className="mission-band">
        <div className="mission-band-inner">
          <div className="mission-item">
            <span className="mission-icon">🛡️</span>
            <span>خدمت به نیروهای مسلح</span>
          </div>
          <div className="mission-sep" />
          <div className="mission-item">
            <span className="mission-icon">📚</span>
            <span>ارتقای سطح علمی کارکنان</span>
          </div>
          <div className="mission-sep" />
          <div className="mission-item">
            <span className="mission-icon">🔒</span>
            <span>دسترسی ایمن و مطمئن</span>
          </div>
        </div>
      </div>

      {/* ── Features ── */}
      <section className="features-section">
        <div className="features-container">
          <div className="features-header">
            <span className="features-eyebrow">امکانات سامانه</span>
            <h2 className="features-title">هر آنچه برای مطالعه نیاز دارید</h2>
            <p className="features-subtitle">
              سامانه یکپارچه‌ای برای مدیریت و استفاده از منابع کتابخانه‌های ارتش
            </p>
          </div>
          <div className="features-grid">
            {features.map((f, i) => (
              <div className="feature-card" key={i} style={{ animationDelay: `${i * 0.08}s` }}>
                <div className="feature-icon-wrap">{f.icon}</div>
                <h3 className="feature-title">{f.title}</h3>
                <p className="feature-desc">{f.desc}</p>
                <div className="feature-card-accent" />
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      {!isAuthenticated && (
        <section className="cta-section">
          <div className="cta-card">
            <div className="cta-card-bg" />
            <div className="cta-content">
              <span className="cta-badge">شروع کنید</span>
              <h2>به کتابخانه هوشمند آجا بپیوندید</h2>
              <p>ثبت‌نام رایگان و دسترسی فوری به تمام امکانات سامانه</p>
              <div className="cta-actions">
                <Link to="/register" className="btn btn-accent btn-lg">
                  ثبت‌نام رایگان
                </Link>
                <Link to="/login" className="btn btn-lg cta-btn-outline">
                  ورود به حساب
                </Link>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* ── Footer ── */}
      <footer className="site-footer">
        <div className="site-footer-inner">
          <div className="footer-brand">
            <span className="footer-brand-title">کتابخانه هوشمند آجا</span>
            <span className="footer-brand-sub">سامانه یکپارچه مدیریت منابع</span>
          </div>
          <div className="footer-copy">
            © تمامی حقوق محفوظ است — ارتش جمهوری اسلامی ایران
          </div>
        </div>
      </footer>
    </div>
  );
};

export default HomePage;
