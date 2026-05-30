import React, { useState } from "react";
import "./BorrowModal.css";

const BorrowModal = ({ isOpen, book, onClose, onBorrow, borrowDuration = 14 }) => {
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
      setError(err.message || "خطا در ثبت امانت");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>امانت گرفتن کتاب</h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="بستن">
            ✕
          </button>
        </div>

        <div className="modal-body">
          <div className="modal-book-card">
            <div className="modal-book-spine" />
            <div className="modal-book-info">
              <h3 className="modal-book-title">{book?.title}</h3>
              <p className="modal-book-author">✍️ {book?.author}</p>
              {book?.publisher && (
                <p className="modal-book-publisher">🏢 {book?.publisher}</p>
              )}
              <div className="modal-copies-badge">
                <span>{book?.availableCopiesCount} نسخه موجود</span>
              </div>
            </div>
          </div>

          {error && <div className="error-message">{error}</div>}

          <div className="modal-borrow-notice">
            <div className="notice-icon">📋</div>
            <div>
              <p className="notice-title">آیا می‌خواهید این کتاب را امانت بگیرید؟</p>
              <p className="notice-detail">
                مدت امانت <strong>{borrowDuration} روز</strong> می‌باشد.
              </p>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-ghost" onClick={onClose}>
            انصراف
          </button>
          <button
            className="btn btn-success"
            onClick={handleBorrow}
            disabled={loading}
          >
            {loading ? "در حال ثبت..." : "✓ تأیید امانت"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default BorrowModal;
