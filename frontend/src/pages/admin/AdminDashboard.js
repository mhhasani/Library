import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { bookAPI, borrowAPI, libraryAdminAPI, libraryStatsAPI } from "../../services/api";
import { useLibrary } from "../../context/LibraryContext";
import "./AdminDashboard.css";

const AdminDashboard = () => {
  const { libraryId } = useParams();
  const { libraryName } = useLibrary() || {};

  const [stats, setStats] = useState({
    totalBooks: 0,
    pendingBorrows: 0,
    pendingMembers: 0,
    activeMembers: 0,
  });
  const [mostBorrowed, setMostBorrowed] = useState([]);
  const [underused, setUnderused] = useState([]);
  const [userActivity, setUserActivity] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!libraryId) return;
    const load = async () => {
      try {
        setLoading(true);
        const [booksRes, pendingBorrowsRes, membersRes, mostBorrowedRes, underusedRes, userActivityRes] = await Promise.all([
          bookAPI.getBooks(libraryId, { page: 0, size: 1 }),
          borrowAPI.getPendingBorrows(libraryId),
          libraryAdminAPI.getMembers(libraryId),
          libraryStatsAPI.getMostBorrowed(libraryId, 5).catch(() => ({ data: { data: [] } })),
          libraryStatsAPI.getUnderused(libraryId).catch(() => ({ data: { data: [] } })),
          libraryStatsAPI.getUserActivity(libraryId, 5).catch(() => ({ data: { data: [] } })),
        ]);

        const books = booksRes.data?.data;
        const totalBooks =
          books?.totalElements ?? (Array.isArray(books) ? books.length : 0);

        const members = membersRes.data?.data || membersRes.data || [];
        const pendingMembers = members.filter((m) => m.status === "PENDING").length;
        const activeMembers = members.filter((m) => m.status === "APPROVED").length;

        const pendingBorrows = (pendingBorrowsRes.data?.data || []).length;

        setStats({ totalBooks, pendingBorrows, pendingMembers, activeMembers });
        setMostBorrowed(mostBorrowedRes.data?.data || []);
        setUnderused(underusedRes.data?.data || []);
        setUserActivity(userActivityRes.data?.data || []);
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
        <p className="ap-subtitle">خلاصه وضعیت کتابخانه «{libraryName || "…"}»</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Overview Stats */}
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
        <Link to={`/libraries/${libraryId}/admin/borrows`} className="adb-quick-card">
          <span className="adb-quick-icon">📋</span>
          <span className="adb-quick-label">بررسی درخواست‌های امانت</span>
          {stats.pendingBorrows > 0 && (
            <span className="adb-badge">{stats.pendingBorrows}</span>
          )}
        </Link>
        <Link to={`/libraries/${libraryId}/admin/members`} className="adb-quick-card">
          <span className="adb-quick-icon">👥</span>
          <span className="adb-quick-label">بررسی درخواست‌های عضویت</span>
          {stats.pendingMembers > 0 && (
            <span className="adb-badge">{stats.pendingMembers}</span>
          )}
        </Link>
        <Link to={`/libraries/${libraryId}/admin/books`} className="adb-quick-card">
          <span className="adb-quick-icon">➕</span>
          <span className="adb-quick-label">افزودن کتاب جدید</span>
        </Link>
      </div>

      {/* Detailed Reports */}
      <div className="adb-reports-grid">
        {/* Most Borrowed */}
        <div className="adb-report-card">
          <h3 className="adb-report-title">📚 پرامانت‌ترین کتاب‌ها</h3>
          {mostBorrowed.length === 0 ? (
            <p className="adb-report-empty">هنوز امانتی ثبت نشده</p>
          ) : (
            <ol className="adb-report-list">
              {mostBorrowed.map((item, i) => (
                <li key={item.bookId} className="adb-report-item">
                  <span className="adb-report-rank">{i + 1}</span>
                  <span className="adb-report-name">{item.bookTitle}</span>
                  <span className="adb-report-count">{item.borrowCount} امانت</span>
                </li>
              ))}
            </ol>
          )}
        </div>

        {/* User Activity */}
        <div className="adb-report-card">
          <h3 className="adb-report-title">👤 فعال‌ترین اعضا</h3>
          {userActivity.length === 0 ? (
            <p className="adb-report-empty">داده‌ای موجود نیست</p>
          ) : (
            <ol className="adb-report-list">
              {userActivity.map((item, i) => (
                <li key={item.userId} className="adb-report-item">
                  <span className="adb-report-rank">{i + 1}</span>
                  <span className="adb-report-name">{item.userEmail}</span>
                  <span className="adb-report-count">{item.borrowCount} امانت</span>
                </li>
              ))}
            </ol>
          )}
        </div>

        {/* Underused Books */}
        <div className="adb-report-card">
          <h3 className="adb-report-title">💤 کتاب‌های کم‌استفاده</h3>
          <p className="adb-report-subtitle">هرگز امانت داده نشده</p>
          {underused.length === 0 ? (
            <p className="adb-report-empty">همه کتاب‌ها حداقل یک‌بار امانت داده شده‌اند</p>
          ) : (
            <ul className="adb-report-list">
              {underused.slice(0, 5).map((item) => (
                <li key={item.bookId} className="adb-report-item">
                  <span className="adb-report-name">{item.bookTitle}</span>
                  <span className="adb-report-count">{item.totalCopies} نسخه</span>
                </li>
              ))}
              {underused.length > 5 && (
                <li className="adb-report-item adb-report-more">
                  و {underused.length - 5} کتاب دیگر...
                </li>
              )}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
