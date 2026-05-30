import React, { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import { borrowAPI } from "../services/api";
import { useLibrary } from "../context/LibraryContext";
import { toPersian, toPersianNum } from "../utils/persian";
import "./BorrowsPage.css";

const STATUS_LABELS = {
  ACTIVE:    "فعال",
  RETURNED:  "برگشت داده شده",
  OVERDUE:   "تأخیر دار",
  APPROVED:  "تأیید شده",
  PENDING:   "در انتظار",
  REQUESTED: "در انتظار تأیید",
  REJECTED:  "رد شده",
  EXPIRED:   "منقضی",
};

const STATUS_CLASS = {
  ACTIVE:    "badge-success",
  RETURNED:  "badge-muted",
  OVERDUE:   "badge-danger",
  APPROVED:  "badge-success",
  PENDING:   "badge-warning",
  REQUESTED: "badge-warning",
  REJECTED:  "badge-danger",
  EXPIRED:   "badge-muted",
};

const BorrowsPage = () => {
  const { libraryId } = useParams();
  const { libraryName } = useLibrary() || {};
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("all");

  const fetchBorrows = useCallback(async () => {
    try {
      setLoading(true);
      const response = await borrowAPI.getBorrows(libraryId, {
        status: filter === "all" ? undefined : filter,
      });
      setBorrows(response.data.data || response.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری امانت‌ها");
    } finally {
      setLoading(false);
    }
  }, [filter, libraryId]);

  useEffect(() => { fetchBorrows(); }, [fetchBorrows]);

  const handleReturnBook = async (borrowId) => {
    try {
      await borrowAPI.returnBorrow(libraryId, borrowId);
      fetchBorrows();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بازگشت کتاب");
    }
  };

  const isOverdue = (dueDate) => new Date(dueDate) < new Date();

  const tabs = [
    { key: "all",       label: "همه" },
    { key: "REQUESTED", label: "⏳ در انتظار" },
    { key: "APPROVED",  label: "✅ فعال" },
    { key: "RETURNED",  label: "📦 برگشت داده شده" },
    { key: "REJECTED",  label: "❌ رد شده" },
  ];

  if (loading) return <div className="loading">در حال بارگذاری امانت‌ها...</div>;

  return (
    <div className="borrows-page">
      {/* Header */}
      <div className="borrows-header">
        <div className="borrows-header-inner">
          <div>
            <h1 className="borrows-main-title">امانت‌های من</h1>
            {libraryName && (
              <p className="borrows-library-name">🏛️ {libraryName}</p>
            )}
          </div>
          <div className="borrows-count-badge">
            {toPersianNum(borrows.length)} رکورد
          </div>
        </div>
      </div>

      <div className="borrows-body">
        {error && <div className="error-message">{error}</div>}

        {/* Filter Tabs */}
        <div className="filter-tabs">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`filter-tab ${filter === tab.key ? "active" : ""}`}
              onClick={() => setFilter(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Table */}
        {borrows.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">📋</span>
            <p>هیچ رکوردی یافت نشد</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr>
                  <th>عنوان کتاب</th>
                  <th>نوع</th>
                  <th>تاریخ درخواست</th>
                  <th>موعد تحویل</th>
                  <th>وضعیت</th>
                  <th>عملیات</th>
                </tr>
              </thead>
              <tbody>
                {borrows.map((borrow) => {
                  const overdue = borrow.isOverdue ||
                    (borrow.status === "APPROVED" && borrow.dueDate && isOverdue(borrow.dueDate));
                  return (
                    <tr key={borrow.id}>
                      <td className="borrow-book-title">
                        {borrow.bookTitle || "—"}
                        {borrow.copyNumber && (
                          <div style={{ fontSize: "0.75rem", color: "#9ca3af" }}>نسخه {toPersian(borrow.copyNumber)}</div>
                        )}
                        {borrow.status === "REJECTED" && borrow.rejectionReason && (
                          <div style={{ fontSize: "0.75rem", color: "#dc2626", marginTop: "0.2rem" }}>
                            دلیل رد: {borrow.rejectionReason}
                          </div>
                        )}
                      </td>
                      <td>
                        <span className="badge badge-info">
                          {borrow.borrowType === "PHYSICAL" ? "فیزیکی" : "دیجیتال"}
                        </span>
                      </td>
                      <td style={{ fontSize: "0.82rem", color: "#9ca3af" }}>
                        {borrow.createdAt ? new Date(borrow.createdAt).toLocaleDateString("fa-IR") : "—"}
                      </td>
                      <td className={overdue ? "td-overdue" : ""} style={{ fontSize: "0.82rem" }}>
                        {borrow.dueDate ? (
                          <>
                            {new Date(borrow.dueDate).toLocaleDateString("fa-IR")}
                            {overdue && <span className="overdue-tag">تأخیر</span>}
                          </>
                        ) : "—"}
                      </td>
                      <td>
                        <span className={`badge ${STATUS_CLASS[borrow.status] || "badge-muted"}`}>
                          {STATUS_LABELS[borrow.status] || borrow.status}
                        </span>
                      </td>
                      <td>
                        {borrow.status === "APPROVED" ? (
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handleReturnBook(borrow.id)}
                          >
                            بازگشت کتاب
                          </button>
                        ) : (
                          <span className="td-none">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default BorrowsPage;
