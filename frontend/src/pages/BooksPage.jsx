import { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useSearchParams, useNavigate } from "react-router-dom";
import { bookAPI, borrowAPI, subjectAPI, meAPI } from "../services/api";
import { useLibrary } from "../context/LibraryContext";
import { useDebounce } from "../hooks/useDebounce";
import BorrowModal from "../components/BorrowModal";
import ConfirmDialog from "../components/ConfirmDialog";
import { toPersian, toPersianNum } from "../utils/persian";
import "./BooksPage.css";

const BooksPage = () => {
  const { libraryId } = useParams();
  const { libraryName, borrowDuration } = useLibrary() || {};

  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState(searchParams.get("q") || "");
  const [searching, setSearching] = useState(false);
  const isFirstLoad = useRef(true);

  // Advanced filters
  const [filterSubjectId, setFilterSubjectId] = useState("");
  const [filterYearFrom, setFilterYearFrom] = useState("");
  const [filterYearTo, setFilterYearTo] = useState("");
  const [availFilter, setAvailFilter] = useState("all"); // all | physical | digital
  const [availableSubjects, setAvailableSubjects] = useState([]);

  // Debounce text-based filters — any new text filter added here is automatically debounced
  const debouncedSearchTerm = useDebounce(searchTerm, 1500);
  const debouncedYearFrom = useDebounce(filterYearFrom, 1500);
  const debouncedYearTo = useDebounce(filterYearTo, 1500);

  // Physical borrow modal
  const [selectedBook, setSelectedBook] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editBorrow, setEditBorrow] = useState(null);

  // Confirmation dialog (generic)
  const [confirm, setConfirm] = useState(null); // { title, message, variant, confirmLabel, onConfirm }
  const [confirmLoading, setConfirmLoading] = useState(false);

  // Favorites (starred books)
  const [favoriteIds, setFavoriteIds] = useState(new Set());

  // borrowMap[bookId] = { hasActivePhysical, hasActiveDigital, hasApprovedDigital }
  const [borrowMap, setBorrowMap] = useState({});

  // Download format picker
  const [downloadBook, setDownloadBook] = useState(null);
  const [downloadFormats, setDownloadFormats] = useState([]);
  const [downloadFormatsLoading, setDownloadFormatsLoading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  // Load available subjects on mount
  useEffect(() => {
    subjectAPI.getSubjects(libraryId)
      .then((res) => setAvailableSubjects(res.data?.data || []))
      .catch(() => {});
  }, [libraryId]);

  const hasActiveFilters = searchTerm || filterSubjectId || filterYearFrom || filterYearTo;

  const fetchBooks = useCallback(async () => {
    try {
      if (isFirstLoad.current) setLoading(true);
      else setSearching(true);

      const params = { page: 0, size: 200 };
      if (debouncedSearchTerm.trim()) params.query = debouncedSearchTerm.trim();
      if (filterSubjectId) params.subjectId = filterSubjectId;
      if (debouncedYearFrom) params.yearFrom = parseInt(debouncedYearFrom);
      if (debouncedYearTo) params.yearTo = parseInt(debouncedYearTo);

      const [booksRes, borrowsRes] = await Promise.all([
        bookAPI.searchBooks(libraryId, params),
        borrowAPI.getBorrows(libraryId).catch(() => ({ data: { data: [] } })),
      ]);

      const payload = booksRes.data.data || booksRes.data;
      const list = Array.isArray(payload) ? payload : payload?.content || [];
      setBooks(list);

      const myBorrows = borrowsRes.data?.data || borrowsRes.data || [];
      const map = {};
      myBorrows.forEach((b) => {
        const bid = b.bookId;
        if (!map[bid]) map[bid] = {};
        const isActive = (b.status === "REQUESTED" || b.status === "APPROVED" || b.status === "RECEIVED") && !b.returnDate;
        if (b.borrowType === "PHYSICAL" && isActive) {
          map[bid].hasActivePhysical = true;
          if (b.status === "REQUESTED") map[bid].pendingPhysical = b;
        }
        if (b.borrowType === "DIGITAL" && isActive) {
          map[bid].hasActiveDigital = true;
          if (b.status === "APPROVED") map[bid].hasApprovedDigital = true;
        }
      });
      setBorrowMap(map);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری کتاب‌ها");
    } finally {
      setLoading(false);
      setSearching(false);
      isFirstLoad.current = false;
    }
  }, [debouncedSearchTerm, filterSubjectId, debouncedYearFrom, debouncedYearTo, libraryId]);

  useEffect(() => { fetchBooks(); }, [fetchBooks]);

  // Load the user's favorite book ids (for the star state)
  useEffect(() => {
    meAPI.favoriteIds()
      .then((res) => setFavoriteIds(new Set(res.data?.data || res.data || [])))
      .catch(() => {});
  }, [libraryId]);

  const toggleFavorite = async (e, bookId) => {
    e.stopPropagation();
    try {
      const res = await meAPI.toggleFavorite(bookId);
      const fav = (res.data?.data || res.data)?.favorited;
      setFavoriteIds((prev) => {
        const next = new Set(prev);
        if (fav) next.add(bookId); else next.delete(bookId);
        return next;
      });
    } catch {
      setError("خطا در ذخیره‌ی نشان");
    }
  };

  const clearFilters = () => {
    setSearchTerm("");
    setFilterSubjectId("");
    setFilterYearFrom("");
    setFilterYearTo("");
  };

  // Submit handler for the physical borrow modal (create OR edit)
  const handlePhysicalSubmit = async (details) => {
    try {
      if (editBorrow) {
        await borrowAPI.updateRequest(libraryId, editBorrow.id, details);
      } else {
        await borrowAPI.createBorrow(libraryId, {
          bookId: selectedBook.id,
          borrowType: "PHYSICAL",
          ...details,
        });
      }
      setIsModalOpen(false);
      setSelectedBook(null);
      setEditBorrow(null);
      fetchBooks();
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || "خطا در ثبت درخواست";
      throw new Error(msg); // surfaced inside the modal
    }
  };

  // Reserve when no copy available (with confirmation)
  const handleReserve = (book) => {
    setConfirm({
      title: "رزرو کتاب",
      message: `کتاب «${book.title}» موجود نیست. آیا می‌خواهید آن را رزرو کنید تا هنگام موجود شدن در نوبت قرار بگیرید؟`,
      confirmLabel: "بله، رزرو کن",
      variant: "primary",
      onConfirm: async () => {
        await borrowAPI.reserveBook(libraryId, book.id);
        fetchBooks();
      },
    });
  };

  // Digital borrow request (with confirmation)
  const handleRequestDigital = (book) => {
    setConfirm({
      title: "درخواست دانلود",
      message: `آیا درخواست دانلود کتاب «${book.title}» را ثبت می‌کنید؟`,
      confirmLabel: "بله، ثبت کن",
      variant: "primary",
      onConfirm: async () => {
        await borrowAPI.createBorrow(libraryId, { bookId: book.id, borrowType: "DIGITAL" });
        fetchBooks();
      },
    });
  };

  const runConfirm = async () => {
    if (!confirm?.onConfirm) return;
    try {
      setConfirmLoading(true);
      await confirm.onConfirm();
      setConfirm(null);
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || "خطا در انجام عملیات");
      setConfirm(null);
    } finally {
      setConfirmLoading(false);
    }
  };

  // Open format picker for download
  const openDownloadPicker = async (book) => {
    setDownloadBook(book);
    setDownloadFormats([]);
    setDownloadFormatsLoading(true);
    try {
      const res = await bookAPI.listDigitalBooks(libraryId, book.id);
      setDownloadFormats(res.data?.data || []);
    } catch {
      setError("خطا در دریافت فرمت‌های موجود");
      setDownloadBook(null);
    } finally {
      setDownloadFormatsLoading(false);
    }
  };

  const handleDownload = async (book, digitalBook) => {
    try {
      setDownloadingId(digitalBook.id);
      const res = await bookAPI.downloadDigitalBook(libraryId, book.id, digitalBook.id);
      const url = URL.createObjectURL(res.data);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${book.title}.${digitalBook.fileFormat.toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
      setDownloadBook(null);
    } catch {
      setError("خطا در دانلود. لطفاً دوباره تلاش کنید.");
    } finally {
      setDownloadingId(null);
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری کتاب‌ها...</div>;

  const visibleBooks = books.filter((b) =>
    availFilter === "physical" ? (b.totalCopiesCount || 0) > 0
      : availFilter === "digital" ? !!b.hasDigitalVersions
      : true);

  return (
    <div className="books-page">
      <div className="books-header">
        <div className="books-header-inner">
          <div>
            <h1 className="books-main-title">کتاب‌ها</h1>
            {libraryName && <p className="books-library-name">🏛️ {libraryName}</p>}
          </div>
          <div className="books-count-badge">{toPersianNum(books.length)} عنوان کتاب</div>
        </div>
      </div>

      <div className="books-body">
        {/* Search & Filters — single inline bar */}
        <div className="search-filter-bar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="جستجو بر اساس عنوان، نویسنده یا ناشر..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            {searching && <span className="search-spinner" />}
            {searchTerm && (
              <button className="search-clear" onClick={() => setSearchTerm("")}>✕</button>
            )}
          </div>

          {availableSubjects.length > 0 && (
            <select
              className="filter-select"
              value={filterSubjectId}
              onChange={(e) => setFilterSubjectId(e.target.value)}
            >
              <option value="">همه موضوعات</option>
              {availableSubjects.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          )}

          <input
            type="number"
            className="filter-input filter-input-year"
            placeholder="از سال"
            value={filterYearFrom}
            onChange={(e) => setFilterYearFrom(e.target.value)}
            min="1000" max="2100"
          />
          <input
            type="number"
            className="filter-input filter-input-year"
            placeholder="تا سال"
            value={filterYearTo}
            onChange={(e) => setFilterYearTo(e.target.value)}
            min="1000" max="2100"
          />

          {hasActiveFilters && (
            <button className="btn btn-ghost btn-sm sfb-clear-btn" onClick={clearFilters}>پاک</button>
          )}
        </div>

        <div className="books-avail-filters">
          {[
            { k: "all", label: "همه" },
            { k: "physical", label: "📦 نسخه چاپی" },
            { k: "digital", label: "💻 نسخه دیجیتال" },
          ].map((f) => (
            <button
              key={f.k}
              className={`books-avail-chip ${availFilter === f.k ? "books-avail-chip--active" : ""}`}
              onClick={() => setAvailFilter(f.k)}
            >
              {f.label}
            </button>
          ))}
        </div>

        {error && <div className="error-message">{error}</div>}

        {searching ? (
          <div className="books-searching">
            <div className="books-searching-spinner" />
            <span>در حال جستجو...</span>
          </div>
        ) : visibleBooks.length === 0 && !error ? (
          <div className="empty-state">
            <span className="empty-icon">📚</span>
            <p>کتابی یافت نشد</p>
          </div>
        ) : (
          <div className="books-grid">
            {visibleBooks.map((book) => {
              const borrow = borrowMap[book.id] || {};
              return (
                <div
                  key={book.id}
                  className="book-card book-card--clickable"
                  onClick={() => navigate(`/libraries/${libraryId}/books/${book.id}`)}
                  title="مشاهده پروفایل کتاب"
                >
                  <button
                    className={`book-fav-btn ${favoriteIds.has(book.id) ? "book-fav-btn--on" : ""}`}
                    onClick={(e) => toggleFavorite(e, book.id)}
                    title={favoriteIds.has(book.id) ? "حذف از نشان‌شده‌ها" : "نشان‌کردن"}
                  >
                    {favoriteIds.has(book.id) ? "★" : "☆"}
                  </button>
                  <div className="book-card-cover">
                    {book.coverImageUrl
                      ? <img src={book.coverImageUrl} alt="جلد" className="book-cover-img" />
                      : <div className="book-cover-placeholder">
                          <span className="book-cover-placeholder-title">{book.title}</span>
                          <span className="book-cover-placeholder-author">{book.author}</span>
                        </div>
                    }
                  </div>
                  <div className="book-card-body">
                    <div className="book-card-top">
                      <h3
                        className="book-title book-title--link"
                        onClick={() => navigate(`/libraries/${libraryId}/books/${book.id}`)}
                        title="مشاهده پروفایل کتاب"
                      >
                        {book.title}
                      </h3>
                      <p className="book-author">✍️ {book.author}</p>
                      {book.publisher && <p className="book-publisher">🏢 {book.publisher}</p>}
                      {book.subjectNames?.length > 0 && (
                        <div className="book-subjects">
                          {book.subjectNames.map((name, i) => (
                            <span key={i} className="book-subject-tag">🏷️ {name}</span>
                          ))}
                        </div>
                      )}
                      {book.publicationYear && <p className="book-year">📅 {toPersian(book.publicationYear)}</p>}
                      <div className="book-version-badges">
                        <span className={`ver-badge ${book.totalCopiesCount > 0 ? "ver-badge--phys" : "ver-badge--off"}`}>
                          {book.totalCopiesCount > 0 ? "✓" : "✗"} نسخه چاپی
                        </span>
                        <span className={`ver-badge ${book.hasDigitalVersions ? "ver-badge--digi" : "ver-badge--off"}`}>
                          {book.hasDigitalVersions ? "✓" : "✗"} نسخه دیجیتال
                        </span>
                      </div>
                    </div>

                    <div className="book-card-footer">
                      <div className="book-availability">
                        <span className={`avail-dot ${book.availableCopiesCount > 0 ? "avail-dot-green" : "avail-dot-red"}`} />
                        <span className="avail-text">
                          {toPersian(book.availableCopiesCount)} از {toPersian(book.totalCopiesCount)} موجود
                        </span>
                        {book.hasDigitalVersions && (
                          <span className="digital-badge">دیجیتال</span>
                        )}
                      </div>

                      <div className="book-card-actions" onClick={(e) => e.stopPropagation()}>
                        {book.totalCopiesCount > 0 && (
                          borrow.pendingPhysical ? (
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => { setEditBorrow(borrow.pendingPhysical); setSelectedBook(book); setIsModalOpen(true); }}
                              title="ویرایش درخواست تأییدنشده"
                            >
                              ✏️ ویرایش درخواست
                            </button>
                          ) : borrow.hasActivePhysical ? (
                            <button className="btn btn-ghost btn-sm" disabled>✓ امانت</button>
                          ) : book.availableCopiesCount > 0 ? (
                            <button className="btn btn-success btn-sm" onClick={() => { setEditBorrow(null); setSelectedBook(book); setIsModalOpen(true); }}>
                              📚 امانت
                            </button>
                          ) : (
                            <button className="btn btn-outline btn-sm" onClick={() => handleReserve(book)}
                              title="کتاب در حال حاضر موجود نیست. با کلیک رزرو می‌کنید.">
                              🔖 رزرو
                            </button>
                          )
                        )}

                        {book.hasDigitalVersions && (
                          borrow.hasApprovedDigital ? (
                            <button className="btn btn-info btn-sm" onClick={() => openDownloadPicker(book)}>
                              ⬇ دانلود
                            </button>
                          ) : borrow.hasActiveDigital ? (
                            <button className="btn btn-ghost btn-sm" disabled>✓ در انتظار تایید دیجیتال</button>
                          ) : (
                            <button className="btn btn-outline btn-sm" onClick={() => handleRequestDigital(book)}>
                              💾 دانلود
                            </button>
                          )
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <BorrowModal
        isOpen={isModalOpen}
        book={selectedBook}
        editBorrow={editBorrow}
        borrowDuration={borrowDuration}
        onClose={() => { setIsModalOpen(false); setSelectedBook(null); setEditBorrow(null); }}
        onSubmit={handlePhysicalSubmit}
      />

      <ConfirmDialog
        open={!!confirm}
        title={confirm?.title}
        message={confirm?.message}
        confirmLabel={confirm?.confirmLabel}
        variant={confirm?.variant}
        loading={confirmLoading}
        onConfirm={runConfirm}
        onCancel={() => setConfirm(null)}
      />

      {downloadBook && (
        <div className="modal-overlay" onClick={() => setDownloadBook(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>انتخاب فرمت دانلود</h2>
              <button className="modal-close-btn" onClick={() => setDownloadBook(null)}>✕</button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: "0.88rem", color: "var(--color-text-secondary)", marginBottom: "1rem" }}>
                {downloadBook.title}
              </p>
              {downloadFormatsLoading ? (
                <div className="loading" style={{ fontSize: "0.9rem" }}>در حال بارگذاری...</div>
              ) : downloadFormats.length === 0 ? (
                <p style={{ color: "var(--color-text-muted)" }}>فرمتی موجود نیست.</p>
              ) : (
                <div className="digital-format-list">
                  {downloadFormats.map((df) => (
                    <button
                      key={df.id}
                      className="digital-format-option"
                      onClick={() => handleDownload(downloadBook, df)}
                      disabled={downloadingId === df.id}
                      style={{ width: "100%", textAlign: "right", cursor: "pointer", border: "none", background: "transparent" }}
                    >
                      <span className="digital-format-badge-lg">{df.fileFormat}</span>
                      <span className="digital-format-filename">{df.originalFilename}</span>
                      {downloadingId === df.id && <span style={{ color: "var(--color-text-muted)", fontSize: "0.8rem" }}>در حال دانلود...</span>}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setDownloadBook(null)}>بستن</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BooksPage;
