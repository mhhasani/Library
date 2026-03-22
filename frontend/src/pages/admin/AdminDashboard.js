import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { bookAPI, borrowAPI, libraryAdminAPI } from "../../services/api";
import "./AdminDashboard.css";

const AdminDashboard = () => {
  const libraryId = localStorage.getItem("activeLibraryId");
  const libraryName = localStorage.getItem("activeLibraryName") || "کتابخانه";

  const [stats, setStats] = useState({
    totalBooks: 0,
    pendingBorrows: 0,
    pendingMembers: 0,
    activeMembers: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!libraryId) return;
    const load = async () => {
      try {
        setLoading(true);
        const [booksRes, pendingBorrowsRes, membersRes] = await Promise.all([
          bookAPI.getBooks(libraryId, { page: 0, size: 1 }),
          borrowAPI.getPendingBorrows(libraryId),
          libraryAdminAPI.getMembers(libraryId),
        ]);

        const books = booksRes.data?.data;
        const totalBooks =
          books?.totalElements ?? (Array.isArray(books) ? books.length : 0);

        const members = membersRes.data?.data || membersRes.data || [];
        const pendingMembers = members.filter((m) => m.status === "PENDING").length;
        const activeMembers = members.filter((m) => m.status === "APPROVED").length;

        const pendingBorrows = (pendingBorrowsRes.data?.data || []).length;

        setStats({ totalBooks, pendingBorrows, pendingMembers, activeMembers });
        setError("");
      } catch (err) {
        setError("خطا در بارگذاری اطلاعات داشبورد");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [libraryId]);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">داشبورد</h1>
        <p className="ap-subtitle">خلاصه وضعیت کتابخانه «{libraryName}»</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Stats */}
      <div className="ap-stat-grid">
        <div className="ap-stat-card">
          <div className="ap-stat-icon ap-stat-icon--blue">📖</div>
          <div>
            <div className="ap-stat-value">{stats.totalBooks}</div>
            <div className="ap-stat-label">کتاب ثبت‌شده</div>
          </div>
        </div>
        <div className="ap-stat-card">
          <div className="ap-stat-icon ap-stat-icon--green">✅</div>
          <div>
            <div className="ap-stat-value">{stats.activeMembers}</div>
            <div className="ap-stat-label">عضو فعال</div>
          </div>
        </div>
        <div className="ap-stat-card">
          <div className="ap-stat-icon ap-stat-icon--gold">📋</div>
          <div>
            <div className="ap-stat-value">{stats.pendingBorrows}</div>
            <div className="ap-stat-label">درخواست امانت در انتظار</div>
          </div>
        </div>
        <div className="ap-stat-card">
          <div className="ap-stat-icon ap-stat-icon--purple">👥</div>
          <div>
            <div className="ap-stat-value">{stats.pendingMembers}</div>
            <div className="ap-stat-label">درخواست عضویت در انتظار</div>
          </div>
        </div>
      </div>

      {/* Quick actions */}
      <div className="adb-quick-grid">
        <Link to="/admin/borrows" className="adb-quick-card">
          <span className="adb-quick-icon">📋</span>
          <span className="adb-quick-label">بررسی درخواست‌های امانت</span>
          {stats.pendingBorrows > 0 && (
            <span className="adb-badge">{stats.pendingBorrows}</span>
          )}
        </Link>
        <Link to="/admin/members" className="adb-quick-card">
          <span className="adb-quick-icon">👥</span>
          <span className="adb-quick-label">بررسی درخواست‌های عضویت</span>
          {stats.pendingMembers > 0 && (
            <span className="adb-badge">{stats.pendingMembers}</span>
          )}
        </Link>
        <Link to="/admin/books" className="adb-quick-card">
          <span className="adb-quick-icon">➕</span>
          <span className="adb-quick-label">افزودن کتاب جدید</span>
        </Link>
      </div>
    </div>
  );
};

export default AdminDashboard;
