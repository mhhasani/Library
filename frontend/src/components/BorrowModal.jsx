import React, { useState, useEffect } from "react";
import { userAPI } from "../services/api";
import "./BorrowModal.css";

/**
 * Physical borrow request form. Works in two modes:
 *  - create: prefilled from the user's profile, calls onSubmit(details)
 *  - edit:   pass `editBorrow` (an existing REQUESTED borrow) to prefill from it
 */
const BorrowModal = ({ isOpen, book, editBorrow, onClose, onSubmit, borrowDuration = 14 }) => {
  const isEdit = !!editBorrow;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [deliveryExtension, setDeliveryExtension] = useState("");
  const [requestedDurationDays, setRequestedDurationDays] = useState(borrowDuration);
  const [saveToProfile, setSaveToProfile] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    setError("");
    if (isEdit) {
      setDeliveryAddress(editBorrow.deliveryAddress || "");
      setDeliveryExtension(editBorrow.deliveryExtension || "");
      setRequestedDurationDays(editBorrow.requestedDurationDays || borrowDuration);
      setSaveToProfile(false);
    } else {
      setRequestedDurationDays(borrowDuration);
      userAPI
        .getProfile()
        .then((res) => {
          const p = res.data?.data || res.data;
          setDeliveryAddress(p?.deliveryAddress || "");
          setDeliveryExtension(p?.internalExtension || "");
        })
        .catch(() => {});
    }
  }, [isOpen, isEdit, editBorrow, borrowDuration]);

  if (!isOpen) return null;

  const handleSubmit = async () => {
    if (!deliveryAddress.trim()) {
      setError("آدرس تحویل الزامی است");
      return;
    }
    if (deliveryExtension && !/^\d{8}$/.test(deliveryExtension)) {
      setError("شماره تلفن داخلی باید ۸ رقم باشد");
      return;
    }
    if (!requestedDurationDays || requestedDurationDays < 1) {
      setError("تعداد روز درخواستی نامعتبر است");
      return;
    }
    try {
      setLoading(true);
      setError("");
      await onSubmit({
        deliveryAddress: deliveryAddress.trim(),
        deliveryExtension: deliveryExtension.trim(),
        requestedDurationDays: Number(requestedDurationDays),
        saveToProfile,
      });
      onClose();
    } catch (err) {
      setError(err.message || "خطا در ثبت درخواست");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{isEdit ? "ویرایش درخواست امانت" : "درخواست امانت"}</h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="بستن">✕</button>
        </div>

        <div className="modal-body">
          <div className="modal-book-card">
            <div className="modal-book-spine" />
            <div className="modal-book-info">
              <h3 className="modal-book-title">{book?.title}</h3>
              {book?.author && <p className="modal-book-author">✍️ {book.author}</p>}
              {book?.publisher && <p className="modal-book-publisher">🏢 {book.publisher}</p>}
              {book?.availableCopiesCount != null && (
                <div className="modal-copies-badge">
                  <span>{book.availableCopiesCount} نسخه موجود</span>
                </div>
              )}
            </div>
          </div>

          {error && <div className="error-message">{error}</div>}

          <div className="bm-form">
            <div className="bm-field">
              <label>آدرس محل تحویل <span className="bm-req">*</span></label>
              <textarea
                rows={2}
                value={deliveryAddress}
                onChange={(e) => setDeliveryAddress(e.target.value)}
                placeholder="مثال: ساختمان فناوری اطلاعات، طبقه دوم، اتاق ۱۱۲"
              />
            </div>

            <div className="bm-row">
              <div className="bm-field">
                <label>شماره تلفن داخلی</label>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={8}
                  value={deliveryExtension}
                  onChange={(e) =>
                    setDeliveryExtension(e.target.value.replace(/\D/g, "").slice(0, 8))
                  }
                  placeholder="۸ رقم"
                />
              </div>
              <div className="bm-field">
                <label>مدت امانت (روز)</label>
                <input
                  type="number"
                  min={1}
                  value={requestedDurationDays}
                  onChange={(e) => setRequestedDurationDays(e.target.value)}
                />
              </div>
            </div>

            <label className="bm-check">
              <input
                type="checkbox"
                checked={saveToProfile}
                onChange={(e) => setSaveToProfile(e.target.checked)}
              />
              <span>ذخیره آدرس و داخلی به‌عنوان پیش‌فرض در پروفایل من</span>
            </label>

            <p className="bm-hint">
              {isEdit
                ? "تا وقتی کتابدار درخواست را تأیید نکرده، می‌توانید اطلاعات را ویرایش کنید."
                : "پس از ثبت، درخواست شما به کارتابل مسئول کتابخانه ارسال می‌شود. مدت نهایی امانت را کتابدار تعیین می‌کند."}
            </p>
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-ghost" onClick={onClose}>انصراف</button>
          <button className="btn btn-success" onClick={handleSubmit} disabled={loading}>
            {loading ? "در حال ثبت..." : isEdit ? "✓ ذخیره تغییرات" : "✓ ثبت درخواست"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default BorrowModal;
