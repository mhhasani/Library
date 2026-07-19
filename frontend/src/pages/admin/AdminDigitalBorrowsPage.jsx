import React, { useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { borrowAPI } from "../../services/api";
import { useDebounce } from "../../hooks/useDebounce";
import Pagination from "../../components/Pagination";
import "./AdminBorrowsPage.css";

const STATUS_LABEL = { APPROVED: "فعال", RETURNED: "پایان‌یافته", EXPIRED: "منقضی" };
const STATUS_CLASS = { APPROVED: "badge-success", RETURNED: "badge-muted", EXPIRED: "badge-muted" };

/** Digital access is instant — read-only, searchable, paginated access log. */
const AdminDigitalBorrowsPage = () => {
  const { libraryId } = useParams();
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const debounced = useDebounce(search, 400);

  const fetchRows = useCallback(async () => {
    if (!libraryId) return;
    try {
      setLoading(true);
      const res = await borrowAPI.searchLibraryBorrows(libraryId, {
        type: "DIGITAL", search: debounced || undefined, page, size: 15,
      });
      const data = res.data?.data || res.data;
      setRows(data?.content || []);
      setTotalPages(data?.totalPages ?? 0);
      setError("");
    } catch {
      setError("خطا در بارگذاری گزارش دسترسی دیجیتال");
    } finally {
      setLoading(false);
    }
  }, [libraryId, debounced, page]);

  useEffect(() => { fetchRows(); }, [fetchRows]);
  useEffect(() => { setPage(0); }, [debounced]);

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">💻 دسترسی‌های دیجیتال</h1>
        <p className="ap-subtitle">دسترسی دیجیتال آنی است؛ این صفحه گزارش دسترسی‌هاست.</p>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="abr-toolbar">
        <input
          className="abr-search"
          placeholder="🔍 جستجو بر اساس نام/ایمیل کاربر یا عنوان کتاب..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : rows.length === 0 ? (
        <div className="empty-state"><span className="empty-icon">📋</span><p>موردی یافت نشد</p></div>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr><th>کتاب</th><th>کاربر</th><th>وضعیت</th><th>تاریخ دسترسی</th></tr>
              </thead>
              <tbody>
                {rows.map((b) => (
                  <tr key={b.id}>
                    <td><div className="abr-book-title">{b.bookTitle}</div></td>
                    <td style={{ fontSize: "0.82rem" }}>{b.userFullName || b.userEmail}</td>
                    <td>
                      <span className={`badge ${STATUS_CLASS[b.status] || "badge-muted"}`}>
                        {STATUS_LABEL[b.status] || b.status}
                      </span>
                    </td>
                    <td className="abr-copy">
                      {b.borrowDate ? new Date(b.borrowDate).toLocaleDateString("fa-IR")
                        : b.createdAt ? new Date(b.createdAt).toLocaleDateString("fa-IR") : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
};

export default AdminDigitalBorrowsPage;
