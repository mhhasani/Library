import React, { useState } from "react";
import { borrowAPI } from "../services/api";
import { toPersianNum } from "../utils/persian";
import BorrowModal from "./BorrowModal";
import ConfirmDialog from "./ConfirmDialog";
import PersianDateTimePicker from "./PersianDateTimePicker";
import "../pages/BorrowsPage.css";
import "../pages/PhysicalBorrowsPage.css";

const STATUS_LABELS = {
  REQUESTED: "در انتظار تأیید کتابدار",
  APPROVED: "تأیید شد — در انتظار دریافت",
  RECEIVED: "در دست شما",
  RETURNED: "برگردانده شد",
  REJECTED: "رد شده",
  CANCELLED: "لغو شد",
  EXPIRED: "منقضی",
};
const STATUS_CLASS = {
  REQUESTED: "badge-warning",
  APPROVED: "badge-info",
  RECEIVED: "badge-success",
  RETURNED: "badge-muted",
  REJECTED: "badge-danger",
  CANCELLED: "badge-muted",
  EXPIRED: "badge-muted",
};
const ACTIVE = ["REQUESTED", "APPROVED", "RECEIVED"];

const fmtDate = (d) => (d ? new Date(d).toLocaleDateString("fa-IR") : "—");
const isOverdue = (b) => b.status === "RECEIVED" && b.dueDate && new Date(b.dueDate) < new Date();

// The raw status doesn't capture the return sub-flow, so RECEIVED must not always read
// "در دست شما" — it would contradict the card body once the user hands the book to the courier.
const statusBadge = (b) => {
  if (b.status === "RECEIVED") {
    if (b.handedOverByUserAt) return { label: "تحویل به پیک شد — در انتظار تأیید", cls: "badge-info" };
    if (b.returnRequestedAt) return { label: "در حال برگرداندن", cls: "badge-info" };
    if (isOverdue(b)) return { label: "در دست شما — سررسید گذشته", cls: "badge-danger" };
  }
  return { label: STATUS_LABELS[b.status] || b.status, cls: STATUS_CLASS[b.status] || "badge-muted" };
};

/**
 * Self-contained card for a single physical borrow: timeline, details, and ALL recipient
 * actions (edit, cancel, confirm receipt, request return, hand over to courier) with their
 * own modals/dialogs. Reused by the physical-borrows page and the book profile page.
 *
 *  borrow:    the BorrowDTO
 *  libraryId: library id
 *  onChanged: callback to refresh after an action
 *  onError:   optional (msg) => void for surfacing errors
 */
const PhysicalBorrowCard = ({ borrow: b, libraryId, onChanged, onError }) => {
  const [confirm, setConfirm] = useState(null);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [confirmError, setConfirmError] = useState(""); // shown inside the confirm dialog
  const [editing, setEditing] = useState(false);
  const [returnOpen, setReturnOpen] = useState(false);
  const [returnForm, setReturnForm] = useState({ returnAddress: "", returnExtension: "", preferredDate: "" });
  const [returnSaving, setReturnSaving] = useState(false);
  const [returnError, setReturnError] = useState(""); // shown inside the return modal, not on the page

  const reportError = (msg) => { if (onError) onError(msg); };

  const runConfirm = async () => {
    if (!confirm?.onConfirm) return;
    try {
      setConfirmError("");
      setConfirmLoading(true);
      await confirm.onConfirm();
      setConfirm(null);
      onChanged?.();
    } catch (err) {
      // keep the dialog open and show the error inside it
      setConfirmError(err.response?.data?.message || "خطا در انجام عملیات");
    } finally {
      setConfirmLoading(false);
    }
  };

  const askConfirmReceipt = () => setConfirm({
    title: "تأیید دریافت کتاب",
    message: `آیا کتاب «${b.bookTitle}» را دریافت کرده‌اید؟ با تأیید، مهلت امانت از همین لحظه آغاز می‌شود.`,
    confirmLabel: "بله، دریافت کردم", variant: "success",
    onConfirm: () => borrowAPI.confirmReceipt(libraryId, b.id),
  });

  const askCancel = () => setConfirm({
    title: "لغو درخواست امانت",
    message: `آیا از لغو امانت کتاب «${b.bookTitle}» مطمئن هستید؟`,
    confirmLabel: "بله، لغو کن", variant: "danger",
    onConfirm: () => borrowAPI.cancelByUser(libraryId, b.id),
  });

  const askHandover = () => setConfirm({
    title: "تحویل کتاب به پیک",
    message: `آیا کتاب «${b.bookTitle}» را به پیک تحویل داده‌اید؟ پس از تأیید کتابدار، امانت پایان می‌یابد.`,
    confirmLabel: "بله، تحویل دادم", variant: "primary",
    onConfirm: () => borrowAPI.confirmHandover(libraryId, b.id),
  });

  const openReturn = () => {
    // prefill from an existing return request when editing, else from delivery defaults
    setReturnForm({
      returnAddress: b.returnAddress || b.deliveryAddress || "",
      returnExtension: b.returnExtension || b.deliveryExtension || "",
      preferredDate: b.returnPreferredDate || "",
    });
    setReturnError("");
    setReturnOpen(true);
  };

  const askCancelReturn = () => setConfirm({
    title: "لغو درخواست برگرداندن",
    message: `آیا درخواست برگرداندن کتاب «${b.bookTitle}» لغو شود؟ امانت همچنان فعال می‌ماند.`,
    confirmLabel: "بله، لغو کن", variant: "danger",
    onConfirm: () => borrowAPI.cancelReturnRequest(libraryId, b.id),
  });

  const submitReturn = async () => {
    if (!returnForm.returnAddress.trim()) { setReturnError("آدرس تحویل کتاب الزامی است"); return; }
    if (returnForm.returnExtension && !/^\d{8}$/.test(returnForm.returnExtension)) {
      setReturnError("شماره تلفن داخلی باید ۸ رقم باشد"); return;
    }
    try {
      setReturnError("");
      setReturnSaving(true);
      await borrowAPI.requestReturn(libraryId, b.id, {
        returnAddress: returnForm.returnAddress.trim(),
        returnExtension: returnForm.returnExtension.trim(),
        preferredDate: returnForm.preferredDate || null,
      });
      setReturnOpen(false);
      onChanged?.();
    } catch (err) {
      setReturnError(err.response?.data?.message || "خطا در ثبت درخواست برگرداندن کتاب");
    } finally {
      setReturnSaving(false);
    }
  };

  const handleUpdateRequest = async (details) => {
    try {
      await borrowAPI.updateRequest(libraryId, b.id, details);
      setEditing(false);
      onChanged?.();
    } catch (err) {
      throw new Error(err.response?.data?.error || err.response?.data?.message || "خطا در ویرایش درخواست");
    }
  };

  const overdue = isOverdue(b);

  return (
    <div className={`pb-card ${overdue ? "pb-card--overdue" : ""}`}>
      <div className="pb-card-head">
        <div>
          <h3 className="pb-card-title">{b.bookTitle}</h3>
          {b.libraryName && <span className="pb-card-lib">🏛️ {b.libraryName}</span>}
          {b.trackingCode && <span className="pb-card-track">کد رهگیری: {b.trackingCode}</span>}
        </div>
        {(() => { const s = statusBadge(b); return <span className={`badge ${s.cls}`}>{s.label}</span>; })()}
      </div>

      <div className="pb-steps">
        <Step label="ثبت درخواست" done active />
        <Step label="تأیید کتابدار" done={["APPROVED", "RECEIVED", "RETURNED"].includes(b.status)} />
        <Step label="دریافت کتاب" done={["RECEIVED", "RETURNED"].includes(b.status)} />
        <Step label="برگرداندن" done={b.status === "RETURNED"} last />
      </div>

      <div className="pb-grid">
        {b.copyNumber != null && <Field label="نسخه" value={`#${toPersianNum(b.copyNumber)}`} />}
        {b.copyUniqueCode && <Field label="کد نسخه" value={b.copyUniqueCode} />}
        {b.deliveryAddress && <Field label="محل تحویل" value={b.deliveryAddress} wide />}
        {b.courierName && <Field label="پیک تحویل" value={b.courierName} />}
        {b.plannedDeliveryDate && <Field label="تاریخ تحویل" value={fmtDate(b.plannedDeliveryDate)} />}
        {b.requestedDurationDays != null && <Field label="مدت درخواستی" value={`${toPersianNum(b.requestedDurationDays)} روز`} />}
        {b.approvedDurationDays != null && <Field label="مدت تأییدشده" value={`${toPersianNum(b.approvedDurationDays)} روز`} />}
        {b.status === "RECEIVED" && b.dueDate && <Field label="موعد برگرداندن" value={fmtDate(b.dueDate)} danger={overdue} />}
        {b.returnRequestedAt && <Field label="درخواست برگرداندن کتاب" value={`ثبت شد (${fmtDate(b.returnRequestedAt)})`} />}
        {b.returnCourierName && <Field label="پیک برگرداندن" value={b.returnCourierName} />}
        {b.returnPlannedDate && <Field label="زمان برگرداندن" value={fmtDate(b.returnPlannedDate)} />}
        {b.handedOverByUserAt && <Field label="تحویل به پیک" value={`انجام شد (${fmtDate(b.handedOverByUserAt)})`} />}
        {b.status === "RETURNED" && b.returnDate && <Field label="تاریخ برگرداندن" value={fmtDate(b.returnDate)} />}
        {b.status === "REJECTED" && b.rejectionReason && <Field label="دلیل رد" value={b.rejectionReason} wide danger />}
      </div>

      {b.status === "REQUESTED" && (
        <div className="pb-card-actions">
          <p className="pb-hint">در انتظار تأیید کتابدار است. تا قبل از تأیید می‌توانید ویرایش یا لغو کنید.</p>
          <button className="btn btn-outline btn-sm" onClick={() => setEditing(true)}>✏️ ویرایش</button>
          <button className="btn btn-outline btn-sm pb-btn-danger" onClick={askCancel}>لغو</button>
        </div>
      )}

      {b.status === "APPROVED" && (
        <div className="pb-card-actions">
          <p className="pb-hint">وقتی کتاب به دست شما رسید، دریافت را تأیید کنید.</p>
          <button className="btn btn-success btn-sm" onClick={askConfirmReceipt}>✅ دریافت کردم</button>
          <button className="btn btn-outline btn-sm pb-btn-danger" onClick={askCancel}>لغو</button>
        </div>
      )}

      {b.status === "RECEIVED" && !b.returnRequestedAt && (
        <div className="pb-card-actions">
          <p className="pb-hint">هر زمان خواستید (حتی زودتر از موعد) می‌توانید درخواست برگرداندن کتاب بدهید.</p>
          <button className="btn btn-primary btn-sm" onClick={openReturn}>↩️ درخواست برگرداندن کتاب</button>
        </div>
      )}

      {b.status === "RECEIVED" && b.returnRequestedAt && !b.handedOverByUserAt && (
        <div className="pb-card-actions">
          <p className="pb-hint">
            {b.returnPlannedDate
              ? "زمان برگرداندن توسط کتابخانه تعیین شد؛ پس از تحویل کتاب به پیک، گزینهٔ زیر را بزنید."
              : "درخواست برگرداندن کتاب ثبت شد؛ در انتظار تعیین زمان توسط کتابخانه. پس از تحویل به پیک، گزینهٔ زیر را بزنید."}
          </p>
          <button className="btn btn-primary btn-sm" onClick={askHandover}>📤 کتاب را تحویل دادم</button>
          <button className="btn btn-outline btn-sm" onClick={openReturn}>✏️ اصلاح درخواست</button>
          <button className="btn btn-outline btn-sm pb-btn-danger" onClick={askCancelReturn}>لغو درخواست</button>
        </div>
      )}

      {b.status === "RECEIVED" && b.handedOverByUserAt && (
        <div className="pb-card-actions">
          <p className="pb-hint">✅ کتاب به پیک تحویل داده شد ({fmtDate(b.handedOverByUserAt)})؛ در انتظار تأیید نهایی کتابدار.</p>
        </div>
      )}

      <BorrowModal
        isOpen={editing}
        book={{ title: b.bookTitle }}
        editBorrow={b}
        onClose={() => setEditing(false)}
        onSubmit={handleUpdateRequest}
      />

      {returnOpen && (
        <div className="modal-overlay" onClick={() => setReturnOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 460 }}>
            <div className="modal-header">
              <h2>درخواست برگرداندن کتاب</h2>
              <button className="modal-close-btn" onClick={() => setReturnOpen(false)}>✕</button>
            </div>
            <div className="modal-body">
              {returnError && <div className="error-message" style={{ marginBottom: "1rem" }}>{returnError}</div>}
              <p style={{ fontSize: "0.88rem", color: "#6b7280", marginBottom: "1rem" }}>
                کتاب: <strong>{b.bookTitle}</strong>
              </p>
              <div className="bm-form">
                <div className="bm-field">
                  <label>آدرس تحویل کتاب به پیک <span className="bm-req">*</span></label>
                  <textarea rows={2} value={returnForm.returnAddress}
                    onChange={(e) => setReturnForm((f) => ({ ...f, returnAddress: e.target.value }))}
                    placeholder="آدرسی که پیک برای دریافت کتاب مراجعه کند" />
                </div>
                <div className="bm-row">
                  <div className="bm-field">
                    <label>شماره تلفن داخلی</label>
                    <input type="text" inputMode="numeric" maxLength={8} value={returnForm.returnExtension}
                      onChange={(e) => setReturnForm((f) => ({ ...f, returnExtension: e.target.value.replace(/\D/g, "").slice(0, 8) }))}
                      placeholder="۸ رقم" />
                  </div>
                  <div className="bm-field">
                    <label>زمان پیشنهادی (اختیاری)</label>
                    <PersianDateTimePicker value={returnForm.preferredDate}
                      onChange={(iso) => setReturnForm((f) => ({ ...f, preferredDate: iso }))} />
                  </div>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setReturnOpen(false)}>انصراف</button>
              <button className="btn btn-primary" onClick={submitReturn} disabled={returnSaving}>
                {returnSaving ? "در حال ثبت..." : "ثبت درخواست برگرداندن کتاب"}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={!!confirm}
        title={confirm?.title}
        message={confirm?.message}
        confirmLabel={confirm?.confirmLabel}
        variant={confirm?.variant}
        loading={confirmLoading}
        error={confirmError}
        onConfirm={runConfirm}
        onCancel={() => { setConfirm(null); setConfirmError(""); }}
      />
    </div>
  );
};

const Step = ({ label, done, active, last }) => (
  <div className={`pb-step ${done ? "pb-step--done" : active ? "pb-step--active" : ""}`}>
    <span className="pb-step-dot">{done ? "✓" : ""}</span>
    <span className="pb-step-label">{label}</span>
    {!last && <span className="pb-step-line" />}
  </div>
);

const Field = ({ label, value, wide, danger }) => (
  <div className={`pb-field ${wide ? "pb-field--wide" : ""}`}>
    <span className="pb-field-label">{label}</span>
    <span className={`pb-field-value ${danger ? "pb-field-value--danger" : ""}`}>{value}</span>
  </div>
);

export { isOverdue, STATUS_LABELS, STATUS_CLASS, ACTIVE };
export default PhysicalBorrowCard;
