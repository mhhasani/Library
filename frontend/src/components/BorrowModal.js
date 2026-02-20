import React, { useState } from "react";
import "./BorrowModal.css";

const BorrowModal = ({ isOpen, book, onClose, onBorrow }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  if (!isOpen) return null;

  const handleBorrow = async () => {
    try {
      setLoading(true);
      setError("");
      await onBorrow(book.id);
      onClose();
    } catch (err) {
      setError(err.message || "Failed to borrow book");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Borrow Book</h2>
          <button className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="modal-body">
          <div className="book-details">
            <p>
              <strong>Title:</strong> {book?.title}
            </p>
            <p>
              <strong>Author:</strong> {book?.author}
            </p>
            {book?.publisher && (
              <p>
                <strong>Publisher:</strong> {book?.publisher}
              </p>
            )}
            <p>
              <strong>Available Copies:</strong> {book?.availableCopiesCount}
            </p>
          </div>

          {error && <div className="error-message">{error}</div>}

          <div className="borrow-info">
            <p>Are you sure you want to borrow this book?</p>
            <p className="terms">
              You will have <strong>14 days</strong> to return the book.
            </p>
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn-cancel" onClick={onClose}>
            Cancel
          </button>
          <button
            className="btn-borrow"
            onClick={handleBorrow}
            disabled={loading}
          >
            {loading ? "Borrowing..." : "Confirm Borrow"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default BorrowModal;
