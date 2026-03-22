import React, { useState, useEffect, useCallback } from "react";
import { bookAPI, borrowAPI } from "../services/api";
import BorrowModal from "../components/BorrowModal";
import "./BooksPage.css";

const BooksPage = () => {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [libraryId] = useState(localStorage.getItem("activeLibraryId") || "");
  const [libraryName] = useState(localStorage.getItem("activeLibraryName") || "");
  const [selectedBook, setSelectedBook] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [borrowingError, setBorrowingError] = useState("");

  const fetchBooks = useCallback(async () => {
    try {
      setLoading(true);
      if (!libraryId) {
        setBooks([]);
        setError("ابتدا یک کتابخانه را از صفحه کتابخانه‌ها انتخاب کنید.");
        return;
      }
      const response = await bookAPI.getBooks(libraryId, { search: searchTerm });
      const payload = response.data.data || response.data;
      const list = Array.isArray(payload) ? payload : payload?.content || [];
      setBooks(list);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری کتاب‌ها");
    } finally {
      setLoading(false);
    }
  }, [searchTerm, libraryId]);

  useEffect(() => { fetchBooks(); }, [fetchBooks]);

  const handleBorrowClick = (book) => {
    setSelectedBook(book);
    setIsModalOpen(true);
    setBorrowingError("");
  };

  const handleBorrow = async (bookId) => {
    try {
      if (!libraryId) { setBorrowingError("ابتدا کتابخانه را انتخاب کنید."); return; }
      await borrowAPI.createBorrow(libraryId, { bookId, borrowType: "PHYSICAL" });
      setIsModalOpen(false);
      setSelectedBook(null);
      fetchBooks();
    } catch (err) {
      setBorrowingError(err.response?.data?.message || "خطا در ثبت امانت. لطفاً دوباره تلاش کنید.");
      throw new Error(borrowingError);
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری کتاب‌ها...</div>;

  return (
    <div className="books-page">
      {/* Page Header */}
      <div className="books-header">
        <div className="books-header-inner">
          <div>
            <h1 className="books-main-title">کتاب‌ها</h1>
            {libraryName && (
              <p className="books-library-name">🏛️ {libraryName}</p>
            )}
          </div>
          <div className="books-count-badge">
            {books.length} عنوان کتاب
          </div>
        </div>
      </div>

      <div className="books-body">
        {!libraryId && (
          <div className="info-banner">
            📍 لطفاً ابتدا از صفحه کتابخانه‌ها یک کتابخانه فعال کنید.
          </div>
        )}

        {/* Search */}
        {libraryId && (
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
        )}

        {error && <div className="error-message">{error}</div>}

        {/* Books Grid */}
        {books.length === 0 && !error ? (
          <div className="empty-state">
            <span className="empty-icon">📚</span>
            <p>کتابی یافت نشد</p>
          </div>
        ) : (
          <div className="books-grid">
            {books.map((book) => (
              <div key={book.id} className="book-card">
                <div className="book-card-spine" />
                <div className="book-card-body">
                  <div className="book-card-top">
                    <h3 className="book-title">{book.title}</h3>
                    <p className="book-author">✍️ {book.author}</p>
                    {book.publisher && (
                      <p className="book-publisher">🏢 {book.publisher}</p>
                    )}
                  </div>
                  <div className="book-card-footer">
                    <div className="book-availability">
                      <span className={`avail-dot ${book.availableCopiesCount > 0 ? "avail-dot-green" : "avail-dot-red"}`} />
                      <span className="avail-text">
                        {book.availableCopiesCount} از {book.totalCopiesCount} موجود
                      </span>
                    </div>
                    {book.availableCopiesCount > 0 ? (
                      <button className="btn btn-success btn-sm" onClick={() => handleBorrowClick(book)}>
                        امانت گرفتن
                      </button>
                    ) : (
                      <button className="btn btn-ghost btn-sm" disabled>
                        ناموجود
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <BorrowModal
        isOpen={isModalOpen}
        book={selectedBook}
        onClose={() => { setIsModalOpen(false); setSelectedBook(null); setBorrowingError(""); }}
        onBorrow={handleBorrow}
      />
    </div>
  );
};

export default BooksPage;
