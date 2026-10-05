import { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useSearchParams, useNavigate } from "react-router-dom";
import { bookAPI, borrowAPI, subjectAPI, meAPI } from "../services/api";
import { useLibrary } from "../context/LibraryContext";
import { useDebounce } from "../hooks/useDebounce";
import BorrowModal from "../components/BorrowModal";
import ConfirmDialog from "../components/ConfirmDialog";
import ClassificationBadge from "../components/ClassificationBadge";
import { toPersian, toPersianNum } from "../utils/persian";
import "./BooksPage.css";

const PAGE_SIZE = 24;

const BooksPage = () => {
  const { libraryId } = useParams();
  const { libraryName, borrowDuration } = useLibrary() || {};

  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState(searchParams.get("q") || "");
  const [searching, setSearching] = useState(false);
  const isFirstLoad = useRef(true);
  const sentinelRef = useRef(null);

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
  const [downloadingId, setDownloadingId] = useState(null);

  // Load available subjects on mount
  useEffect(() => {
    subjectAPI.getSubjects(libraryId)
      .then((res) => setAvailableSubjects(res.data?.data || []))
      .catch(() => {});
  }, [libraryId]);

  const hasActiveFilters = searchTerm || filterSubjectId || filterYearFrom || filterYearTo;

  const fetchBorrows = useCallback(async () => {
    try {
      const res = await borrowAPI.getBorrows(libraryId);
      const myBorrows = res.data?.data || res.data || [];
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
    } catch {
      setBorrowMap({});
    }
  }, [libraryId]);

  // Fetches one page of results. `pageNum === 0` replaces the list (new search/filter/first
  // load); any later page appends, powering infinite scroll.
  const fetchBooksPage = useCallback(async (pageNum) => {
    try {
      if (pageNum === 0) {
        if (isFirstLoad.current) setLoading(true);
        else setSearching(true);
      } else {
        setLoadingMore(true);
      }

      const params = { page: pageNum, size: PAGE_SIZE };
      if (debouncedSearchTerm.trim()) params.query = debouncedSearchTerm.trim();
      if (filterSubjectId) params.subjectId = filterSubjectId;
      if (debouncedYearFrom) params.yearFrom = parseInt(debouncedYearFrom);
      if (debouncedYearTo) params.yearTo = parseInt(debouncedYearTo);

      const booksRes = await bookAPI.searchBooks(libraryId, params);
      const payload = booksRes.data.data || booksRes.data;
      const list = Array.isArray(payload) ? payload : payload?.content || [];
      const totalElements = Array.isArray(payload) ? list.length : (payload?.totalElements ?? list.length);
      const last = Array.isArray(payload) ? true : (payload?.last ?? true);

      setBooks((prev) => (pageNum === 0 ? list : [...prev, ...list]));
      setTotalCount(totalElements);
      setHasMore(!last);
      setPage(pageNum);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری کتاب‌ها");
    } finally {
      setLoading(false);
      setSearching(false);
      setLoadingMore(false);
      isFirstLoad.current = false;
    }
  }, [debouncedSearchTerm, filterSubjectId, debouncedYearFrom, debouncedYearTo, libraryId]);

  // Any filter change restarts pagination from page 0
  useEffect(() => { fetchBooksPage(0); }, [fetchBooksPage]);
  useEffect(() => { fetchBorrows(); }, [fetchBorrows]);

  // Infinite scroll: load the next page when the sentinel at the bottom of the grid
  // becomes visible, instead of fetching all 100+ books (with cover images) up front.
  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return undefined;
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && hasMore && !loadingMore && !loading && !searching) {
        fetchBooksPage(page + 1);
      }
    }, { rootMargin: "400px" });
    observer.observe(el);
    return () => observer.disconnect();
  }, [hasMore, loadingMore, loading, searching, page, fetchBooksPage]);

  // After a mutation (borrow/return/reserve), re-sync from the first page rather than
  // trying to patch individual book entries across however many pages are loaded.
  const refresh = useCallback(() => {
    fetchBooksPage(0);
    fetchBorrows();
  }, [fetchBooksPage, fetchBorrows]);

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
      refresh();
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
        refresh();
      },
    });
  };

  // Digital access is granted instantly on request, so one confirmation click
  // both requests AND downloads the file — no second "now click download" step.
  const handleRequestDigital = (book) => {
    setConfirm({
      title: "درخواست دانلود",
      message: `آیا درخواست دانلود کتاب «${book.title}» را ثبت می‌کنید؟`,
      confirmLabel: "بله، ثبت کن",
      variant: "primary",
      onConfirm: async () => {
        await borrowAPI.createBorrow(libraryId, { bookId: book.id, borrowType: "DIGITAL" });
        await handleDownload(book);
        refresh();
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

  // One click: fetch the book's digital version and download it directly — no
  // intermediate picker, since a book only ever has one digital (PDF) version.
  const handleDownload = async (book) => {
    try {
      setDownloadingId(book.id);
      const listRes = await bookAPI.listDigitalBooks(libraryId, book.id);
      const versions = listRes.data?.data || [];
      if (versions.length === 0) {
        setError("نسخه‌ی دیجیتالی موجود نیست");
        return;
      }
      const digitalBook = versions[0];
      const res = await bookAPI.downloadDigitalBook(libraryId, book.id, digitalBook.id);
      const url = URL.createObjectURL(res.data);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${book.title}.${digitalBook.fileFormat.toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
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
            {libraryName && <span className="books-lib-eyebrow">🏛️ {libraryName}</span>}
            <h1 className="books-main-title">کتاب‌ها</h1>
            <span className="books-title-rule" aria-hidden="true" />
          </div>
          <div className="books-count-badge">
            <strong>{toPersianNum(totalCount)}</strong>
            <span>عنوان کتاب</span>
          </div>
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
                      <ClassificationBadge level={book.classification} />
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
                            <button className="btn btn-info btn-sm" onClick={() => handleDownload(book)} disabled={downloadingId === book.id}>
                              {downloadingId === book.id ? "در حال دانلود..." : "⬇ دانلود"}
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

        {!searching && hasMore && (
          <div ref={sentinelRef} className="books-load-sentinel">
            {loadingMore && <span className="books-searching-spinner" />}
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
    </div>
  );
};

export default BooksPage;
