import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { bookAPI, borrowAPI, meAPI } from "../services/api";
import { toPersian } from "../utils/persian";
import PhysicalBorrowCard from "../components/PhysicalBorrowCard";
import BorrowModal from "../components/BorrowModal";
import "./BookProfilePage.css";

const fmt = (d) => (d ? new Date(d).toLocaleDateString("fa-IR") : "—");

const BookProfilePage = () => {
  const { libraryId, bookId } = useParams();
  const navigate = useNavigate();
  const [book, setBook] = useState(null);
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [favorite, setFavorite] = useState(false);
  const [borrowOpen, setBorrowOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const [bookRes, borrowRes] = await Promise.all([
        bookAPI.getBook(libraryId, bookId),
        borrowAPI.getBorrows(libraryId).catch(() => ({ data: { data: [] } })),
      ]);
      setBook(bookRes.data?.data || bookRes.data);
      const all = borrowRes.data?.data || borrowRes.data || [];
      setBorrows(all.filter((b) => String(b.bookId) === String(bookId)));
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری اطلاعات کتاب");
    } finally {
      setLoading(false);
    }
  }, [libraryId, bookId]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    meAPI.favoriteIds()
      .then((res) => setFavorite((res.data?.data || res.data || []).map(String).includes(String(bookId))))
      .catch(() => {});
  }, [bookId]);

  const toggleFavorite = async () => {
    try {
      const res = await meAPI.toggleFavorite(bookId);
      setFavorite((res.data?.data || res.data)?.favorited);
    } catch { setError("خطا در ذخیره‌ی نشان"); }
  };

  const handleBorrowSubmit = async (details) => {
    try {
      await borrowAPI.createBorrow(libraryId, { bookId: Number(bookId), borrowType: "PHYSICAL", ...details });
      setBorrowOpen(false);
      load();
    } catch (err) {
      throw new Error(err.response?.data?.error || err.response?.data?.message || "خطا در ثبت درخواست");
    }
  };

  // One click does everything: request digital access if needed (it's granted
  // instantly, no librarian approval) and download the file — the user never
  // sees a separate "now click download" step.
  const handleDownload = async () => {
    try {
      setDownloading(true);
      const listRes = await bookAPI.listDigitalBooks(libraryId, bookId);
      const versions = listRes.data?.data || listRes.data || [];
      if (versions.length === 0) { setError("نسخه‌ی دیجیتالی موجود نیست"); return; }
      const v = versions[0];
      let res;
      try {
        res = await bookAPI.downloadDigitalBook(libraryId, bookId, v.id);
      } catch (err) {
        if (err.response?.status !== 401) throw err;
        // No approved access yet — request it (auto-approved instantly) and retry once.
        await borrowAPI.createBorrow(libraryId, { bookId: Number(bookId), borrowType: "DIGITAL" });
        res = await bookAPI.downloadDigitalBook(libraryId, bookId, v.id);
      }
      const url = URL.createObjectURL(res.data);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${book.title}.${(v.fileFormat || "pdf").toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
      load();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در دانلود");
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری...</div>;
  if (error) return <div className="error-message" style={{ margin: "2rem" }}>{error}</div>;
  if (!book) return null;

  const physical = borrows
    .filter((b) => b.borrowType === "PHYSICAL")
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
  const hasActivePhysical = physical && ["REQUESTED", "APPROVED", "RECEIVED"].includes(physical.status);

  return (
    <div className="bp-page">
      <button className="bp-back" onClick={() => navigate(`/libraries/${libraryId}/books`)}>→ بازگشت به کتاب‌ها</button>

      {/* Hero */}
      <div className="bp-hero">
        <div className="bp-cover">
          {book.coverImageUrl
            ? <img src={book.coverImageUrl} alt="جلد" />
            : <div className="bp-cover-ph"><span>{book.title}</span></div>}
        </div>
        <div className="bp-hero-info">
          <h1 className="bp-title">{book.title}</h1>
          <p className="bp-author">✍️ {book.author}</p>
          {book.publisher && <p className="bp-meta">🏢 {book.publisher}</p>}
          {book.publicationYear && <p className="bp-meta">📅 {toPersian(book.publicationYear)}</p>}
          {book.subjectNames?.length > 0 && (
            <div className="bp-subjects">
              {book.subjectNames.map((n, i) => <span key={i} className="bp-subject">🏷️ {n}</span>)}
            </div>
          )}
          <div className="bp-badges">
            <span className={`ver-badge ${book.totalCopiesCount > 0 ? "ver-badge--phys" : "ver-badge--off"}`}>
              {book.totalCopiesCount > 0 ? "✓" : "✗"} نسخه چاپی
            </span>
            <span className={`ver-badge ${book.hasDigitalVersions ? "ver-badge--digi" : "ver-badge--off"}`}>
              {book.hasDigitalVersions ? "✓" : "✗"} نسخه دیجیتال
            </span>
          </div>
          <div className="bp-avail">
            <span className={`avail-dot ${book.availableCopiesCount > 0 ? "avail-dot-green" : "avail-dot-red"}`} />
            {toPersian(book.availableCopiesCount)} از {toPersian(book.totalCopiesCount)} نسخه موجود
          </div>

          <div className="bp-actions">
            <button className={`bp-action bp-action--fav ${favorite ? "bp-action--fav-on" : ""}`} onClick={toggleFavorite}>
              {favorite ? "★ نشان‌شده" : "☆ نشان‌کردن"}
            </button>
            {!hasActivePhysical && book.availableCopiesCount > 0 && (
              <button className="bp-action bp-action--primary" onClick={() => setBorrowOpen(true)}>
                📚 درخواست امانت
              </button>
            )}
            {book.hasDigitalVersions && (
              <button className="bp-action bp-action--gold" onClick={handleDownload} disabled={downloading}>
                {downloading ? "در حال دانلود..." : "⬇ دانلود نسخه دیجیتال"}
              </button>
            )}
          </div>
        </div>
      </div>

      <BorrowModal
        isOpen={borrowOpen}
        book={book}
        onClose={() => setBorrowOpen(false)}
        onSubmit={handleBorrowSubmit}
      />

      {book.description && (
        <div className="bp-card">
          <h2 className="bp-section-title">معرفی کتاب</h2>
          <p className="bp-desc">{book.description}</p>
        </div>
      )}

      {/* My physical borrow status — full actions, shared with the borrows page */}
      <div className="bp-card">
        <h2 className="bp-section-title">📦 وضعیت امانت من</h2>
        {!physical ? (
          <p className="bp-empty">برای این کتاب امانت فعالی ندارید.</p>
        ) : (
          <PhysicalBorrowCard borrow={physical} libraryId={libraryId} onChanged={load} onError={setError} />
        )}
      </div>

    </div>
  );
};

export default BookProfilePage;
