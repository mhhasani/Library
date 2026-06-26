import React, { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import { borrowAPI } from "../services/api";
import { useLibrary } from "../context/LibraryContext";
import { toPersianNum } from "../utils/persian";
import PhysicalBorrowCard, { isOverdue, ACTIVE } from "../components/PhysicalBorrowCard";
import "./BorrowsPage.css";
import "./PhysicalBorrowsPage.css";

const FINISHED = ["RETURNED", "REJECTED", "CANCELLED", "EXPIRED"];

const PhysicalBorrowsPage = () => {
  const { libraryId } = useParams();
  const { libraryName } = useLibrary() || {};
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [tab, setTab] = useState("active");
  const [search, setSearch] = useState("");

  const fetchBorrows = useCallback(async () => {
    try {
      setLoading(true);
      const res = await borrowAPI.getBorrows(libraryId, { type: "PHYSICAL" });
      setBorrows(res.data.data || res.data || []);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری امانت‌ها");
    } finally {
      setLoading(false);
    }
  }, [libraryId]);

  useEffect(() => { fetchBorrows(); }, [fetchBorrows]);

  if (loading) return <div className="loading">در حال بارگذاری امانت‌ها...</div>;

  const overdueCount = borrows.filter(isOverdue).length;
  const q = search.trim().toLowerCase();
  const visible = borrows
    .filter((b) => (tab === "active" ? ACTIVE.includes(b.status) : FINISHED.includes(b.status)))
    .filter((b) => !q || (b.bookTitle || "").toLowerCase().includes(q)
      || (b.courierName || "").toLowerCase().includes(q)
      || (b.copyUniqueCode || "").toLowerCase().includes(q));

  return (
    <div className="borrows-page">
      <div className="borrows-header">
        <div className="borrows-header-inner">
          <div>
            <h1 className="borrows-main-title">📦 امانت‌ها من</h1>
            {libraryName && <p className="borrows-library-name">🏛️ {libraryName}</p>}
          </div>
          <div className="borrows-count-badge">{toPersianNum(visible.length)} رکورد</div>
        </div>
      </div>

      <div className="borrows-body">
        {error && <div className="error-message">{error}</div>}

        {overdueCount > 0 && tab === "active" && (
          <div className="pb-banner pb-banner--danger">
            ⏰ مهلت {toPersianNum(overdueCount)} امانت به پایان رسیده است. لطفاً درخواست برگرداندن کتاب ثبت کنید.
          </div>
        )}

        <div className="filter-tabs">
          <button className={`filter-tab ${tab === "active" ? "active" : ""}`} onClick={() => setTab("active")}>
            🔄 جاری
          </button>
          <button className={`filter-tab ${tab === "finished" ? "active" : ""}`} onClick={() => setTab("finished")}>
            ✅ به اتمام رسیده
          </button>
        </div>

        <div className="abr-toolbar">
          <input
            className="abr-search"
            placeholder="🔍 جستجوی کتاب، پیک یا کد نسخه..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {visible.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">📋</span>
            <p>موردی یافت نشد</p>
          </div>
        ) : (
          <div className="pb-list">
            {visible.map((b) => (
              <PhysicalBorrowCard
                key={b.id}
                borrow={b}
                libraryId={libraryId}
                onChanged={fetchBorrows}
                onError={setError}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PhysicalBorrowsPage;
