import React, { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import { borrowAPI } from "../services/api";
import { useLibrary } from "../context/LibraryContext";
import { toPersianNum } from "../utils/persian";
import "./BorrowsPage.css";

const STATUS_LABELS = { APPROVED: "فعال", RETURNED: "پایان‌یافته", EXPIRED: "منقضی" };
const STATUS_CLASS = { APPROVED: "badge-success", RETURNED: "badge-muted", EXPIRED: "badge-muted" };

/** Digital access is instant — this page is the user's read-only digital access log. */
const DigitalBorrowsPage = () => {
  const { libraryId } = useParams();
  const { libraryName } = useLibrary() || {};
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  const fetchRows = useCallback(async () => {
    try {
      setLoading(true);
      const res = await borrowAPI.getBorrows(libraryId, { type: "DIGITAL" });
      setRows(res.data.data || res.data || []);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری دسترسی‌ها");
    } finally {
      setLoading(false);
    }
  }, [libraryId]);

  useEffect(() => { fetchRows(); }, [fetchRows]);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div className="borrows-page">
      <div className="borrows-header">
        <div className="borrows-header-inner">
          <div>
            <h1 className="borrows-main-title">💻 دسترسی‌های دیجیتال من</h1>
            {libraryName && <p className="borrows-library-name">🏛️ {libraryName}</p>}
          </div>
          <div className="borrows-count-badge">{toPersianNum(rows.length)} رکورد</div>
        </div>
      </div>

      <div className="borrows-body">
        {error && <div className="error-message">{error}</div>}
        <p style={{ fontSize: "0.85rem", color: "#6b7280", marginBottom: "1rem" }}>
          دسترسی به نسخه‌های دیجیتال آنی است؛ برای دانلود به صفحه‌ی «کتاب‌ها» مراجعه کنید.
        </p>

        <div className="abr-toolbar">
          <input
            className="abr-search"
            placeholder="🔍 جستجوی عنوان کتاب..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {rows.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">📋</span>
            <p>هنوز دسترسی دیجیتالی ندارید</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr>
                  <th>عنوان کتاب</th>
                  <th>تاریخ دسترسی</th>
                  <th>وضعیت</th>
                </tr>
              </thead>
              <tbody>
                {rows.filter((b) => {
                  const q = search.trim().toLowerCase();
                  return !q || (b.bookTitle || "").toLowerCase().includes(q);
                }).map((b) => (
                  <tr key={b.id}>
                    <td className="borrow-book-title">{b.bookTitle || "—"}</td>
                    <td style={{ fontSize: "0.82rem", color: "#9ca3af" }}>
                      {b.borrowDate
                        ? new Date(b.borrowDate).toLocaleDateString("fa-IR")
                        : b.createdAt
                        ? new Date(b.createdAt).toLocaleDateString("fa-IR")
                        : "—"}
                    </td>
                    <td>
                      <span className={`badge ${STATUS_CLASS[b.status] || "badge-muted"}`}>
                        {STATUS_LABELS[b.status] || b.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DigitalBorrowsPage;
