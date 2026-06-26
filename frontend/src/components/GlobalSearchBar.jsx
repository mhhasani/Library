import React, { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { searchAPI } from "../services/api";
import { useDebounce } from "../hooks/useDebounce";
import "./GlobalSearchBar.css";

const GlobalSearchBar = () => {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [filter, setFilter] = useState("all"); // all | physical | digital
  const debounced = useDebounce(query, 400);
  const ref = useRef(null);

  const runSearch = useCallback(async (q) => {
    if (!q || q.trim().length < 2) { setResults([]); setOpen(false); return; }
    try {
      setLoading(true);
      const res = await searchAPI.global(q.trim());
      const page = res.data?.data || res.data;
      const list = page?.content || page || [];
      setResults(Array.isArray(list) ? list : []);
      setOpen(true);
    } catch {
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { runSearch(debounced); }, [debounced, runSearch]);

  useEffect(() => {
    const onClick = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const goToBook = (b) => {
    setOpen(false);
    navigate(`/libraries/${b.libraryId}/books/${b.id}`);
  };

  const hasPhysical = (b) => (b.totalCopiesCount || 0) > 0;
  const hasDigital = (b) => !!b.hasDigitalVersions;
  const filtered = results.filter((b) =>
    filter === "physical" ? hasPhysical(b) : filter === "digital" ? hasDigital(b) : true);

  return (
    <div className="gsb" ref={ref}>
      <div className="gsb-box">
        <span className="gsb-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
        </span>
        <input
          className="gsb-input"
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => results.length && setOpen(true)}
          placeholder="جستجوی کتاب در همهٔ کتابخانه‌ها… (عنوان، نویسنده، ناشر)"
        />
        {loading && <span className="gsb-spinner" aria-hidden="true" />}
      </div>

      {open && (
        <div className="gsb-results">
          <div className="gsb-filters">
            {[
              { k: "all", label: "همه" },
              { k: "physical", label: "📦 فیزیکی" },
              { k: "digital", label: "💻 دیجیتال" },
            ].map((f) => (
              <button
                key={f.k}
                className={`gsb-filter ${filter === f.k ? "gsb-filter--active" : ""}`}
                onClick={() => setFilter(f.k)}
              >
                {f.label}
              </button>
            ))}
          </div>
          {filtered.length === 0 ? (
            <div className="gsb-empty">نتیجه‌ای یافت نشد</div>
          ) : (
            filtered.map((b) => (
              <button key={`${b.libraryId}-${b.id}`} className="gsb-item" onClick={() => goToBook(b)}>
                <span className="gsb-item-cover">📘</span>
                <span className="gsb-item-body">
                  <span className="gsb-item-title">{b.title}</span>
                  <span className="gsb-item-meta">
                    {b.author}{b.author && b.libraryName ? " · " : ""}
                    <span className="gsb-item-lib">🏛️ {b.libraryName}</span>
                  </span>
                </span>
                <span className="gsb-tags">
                  <span className={`gsb-tag ${hasPhysical(b) ? "gsb-tag--ok" : "gsb-tag--off"}`}>
                    {hasPhysical(b) ? "✓" : "✗"} فیزیکی
                  </span>
                  <span className={`gsb-tag ${hasDigital(b) ? "gsb-tag--info" : "gsb-tag--off"}`}>
                    {hasDigital(b) ? "✓" : "✗"} دیجیتال
                  </span>
                </span>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
};

export default GlobalSearchBar;
