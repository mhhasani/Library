import React, { useState, useEffect, useCallback } from "react";
import { useParams } from "react-router-dom";
import { bookAPI, borrowAPI } from "../services/api";
import { useLibrary } from "../context/LibraryContext";
import BorrowModal from "../components/BorrowModal";
import "./BooksPage.css";

const BooksPage = () => {
  const { libraryId } = useParams();
  const { libraryName, borrowDuration } = useLibrary() || {};

  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");

  // Physical borrow modal
  const [selectedBook, setSelectedBook] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // borrowMap[bookId] = { hasActivePhysical, hasActiveDigital, hasApprovedDigital }
  const [borrowMap, setBorrowMap] = useState({});

  // Download format picker
  const [downloadBook, setDownloadBook] = useState(null);
  const [downloadFormats, setDownloadFormats] = useState([]);
  const [downloadFormatsLoading, setDownloadFormatsLoading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  const fetchBooks = useCallback(async () => {
    try {
      setLoading(true);
      const [booksRes, borrowsRes] = await Promise.all([
        bookAPI.getBooks(libraryId, { search: searchTerm }),
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
        const isActive = (b.status === "REQUESTED" || b.status === "APPROVED") && !b.returnDate;
        if (b.borrowType === "PHYSICAL" && isActive) map[bid].hasActivePhysical = true;
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
    }
  }, [searchTerm, libraryId]);

  useEffect(() => { fetchBooks(); }, [fetchBooks]);

  // Physical borrow
  const handleBorrow = async (bookId) => {
    await borrowAPI.createBorrow(libraryId, { bookId, borrowType: "PHYSICAL" });
    setIsModalOpen(false);
    setSelectedBook(null);
    fetchBooks();
  };

  // Digital borrow request — یه کلیک، بدون انتخاب فرمت
  const handleRequestDigital = async (book) => {
    try {
      await borrowAPI.createBorrow(libraryId, { bookId: book.id, borrowType: "DIGITAL" });
      fetchBooks();
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || "خطا در ثبت درخواست دیجیتال");
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

  return (
    <div className="books-page">
      <div className="books-header">
        <div className="books-header-inner">
          <div>
            <h1 className="books-main-title">کتاب‌ها</h1>
            {libraryName && <p className="books-library-name">🏛️ {libraryName}</p>}
          </div>
          <div className="books-count-badge">{books.length} عنوان کتاب</div>
        </div>
      </div>

      <div className="books-body">
        <div className="search-container">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="جستجو بر اساس عنوان یا نام نویسنده..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            {searchTerm && (
              <button className="search-clear" onClick={() => setSearchTerm("")}>✕</button>
            )}
          </div>
        </div>

        {error && <div className="error-message">{error}</div>}

        {books.length === 0 && !error ? (
          <div className="empty-state">
            <span className="empty-icon">📚</span>
            <p>کتابی یافت نشد</p>
          </div>
        ) : (
          <div className="books-grid">
            {books.map((book) => {
              const borrow = borrowMap[book.id] || {};
              return (
                <div key={book.id} className="book-card">
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
                      <h3 className="book-title">{book.title}</h3>
                      <p className="book-author">✍️ {book.author}</p>
                      {book.publisher && <p className="book-publisher">🏢 {book.publisher}</p>}
                    </div>

                    <div className="book-card-footer">
                      <div className="book-availability">
                        <span className={`avail-dot ${book.availableCopiesCount > 0 ? "avail-dot-green" : "avail-dot-red"}`} />
                        <span className="avail-text">
                          {book.availableCopiesCount} از {book.totalCopiesCount} موجود
                        </span>
                        {book.hasDigitalVersions && (
                          <span className="digital-badge">دیجیتال</span>
                        )}
                      </div>

                      <div className="book-card-actions">
                        {/* Physical borrow — only shown when book has physical copies */}
                        {book.totalCopiesCount > 0 && (
                          borrow.hasActivePhysical ? (
                            <button className="btn btn-ghost btn-sm" disabled>✓ امانت فیزیکی</button>
                          ) : book.availableCopiesCount > 0 ? (
                            <button className="btn btn-success btn-sm" onClick={() => { setSelectedBook(book); setIsModalOpen(true); }}>
                              📚 امانت فیزیکی
                            </button>
                          ) : (
                            <button className="btn btn-ghost btn-sm" disabled>ناموجود</button>
                          )
                        )}

                        {/* Digital borrow / download */}
                        {book.hasDigitalVersions && (
                          borrow.hasApprovedDigital ? (
                            <button className="btn btn-info btn-sm" onClick={() => openDownloadPicker(book)}>
                              ⬇ دانلود
                            </button>
                          ) : borrow.hasActiveDigital ? (
                            <button className="btn btn-ghost btn-sm" disabled>✓ در انتظار تایید دیجیتال</button>
                          ) : (
                            <button className="btn btn-outline btn-sm" onClick={() => handleRequestDigital(book)}>
                              💾 امانت دیجیتال
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

      {/* Physical Borrow Modal */}
      <BorrowModal
        isOpen={isModalOpen}
        book={selectedBook}
        borrowDuration={borrowDuration}
        onClose={() => { setIsModalOpen(false); setSelectedBook(null); }}
        onBorrow={handleBorrow}
      />

      {/* Download Format Picker */}
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
