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
  const [libraryName] = useState(
    localStorage.getItem("activeLibraryName") || "",
  );
  const [selectedBook, setSelectedBook] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [borrowingError, setBorrowingError] = useState("");

  const fetchBooks = useCallback(async () => {
    try {
      setLoading(true);
      if (!libraryId) {
        setBooks([]);
        setError("Select a library first to view books.");
        return;
      }
      const response = await bookAPI.getBooks(libraryId, {
        search: searchTerm,
      });
      const payload = response.data.data || response.data;
      const list = Array.isArray(payload) ? payload : payload?.content || [];
      setBooks(list);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load books");
    } finally {
      setLoading(false);
    }
  }, [searchTerm, libraryId]);

  useEffect(() => {
    fetchBooks();
  }, [fetchBooks]);

  const handleBorrowClick = (book) => {
    setSelectedBook(book);
    setIsModalOpen(true);
    setBorrowingError("");
  };

  const handleBorrow = async (bookId) => {
    try {
      if (!libraryId) {
        setBorrowingError("Select a library first.");
        return;
      }
      await borrowAPI.createBorrow(libraryId, {
        bookId,
        borrowType: "PHYSICAL",
      });
      setIsModalOpen(false);
      setSelectedBook(null);
      fetchBooks();
    } catch (err) {
      setBorrowingError(
        err.response?.data?.message ||
          "Failed to borrow book. Please try again.",
      );
      throw new Error(borrowingError);
    }
  };

  if (loading) return <div className="loading">Loading books...</div>;

  return (
    <div className="books-container">
      <h2>Available Books</h2>
      {libraryName && (
        <div className="active-library">Active Library: {libraryName}</div>
      )}
      {!libraryId && (
        <div className="error-message">
          Please select a library from the Libraries page.
        </div>
      )}
      <div className="search-box">
        <input
          type="text"
          placeholder="Search books by title or author..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="search-input"
        />
      </div>

      {error && <div className="error-message">{error}</div>}

      {books.length === 0 ? (
        <p className="no-books">No books found</p>
      ) : (
        <div className="books-grid">
          {books.map((book) => (
            <div key={book.id} className="book-card">
              <h3>{book.title}</h3>
              <p className="author">{book.author}</p>
              {book.publisher && (
                <p className="isbn">Publisher: {book.publisher}</p>
              )}
              <p className="availability">
                Available: <strong>{book.availableCopiesCount}</strong> /{" "}
                {book.totalCopiesCount}
              </p>
              {book.availableCopiesCount > 0 ? (
                <button
                  className="borrow-btn"
                  onClick={() => handleBorrowClick(book)}
                >
                  Borrow
                </button>
              ) : (
                <button className="borrow-btn" disabled>
                  Not Available
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      <BorrowModal
        isOpen={isModalOpen}
        book={selectedBook}
        onClose={() => {
          setIsModalOpen(false);
          setSelectedBook(null);
          setBorrowingError("");
        }}
        onBorrow={handleBorrow}
      />
    </div>
  );
};

export default BooksPage;
