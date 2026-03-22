import React, { useState, useEffect, useCallback } from "react";
import { borrowAPI } from "../services/api";
import "./BorrowsPage.css";

const STATUS_LABELS = {
  ACTIVE:   "فعال",
  RETURNED: "برگشت داده شده",
  OVERDUE:  "تأخیر دار",
  APPROVED: "تأیید شده",
  PENDING:  "در انتظار",
  REJECTED: "رد شده",
};

const STATUS_CLASS = {
  ACTIVE:   "badge-success",
  RETURNED: "badge-muted",
  OVERDUE:  "badge-danger",
  APPROVED: "badge-success",
  PENDING:  "badge-warning",
  REJECTED: "badge-danger",
};

const BorrowsPage = () => {
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("all");
  const [libraryId] = useState(localStorage.getItem("activeLibraryId") || "");
  const [libraryName] = useState(localStorage.getItem("activeLibraryName") || "");

  const fetchBorrows = useCallback(async () => {
    try {
      setLoading(true);
      if (!libraryId) {
        setBorrows([]);
        setError("ابتدا یک کتابخانه را فعال کنید.");
        return;
      }
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
      if (!libraryId) { setError("ابتدا کتابخانه را انتخاب کنید."); return; }
      await borrowAPI.returnBorrow(libraryId, borrowId);
      fetchBorrows();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بازگشت کتاب");
    }
  };

  const isOverdue = (dueDate) => new Date(dueDate) < new Date();

  const formatDate = (dateStr) =>
    new Date(dateStr).toLocaleDateString("fa-IR");

  const tabs = [
    { key: "all",      label: "همه" },
    { key: "ACTIVE",   label: "فعال" },
    { key: "RETURNED", label: "برگشت داده شده" },
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
            {borrows.length} رکورد
          </div>
        </div>
      </div>

      <div className="borrows-body">
        {!libraryId && (
          <div className="info-banner">
            📍 لطفاً ابتدا از صفحه کتابخانه‌ها یک کتابخانه فعال کنید.
          </div>
        )}

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
                  <th>شناسه کتاب</th>
                  <th>تاریخ امانت</th>
                  <th>تاریخ بازگشت</th>
                  <th>وضعیت</th>
                  <th>عملیات</th>
                </tr>
              </thead>
              <tbody>
                {borrows.map((borrow) => {
                  const overdue =
                    borrow.isOverdue ||
                    (borrow.status === "ACTIVE" && isOverdue(borrow.dueDate));
                  return (
                    <tr key={borrow.id}>
                      <td className="borrow-book-title">{borrow.bookTitle || "—"}</td>
                      <td className="borrow-id">{borrow.bookId || "—"}</td>
                      <td>{formatDate(borrow.borrowDate)}</td>
                      <td className={overdue ? "td-overdue" : ""}>
                        {formatDate(borrow.dueDate)}
                        {overdue && <span className="overdue-tag">تأخیر</span>}
                      </td>
                      <td>
                        <span className={`badge ${STATUS_CLASS[borrow.status] || "badge-muted"}`}>
                          {STATUS_LABELS[borrow.status] || borrow.status}
                        </span>
                      </td>
                      <td>
                        {borrow.status === "ACTIVE" ? (
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handleReturnBook(borrow.id)}
                          >
                            بازگشت
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
