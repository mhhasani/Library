import React from "react";
import { toPersianNum } from "../utils/persian";
import "./Pagination.css";

/**
 * Reusable pager. `page` is zero-based. Renders nothing when there's a single page.
 *  page, totalPages, onChange(nextZeroBasedPage)
 */
const Pagination = ({ page, totalPages, onChange }) => {
  if (!totalPages || totalPages <= 1) return null;
  const go = (p) => { if (p >= 0 && p < totalPages && p !== page) onChange(p); };

  // window of page numbers around the current page
  const nums = [];
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const end = Math.min(totalPages, start + 5);
  for (let i = start; i < end; i++) nums.push(i);

  return (
    <div className="pgn">
      <button className="pgn-btn" onClick={() => go(page - 1)} disabled={page === 0}>قبلی</button>
      {start > 0 && (
        <>
          <button className="pgn-num" onClick={() => go(0)}>{toPersianNum(1)}</button>
          {start > 1 && <span className="pgn-dots">…</span>}
        </>
      )}
      {nums.map((n) => (
        <button key={n} className={`pgn-num ${n === page ? "pgn-num--active" : ""}`} onClick={() => go(n)}>
          {toPersianNum(n + 1)}
        </button>
      ))}
      {end < totalPages && (
        <>
          {end < totalPages - 1 && <span className="pgn-dots">…</span>}
          <button className="pgn-num" onClick={() => go(totalPages - 1)}>{toPersianNum(totalPages)}</button>
        </>
      )}
      <button className="pgn-btn" onClick={() => go(page + 1)} disabled={page >= totalPages - 1}>بعدی</button>
    </div>
  );
};

export default Pagination;
