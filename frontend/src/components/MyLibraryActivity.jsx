import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { meAPI } from "../services/api";
import { useDebounce } from "../hooks/useDebounce";
import { toPersianNum } from "../utils/persian";
import PhysicalBorrowCard from "./PhysicalBorrowCard";
import Pagination from "./Pagination";
import "./MyLibraryActivity.css";

const TABS = [
  { key: "borrows", label: "📦 امانت‌های من" },
  { key: "downloads", label: "💻 دانلودهای من" },
  { key: "favorites", label: "⭐ نشان‌شده‌ها" },
];

const fmt = (d) => (d ? new Date(d).toLocaleDateString("fa-IR") : "—");

const MyLibraryActivity = ({ initialTab = "borrows" }) => {
  const navigate = useNavigate();
  const [tab, setTab] = useState(initialTab);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const debounced = useDebounce(search, 400);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      let res;
      if (tab === "favorites") {
        res = await meAPI.favorites({ search: debounced || undefined, page, size: 12 });
      } else {
        res = await meAPI.borrows({
          type: tab === "downloads" ? "DIGITAL" : "PHYSICAL",
          search: debounced || undefined, page, size: 8,
        });
      }
      const d = res.data?.data || res.data;
      setData({ content: d?.content || [], totalPages: d?.totalPages ?? 0 });
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری");
    } finally {
      setLoading(false);
    }
  }, [tab, debounced, page]);

  useEffect(() => { fetchData(); }, [fetchData]);
  useEffect(() => { setPage(0); setSearch(""); }, [tab]);
  useEffect(() => { setPage(0); }, [debounced]);

  return (
    <div className="mla">
      <div className="mla-tabs">
        {TABS.map((t) => (
          <button key={t.key} className={`mla-tab ${tab === t.key ? "mla-tab--active" : ""}`} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="abr-toolbar">
        <input
          className="abr-search"
          placeholder={tab === "favorites" ? "🔍 جستجوی کتاب‌های نشان‌شده..." : "🔍 جستجو بر اساس کد رهگیری، عنوان کتاب یا کتابخانه..."}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {error && <div className="error-message">{error}</div>}

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : data.content.length === 0 ? (
        <div className="empty-state"><span className="empty-icon">📋</span><p>موردی یافت نشد</p></div>
      ) : tab === "borrows" ? (
        <div className="mla-list">
          {data.content.map((b) => (
            <PhysicalBorrowCard key={b.id} borrow={b} libraryId={b.libraryId} onChanged={fetchData} onError={setError} />
          ))}
        </div>
      ) : tab === "downloads" ? (
        <div className="table-wrapper">
          <table className="modern-table">
            <thead><tr><th>کتاب</th><th>کتابخانه</th><th>تاریخ دسترسی</th></tr></thead>
            <tbody>
              {data.content.map((b) => (
                <tr key={b.id}>
                  <td className="borrow-book-title">{b.bookTitle}</td>
                  <td style={{ fontSize: "0.82rem" }}>🏛️ {b.libraryName}</td>
                  <td style={{ fontSize: "0.82rem", color: "#9ca3af" }}>{fmt(b.borrowDate || b.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="mla-fav-grid">
          {data.content.map((bk) => (
            <button key={`${bk.libraryId}-${bk.id}`} className="mla-fav-card" onClick={() => navigate(`/libraries/${bk.libraryId}/books/${bk.id}`)}>
              <span className="mla-fav-cover">{bk.coverImageUrl ? <img src={bk.coverImageUrl} alt="" /> : "📘"}</span>
              <span className="mla-fav-body">
                <span className="mla-fav-title">{bk.title}</span>
                <span className="mla-fav-meta">{bk.author} · 🏛️ {bk.libraryName}</span>
                <span className="mla-fav-badges">
                  {bk.totalCopiesCount > 0 && <span className="ver-badge ver-badge--phys">✓ چاپی</span>}
                  {bk.hasDigitalVersions && <span className="ver-badge ver-badge--digi">✓ دیجیتال</span>}
                </span>
              </span>
            </button>
          ))}
        </div>
      )}

      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
};

export default MyLibraryActivity;
