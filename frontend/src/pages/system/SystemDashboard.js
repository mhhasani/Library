import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminAPI } from "../../services/api";
import { toPersianNum } from "../../utils/persian";
import "./SystemDashboard.css";

const SystemDashboard = () => {
  const [stats, setStats] = useState({ totalUsers: 0, activeUsers: 0, totalLibraries: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const [usersRes, librariesRes] = await Promise.all([
          adminAPI.getUsers(),
          adminAPI.getLibraries(),
        ]);
        const users = usersRes.data?.data || usersRes.data || [];
        const libraries = librariesRes.data?.data || librariesRes.data || [];
        setStats({
          totalUsers: users.length,
          activeUsers: users.filter((u) => u.accountStatus === "ACTIVE").length,
          totalLibraries: libraries.length,
        });
        setError("");
      } catch (err) {
        setError("خطا در بارگذاری اطلاعات");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">داشبورد سیستم</h1>
        <p className="ap-subtitle">خلاصه وضعیت کل سامانه</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="ap-stat-grid">
        <div className="ap-stat-card">
          <div className="ap-stat-icon ap-stat-icon--blue">👤</div>
          <div>
            <div className="ap-stat-value">{toPersianNum(stats.totalUsers)}</div>
            <div className="ap-stat-label">کل کاربران</div>
          </div>
        </div>
        <div className="ap-stat-card">
          <div className="ap-stat-icon ap-stat-icon--green">✅</div>
          <div>
            <div className="ap-stat-value">{toPersianNum(stats.activeUsers)}</div>
            <div className="ap-stat-label">کاربران فعال</div>
          </div>
        </div>
        <div className="ap-stat-card">
          <div className="ap-stat-icon ap-stat-icon--gold">🏛️</div>
          <div>
            <div className="ap-stat-value">{toPersianNum(stats.totalLibraries)}</div>
            <div className="ap-stat-label">کتابخانه‌های ثبت‌شده</div>
          </div>
        </div>
      </div>

      <div className="adb-quick-grid">
        <Link to="/system/users" className="adb-quick-card">
          <span className="adb-quick-icon">👤</span>
          <span className="adb-quick-label">مدیریت کاربران</span>
        </Link>
        <Link to="/system/libraries" className="adb-quick-card">
          <span className="adb-quick-icon">🏛️</span>
          <span className="adb-quick-label">مدیریت کتابخانه‌ها</span>
        </Link>
      </div>
    </div>
  );
};

export default SystemDashboard;
