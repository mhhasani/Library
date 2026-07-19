import React, { useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { borrowAPI } from "../../services/api";
import { toPersian } from "../../utils/persian";
import { useDebounce } from "../../hooks/useDebounce";
import ConfirmDialog from "../../components/ConfirmDialog";
import PersianDateTimePicker from "../../components/PersianDateTimePicker";
import Pagination from "../../components/Pagination";
import FieldHint from "../../components/FieldHint";
import "./AdminBorrowsPage.css";

const STATUS_LABEL = {
  REQUESTED: "در انتظار بررسی",
  APPROVED: "تأییدشده — در انتظار دریافت",
  RECEIVED: "در دست گیرنده",
  RETURNED: "بازگردانده‌شده",
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

// RECEIVED alone reads "در دست گیرنده", but once the recipient requests a return / hands the
// book to the courier the badge must reflect that, otherwise it contradicts the row details.
const statusBadge = (b) => {
  if (b.status === "RECEIVED") {
    if (b.handedOverByUserAt) return { label: "در مسیر بازگشت", cls: "badge-info" };
    if (b.returnRequestedAt) return { label: "درخواست برگرداندن", cls: "badge-info" };
    if (b.dueDate && new Date(b.dueDate) < new Date()) return { label: "در دست گیرنده — سررسید گذشته", cls: "badge-danger" };
  }
  return { label: STATUS_LABEL[b.status] || b.status, cls: STATUS_CLASS[b.status] || "badge-muted" };
};

const FINISHED = ["RETURNED", "REJECTED", "CANCELLED", "EXPIRED"];

const TABS = [
  { key: "REVIEW", label: "🔔 در انتظار بررسی" },
  { key: "ACTIVE", label: "✅ امانت‌های فعال" },
  { key: "OVERDUE", label: "⏰ سررسید گذشته" },
  { key: "COMPLETED", label: "🏁 به اتمام رسیده" },
  { key: "ALL", label: "📋 همه" },
];

const REJECT_PRESETS = [
  "عدم موجودی کتاب",
  "اطلاعات تحویل ناقص است",
  "سقف امانت کاربر تکمیل است",
  "کتاب در دسترس امانت نیست",
];

const fmtDate = (d) => (d ? new Date(d).toLocaleDateString("fa-IR") : "—");

const AdminPhysicalBorrowsPage = () => {
  const { libraryId } = useParams();

  const [activeTab, setActiveTab] = useState("REVIEW");
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [modalError, setModalError] = useState(""); // shown inside the open modal, not on the page behind it
  const [successMsg, setSuccessMsg] = useState("");
  const [actionLoading, setActionLoading] = useState({});
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const debouncedSearch = useDebounce(search, 400);

  // approve modal
  const [approveTarget, setApproveTarget] = useState(null);
  const [approveForm, setApproveForm] = useState({
    plannedDeliveryDate: "",
    approvedDurationDays: 14,
    copyUniqueCode: "",
    courierName: "",
  });

  // delivery (courier) modal
  const [deliveryTarget, setDeliveryTarget] = useState(null);
  const [deliveryForm, setDeliveryForm] = useState({
    courierName: "",
    copyUniqueCode: "",
    plannedDeliveryDate: "",
  });

  // reject modal
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState("");

  // generic confirm dialog
  const [confirm, setConfirm] = useState(null);
  const [confirmLoading, setConfirmLoading] = useState(false);

  // borrower summary (shown in approve/reject modals)
  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(false);

  // return-pickup scheduling modal
  const [returnTarget, setReturnTarget] = useState(null);
  const [returnForm, setReturnForm] = useState({ returnCourierName: "", returnPlannedDate: "" });

  // event-log modal
  const [logTarget, setLogTarget] = useState(null);
  const [logEvents, setLogEvents] = useState([]);
  const [logLoading, setLogLoading] = useState(false);

  const openLog = async (b) => {
    setLogTarget(b);
    setLogEvents([]);
    try {
      setLogLoading(true);
      const res = await borrowAPI.getBorrowEvents(libraryId, b.id);
      setLogEvents(res.data?.data || res.data || []);
    } catch {
      setLogEvents([]);
    } finally {
      setLogLoading(false);
    }
  };

  const loadSummary = useCallback(async (userId) => {
    setSummary(null);
    if (!userId) return;
    try {
      setSummaryLoading(true);
      const res = await borrowAPI.getBorrowerSummary(libraryId, userId);
      setSummary(res.data?.data || res.data || null);
    } catch {
      setSummary(null);
    } finally {
      setSummaryLoading(false);
    }
  }, [libraryId]);

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(""), 3500);
  };

  const TAB_STATUSES = {
    ACTIVE: ["APPROVED", "RECEIVED"],
    COMPLETED: FINISHED,
    ALL: null,
  };

  const fetchBorrows = useCallback(async () => {
    if (!libraryId) return;
    try {
      setLoading(true);
      const statuses = TAB_STATUSES[activeTab];
      const res = await borrowAPI.searchLibraryBorrows(libraryId, {
        type: "PHYSICAL",
        statuses: statuses ? statuses.join(",") : undefined,
        needsAttention: activeTab === "REVIEW" ? true : undefined,
        overdue: activeTab === "OVERDUE" ? true : undefined,
        search: debouncedSearch || undefined,
        page,
        size: 15,
      });
      const data = res.data?.data || res.data;
      setBorrows(data?.content || []);
      setTotalPages(data?.totalPages ?? 0);
      setError("");
    } catch {
      setError("خطا در بارگذاری امانت‌ها");
    } finally {
      setLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [libraryId, activeTab, debouncedSearch, page]);

  useEffect(() => { fetchBorrows(); }, [fetchBorrows]);
  useEffect(() => { setPage(0); }, [activeTab, debouncedSearch]);

  // ---- Approve ----
  const openApprove = (b) => {
    setModalError("");
    setApproveTarget(b);
    setApproveForm({
      plannedDeliveryDate: "",
      approvedDurationDays: b.requestedDurationDays || 14,
      copyUniqueCode: "",
      courierName: "",
    });
    loadSummary(b.userId);
  };

  const openReject = (b) => {
    setModalError("");
    setRejectTarget(b);
    setRejectReason("");
    loadSummary(b.userId);
  };

  const askCancel = (b) => {
    setConfirm({
      title: "لغو امانت",
      message: `آیا امانت کتاب «${b.bookTitle}» را لغو می‌کنید؟ این عمل نسخه را آزاد می‌کند.`,
      confirmLabel: "بله، لغو کن",
      variant: "danger",
      onConfirm: async () => {
        await borrowAPI.cancelByLibrarian(libraryId, b.id);
        showSuccess(`امانت «${b.bookTitle}» لغو شد`);
        fetchBorrows();
      },
    });
  };

  // ---- Return-pickup scheduling ----
  const openReturnSchedule = (b) => {
    setModalError("");
    setReturnTarget(b);
    setReturnForm({
      returnCourierName: b.returnCourierName || "",
      returnPlannedDate: "",
    });
  };
  const handleReturnSchedule = async () => {
    if (!returnTarget) return;
    try {
      setActionLoading((p) => ({ ...p, [returnTarget.id]: "rsched" }));
      await borrowAPI.scheduleReturnPickup(libraryId, returnTarget.id, {
        returnCourierName: returnForm.returnCourierName || null,
        returnPlannedDate: returnForm.returnPlannedDate || null,
      });
      showSuccess("زمان برگرداندن تعیین شد");
      setReturnTarget(null);
      fetchBorrows();
    } catch (err) {
      setModalError(err.response?.data?.message || "خطا در تعیین زمان برگرداندن");
    } finally {
      setActionLoading((p) => ({ ...p, [returnTarget?.id]: null }));
    }
  };
  const handleApproveConfirm = async () => {
    if (!approveTarget) return;
    if (!approveForm.plannedDeliveryDate) {
      setModalError("تاریخ تحویل را وارد کنید");
      return;
    }
    if (!approveForm.approvedDurationDays || approveForm.approvedDurationDays < 1) {
      setModalError("تعداد روز امانت نامعتبر است");
      return;
    }
    try {
      setActionLoading((p) => ({ ...p, [approveTarget.id]: "approve" }));
      await borrowAPI.approvePhysical(libraryId, approveTarget.id, {
        plannedDeliveryDate: approveForm.plannedDeliveryDate,
        approvedDurationDays: Number(approveForm.approvedDurationDays),
        copyUniqueCode: approveForm.copyUniqueCode || null,
        courierName: approveForm.courierName || null,
      });
      showSuccess(`امانت «${approveTarget.bookTitle}» تأیید شد`);
      setApproveTarget(null);
      fetchBorrows();
    } catch (err) {
      setModalError(err.response?.data?.message || "خطا در تأیید");
    } finally {
      setActionLoading((p) => ({ ...p, [approveTarget?.id]: null }));
    }
  };

  // ---- Delivery details ----
  const openDelivery = (b) => {
    setModalError("");
    setDeliveryTarget(b);
    setDeliveryForm({
      courierName: b.courierName || "",
      copyUniqueCode: b.copyUniqueCode || "",
      plannedDeliveryDate: "",
    });
  };
  const handleDeliveryConfirm = async () => {
    if (!deliveryTarget) return;
    try {
      setActionLoading((p) => ({ ...p, [deliveryTarget.id]: "delivery" }));
      await borrowAPI.updateDelivery(libraryId, deliveryTarget.id, {
        courierName: deliveryForm.courierName || null,
        copyUniqueCode: deliveryForm.copyUniqueCode || null,
        plannedDeliveryDate: deliveryForm.plannedDeliveryDate || null,
      });
      showSuccess("جزئیات تحویل به‌روزرسانی شد");
      setDeliveryTarget(null);
      fetchBorrows();
    } catch (err) {
      setModalError(err.response?.data?.message || "خطا در به‌روزرسانی");
    } finally {
      setActionLoading((p) => ({ ...p, [deliveryTarget?.id]: null }));
    }
  };

  // ---- Confirm return (with confirmation dialog) ----
  const askConfirmReturn = (b) => {
    setConfirm({
      title: "ثبت برگرداندن کتاب",
      message: `آیا کتاب «${b.bookTitle}» را از گیرنده تحویل گرفته‌اید؟ با تأیید، دوره امانت پایان می‌یابد و نسخه آزاد می‌شود.`,
      confirmLabel: "بله، تحویل گرفتم",
      variant: "primary",
      onConfirm: async () => {
        await borrowAPI.confirmReturn(libraryId, b.id);
        showSuccess(`برگرداندن کتاب «${b.bookTitle}» ثبت شد`);
        fetchBorrows();
      },
    });
  };

  const askCancelReturn = (b) => {
    setConfirm({
      title: "لغو درخواست برگرداندن",
      message: `آیا درخواست برگرداندن کتاب «${b.bookTitle}» لغو شود؟ امانت همچنان فعال می‌ماند.`,
      confirmLabel: "بله، لغو کن", variant: "danger",
      onConfirm: async () => {
        await borrowAPI.cancelReturnRequest(libraryId, b.id);
        showSuccess("درخواست برگرداندن لغو شد");
        fetchBorrows();
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
      setError(err.response?.data?.message || "خطا در انجام عملیات");
      setConfirm(null);
    } finally {
      setConfirmLoading(false);
    }
  };

  // ---- Reject ----
  const handleRejectConfirm = async () => {
    if (!rejectTarget) return;
    try {
      setActionLoading((p) => ({ ...p, [rejectTarget.id]: "reject" }));
      await borrowAPI.rejectBorrow(libraryId, rejectTarget.id, rejectReason);
      showSuccess(`امانت «${rejectTarget.bookTitle}» رد شد`);
      setRejectTarget(null);
      setRejectReason("");
      fetchBorrows();
    } catch (err) {
      setModalError(err.response?.data?.message || "خطا در رد درخواست");
    } finally {
      setActionLoading((p) => ({ ...p, [rejectTarget?.id]: null }));
    }
  };

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">📦 امانت‌ها</h1>
        <p className="ap-subtitle">تأیید، تعیین پیک، و ثبت برگرداندن کتاب‌های فیزیکی</p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

      <div className="abr-tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            className={`abr-tab ${activeTab === tab.key ? "abr-tab--active" : ""}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="abr-toolbar">
        <input
          className="abr-search"
          placeholder="🔍 جستجو بر اساس کد رهگیری، نام/ایمیل/تلفن کاربر، عنوان کتاب یا کد نسخه..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : borrows.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📋</span>
          <p>موردی یافت نشد</p>
        </div>
      ) : (
        <div className="abr-cards">
          {borrows.map((b) => (
            <div key={b.id} className={`abr-card ${b.isOverdue ? "abr-card--overdue" : ""}`}>
              <div className="abr-card-head">
                <div>
                  <div className="abr-book-title">{b.bookTitle}</div>
                  {b.trackingCode && <div className="abr-tracking">کد رهگیری: {b.trackingCode}</div>}
                  <div className="abr-user-line">
                    👤 {b.userFullName || b.userEmail}
                    {b.userPhone && <span> · 📱 {b.userPhone}</span>}
                  </div>
                </div>
                {(() => { const s = statusBadge(b); return (
                  <span className={`badge ${s.cls}`}>
                    {s.label}{b.isOverdue ? " · تأخیر" : ""}
                  </span>
                ); })()}
              </div>

              <div className="abr-detail-grid">
                {b.deliveryAddress && <Detail label="محل تحویل" value={b.deliveryAddress} wide hintKey="deliveryAddress" />}
                {b.deliveryExtension && <Detail label="تلفن داخلی" value={toPersian(b.deliveryExtension)} hintKey="deliveryExtension" />}
                {b.copyNumber != null && <Detail label="نسخه" value={`#${toPersian(b.copyNumber)}`} hintKey="copyNumber" />}
                {b.copyUniqueCode && <Detail label="کد نسخه" value={b.copyUniqueCode} hintKey="copyUniqueCode" />}
                {b.courierName && <Detail label="پیک" value={b.courierName} hintKey="courierName" />}
                {b.requestedDurationDays != null && (
                  <Detail label="مدت درخواستی" value={`${toPersian(b.requestedDurationDays)} روز`} hintKey="requestedDurationDays" />
                )}
                {b.approvedDurationDays != null && (
                  <Detail label="مدت تأییدشده" value={`${toPersian(b.approvedDurationDays)} روز`} hintKey="approvedDurationDays" />
                )}
                {b.plannedDeliveryDate && (
                  <Detail label="تاریخ تحویل" value={fmtDate(b.plannedDeliveryDate)} hintKey="plannedDeliveryDate" />
                )}
                {b.status === "RECEIVED" && b.dueDate && (
                  <Detail label="موعد برگرداندن" value={fmtDate(b.dueDate)} danger={b.isOverdue} hintKey="dueDate" />
                )}
                {b.returnRequestedAt && (
                  <Detail label="درخواست برگرداندن کتاب" value={`ثبت شد (${fmtDate(b.returnRequestedAt)})`} hintKey="returnRequestedAt" />
                )}
                {b.returnAddress && <Detail label="آدرس برگرداندن" value={b.returnAddress} wide hintKey="returnAddress" />}
                {b.returnPreferredDate && <Detail label="زمان پیشنهادی کاربر" value={fmtDate(b.returnPreferredDate)} hintKey="returnPreferredDate" />}
                {b.returnCourierName && <Detail label="پیک برگرداندن" value={b.returnCourierName} hintKey="returnCourierName" />}
                {b.returnPlannedDate && <Detail label="زمان برگرداندن" value={fmtDate(b.returnPlannedDate)} hintKey="returnPlannedDate" />}
                {b.handedOverByUserAt && <Detail label="تحویل به پیک" value={`انجام شد (${fmtDate(b.handedOverByUserAt)}) — در مسیر`} hintKey="handedOverByUserAt" />}
                {b.status === "RETURNED" && b.returnDate && <Detail label="تاریخ برگرداندن" value={fmtDate(b.returnDate)} hintKey="returnDate" />}
                {b.status === "REJECTED" && b.rejectionReason && (
                  <Detail label="دلیل رد" value={b.rejectionReason} wide danger hintKey="rejectionReason" />
                )}
              </div>

              <div className="abr-card-actions">
                <button className="btn btn-ghost btn-sm" onClick={() => openLog(b)}>
                  📜 مشاهده لاگ
                </button>
                {b.status === "REQUESTED" && (
                  <>
                    <button className="btn btn-success btn-sm" onClick={() => openApprove(b)}>
                      ✓ تأیید با جزئیات تحویل
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => openReject(b)}
                    >
                      رد درخواست
                    </button>
                  </>
                )}
                {b.status === "APPROVED" && (
                  <>
                    <button className="btn btn-outline btn-sm" onClick={() => openDelivery(b)}>
                      {b.courierName ? "✏️ ویرایش پیک" : "🚚 افزودن پیک"}
                    </button>
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={actionLoading[b.id] === "return"}
                      onClick={() => askConfirmReturn(b)}
                    >
                      📦 کتاب را تحویل گرفتم
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => askCancel(b)}
                    >
                      لغو
                    </button>
                  </>
                )}
                {b.status === "RECEIVED" && (
                  <>
                    <button className="btn btn-outline btn-sm" onClick={() => openReturnSchedule(b)}>
                      🚚 {b.returnRequestedAt
                        ? (b.returnCourierName ? "ویرایش زمان برگرداندن" : "تعیین پیک/زمان برگرداندن")
                        : "ثبت درخواست برگرداندن"}
                    </button>
                    {b.returnRequestedAt && (
                      <button className="btn btn-outline-danger btn-sm"
                        onClick={() => askCancelReturn(b)}>
                        لغو درخواست برگرداندن
                      </button>
                    )}
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={actionLoading[b.id] === "return"}
                      onClick={() => askConfirmReturn(b)}
                    >
                      {actionLoading[b.id] === "return" ? "..." : "📦 کتاب را تحویل گرفتم"}
                    </button>
                    <button
                      className="btn btn-outline-danger btn-sm"
                      onClick={() => askCancel(b)}
                    >
                      لغو
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && <Pagination page={page} totalPages={totalPages} onChange={setPage} />}

      {/* Approve modal */}
      {approveTarget && (
        <div className="ap-modal-overlay" onClick={() => setApproveTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 480 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">تأیید امانت</h2>
            {modalError && <div className="error-message" style={{ marginBottom: "1rem" }}>{modalError}</div>}
            {modalError && <div className="error-message" style={{ marginBottom: "0.75rem" }}>{modalError}</div>}
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
              کتاب: <strong>{approveTarget.bookTitle}</strong><br />
              گیرنده: {approveTarget.userFullName || approveTarget.userEmail}
              {approveTarget.requestedDurationDays != null && (
                <> · درخواست کاربر: {toPersian(approveTarget.requestedDurationDays)} روز</>
              )}
            </p>
            <SummaryPanel loading={summaryLoading} s={summary} />
            <div className="ap-form-grid">
              <div className="ap-form-group">
                <label>تاریخ تحویل *</label>
                <PersianDateTimePicker
                  value={approveForm.plannedDeliveryDate}
                  onChange={(iso) => setApproveForm((f) => ({ ...f, plannedDeliveryDate: iso }))}
                />
              </div>
              <div className="ap-form-group">
                <label>مدت امانت (روز) *</label>
                <input
                  type="number"
                  min={1}
                  value={approveForm.approvedDurationDays}
                  onChange={(e) => setApproveForm((f) => ({ ...f, approvedDurationDays: e.target.value }))}
                />
              </div>
              <div className="ap-form-group">
                <label>کد یکتای نسخه</label>
                <input
                  type="text"
                  value={approveForm.copyUniqueCode}
                  onChange={(e) => setApproveForm((f) => ({ ...f, copyUniqueCode: e.target.value }))}
                  placeholder="مثال: LIB-A-00123"
                />
              </div>
              <div className="ap-form-group">
                <label>نام پیک (اختیاری)</label>
                <input
                  type="text"
                  value={approveForm.courierName}
                  onChange={(e) => setApproveForm((f) => ({ ...f, courierName: e.target.value }))}
                  placeholder="می‌توانید بعداً وارد کنید"
                />
              </div>
            </div>
            <div className="ap-modal-actions">
              <button
                className="btn btn-success"
                onClick={handleApproveConfirm}
                disabled={actionLoading[approveTarget?.id] === "approve"}
              >
                {actionLoading[approveTarget?.id] === "approve" ? "در حال ثبت..." : "تأیید امانت"}
              </button>
              <button className="btn btn-outline" onClick={() => setApproveTarget(null)}>انصراف</button>
            </div>
          </div>
        </div>
      )}

      {/* Delivery modal */}
      {deliveryTarget && (
        <div className="ap-modal-overlay" onClick={() => setDeliveryTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 460 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">جزئیات تحویل / پیک</h2>
            {modalError && <div className="error-message" style={{ marginBottom: "1rem" }}>{modalError}</div>}
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
              کتاب: <strong>{deliveryTarget.bookTitle}</strong>
            </p>
            <div className="ap-form-grid">
              <div className="ap-form-group">
                <label>نام پیک</label>
                <input
                  type="text"
                  value={deliveryForm.courierName}
                  onChange={(e) => setDeliveryForm((f) => ({ ...f, courierName: e.target.value }))}
                />
              </div>
              <div className="ap-form-group">
                <label>کد یکتای نسخه</label>
                <input
                  type="text"
                  value={deliveryForm.copyUniqueCode}
                  onChange={(e) => setDeliveryForm((f) => ({ ...f, copyUniqueCode: e.target.value }))}
                />
              </div>
              <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                <label>تاریخ تحویل (اختیاری)</label>
                <PersianDateTimePicker
                  value={deliveryForm.plannedDeliveryDate}
                  onChange={(iso) => setDeliveryForm((f) => ({ ...f, plannedDeliveryDate: iso }))}
                />
              </div>
            </div>
            <div className="ap-modal-actions">
              <button
                className="btn btn-primary"
                onClick={handleDeliveryConfirm}
                disabled={actionLoading[deliveryTarget?.id] === "delivery"}
              >
                {actionLoading[deliveryTarget?.id] === "delivery" ? "در حال ثبت..." : "ذخیره"}
              </button>
              <button className="btn btn-outline" onClick={() => setDeliveryTarget(null)}>انصراف</button>
            </div>
          </div>
        </div>
      )}

      {/* Reject modal */}
      {rejectTarget && (
        <div className="ap-modal-overlay" onClick={() => setRejectTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 440 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">رد درخواست امانت</h2>
            {modalError && <div className="error-message" style={{ marginBottom: "1rem" }}>{modalError}</div>}
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
              کتاب: <strong>{rejectTarget.bookTitle}</strong><br />
              کاربر: {rejectTarget.userFullName || rejectTarget.userEmail}
            </p>
            <SummaryPanel loading={summaryLoading} s={summary} />
            <div className="abr-presets">
              {REJECT_PRESETS.map((r) => (
                <button key={r} type="button" className="abr-chip" onClick={() => setRejectReason(r)}>
                  {r}
                </button>
              ))}
            </div>
            <div className="ap-form-group">
              <label>دلیل رد <span style={{ color: "var(--color-danger)" }}>*</span></label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={3}
                placeholder="دلیل رد درخواست (الزامی)..."
              />
            </div>
            <div className="ap-modal-actions">
              <button
                className="btn btn-danger"
                onClick={handleRejectConfirm}
                disabled={!rejectReason.trim() || actionLoading[rejectTarget?.id] === "reject"}
              >
                {actionLoading[rejectTarget?.id] === "reject" ? "در حال اجرا..." : "رد درخواست"}
              </button>
              <button className="btn btn-outline" onClick={() => setRejectTarget(null)}>انصراف</button>
            </div>
          </div>
        </div>
      )}

      {/* Return-pickup schedule modal */}
      {returnTarget && (
        <div className="ap-modal-overlay" onClick={() => setReturnTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 460 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">تعیین زمان برگرداندن کتاب</h2>
            {modalError && <div className="error-message" style={{ marginBottom: "1rem" }}>{modalError}</div>}
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
              کتاب: <strong>{returnTarget.bookTitle}</strong><br />
              گیرنده: {returnTarget.userFullName || returnTarget.userEmail}
              {returnTarget.returnAddress && <><br />آدرس برگرداندن: {returnTarget.returnAddress}</>}
              {returnTarget.returnPreferredDate && <><br />زمان پیشنهادی کاربر: {fmtDate(returnTarget.returnPreferredDate)}</>}
            </p>
            <div className="ap-form-grid">
              <div className="ap-form-group">
                <label>نام پیک برگرداندن</label>
                <input
                  type="text"
                  value={returnForm.returnCourierName}
                  onChange={(e) => setReturnForm((f) => ({ ...f, returnCourierName: e.target.value }))}
                  placeholder="نام پیک"
                />
              </div>
              <div className="ap-form-group">
                <label>تاریخ و ساعت برگرداندن</label>
                <PersianDateTimePicker
                  value={returnForm.returnPlannedDate}
                  onChange={(iso) => setReturnForm((f) => ({ ...f, returnPlannedDate: iso }))}
                />
              </div>
            </div>
            <div className="ap-modal-actions">
              <button
                className="btn btn-primary"
                onClick={handleReturnSchedule}
                disabled={actionLoading[returnTarget?.id] === "rsched"}
              >
                {actionLoading[returnTarget?.id] === "rsched" ? "در حال ثبت..." : "ثبت زمان برگرداندن"}
              </button>
              <button className="btn btn-outline" onClick={() => setReturnTarget(null)}>انصراف</button>
            </div>
          </div>
        </div>
      )}

      {/* Event-log modal */}
      {logTarget && (
        <div className="ap-modal-overlay" onClick={() => setLogTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 520 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">📜 تاریخچهٔ امانت</h2>
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
              کتاب: <strong>{logTarget.bookTitle}</strong> · گیرنده: {logTarget.userFullName || logTarget.userEmail}
            </p>
            {logLoading ? (
              <div className="loading">در حال بارگذاری...</div>
            ) : logEvents.length === 0 ? (
              <p className="ap-subtitle">رویدادی ثبت نشده است.</p>
            ) : (
              <div className="abr-timeline">
                {logEvents.map((e) => (
                  <div key={e.id} className="abr-tl-item">
                    <span className="abr-tl-dot" />
                    <div className="abr-tl-body">
                      <div className="abr-tl-head">
                        <span className="abr-tl-title">{e.title}</span>
                        <span className="abr-tl-time">
                          {e.createdAt ? new Date(e.createdAt).toLocaleString("fa-IR") : ""}
                        </span>
                      </div>
                      {e.detail && <div className="abr-tl-detail">{e.detail}</div>}
                      {e.actorName && <div className="abr-tl-actor">👤 {e.actorName}</div>}
                    </div>
                  </div>
                ))}
              </div>
            )}
            <div className="ap-modal-actions">
              <button className="btn btn-outline" onClick={() => setLogTarget(null)}>بستن</button>
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
        onConfirm={runConfirm}
        onCancel={() => setConfirm(null)}
      />
    </div>
  );
};

const SummaryPanel = ({ loading, s }) => {
  if (loading) return <div className="abr-summary abr-summary--loading">در حال دریافت سابقهٔ کاربر...</div>;
  if (!s) return null;
  return (
    <div className="abr-summary">
      <div className="abr-summary-title">سابقهٔ این کاربر در کتابخانه</div>
      <div className="abr-summary-chips">
        <span className="abr-sum-chip">امانت فعال فیزیکی: {toPersian(s.activePhysical)}</span>
        <span className="abr-sum-chip">در انتظار: {toPersian(s.pending)}</span>
        {s.overdue > 0 && <span className="abr-sum-chip abr-sum-chip--danger">تأخیردار: {toPersian(s.overdue)}</span>}
        <span className="abr-sum-chip">بازگردانده: {toPersian(s.returned)}</span>
        <span className="abr-sum-chip">لغو/رد: {toPersian((s.cancelled || 0) + (s.rejected || 0))}</span>
        <span className="abr-sum-chip">کل: {toPersian(s.totalBorrows)}</span>
      </div>
      {s.activeLoans && s.activeLoans.length > 0 && (
        <div className="abr-summary-loans">
          کتاب‌های در دست کاربر: {s.activeLoans.map((l) => l.bookTitle).join("، ")}
        </div>
      )}
    </div>
  );
};

const Detail = ({ label, value, wide, danger, hintKey }) => (
  <div className={`abr-detail ${wide ? "abr-detail--wide" : ""}`}>
    <span className="abr-detail-label">
      {label}
      {hintKey && <FieldHint hintKey={hintKey} />}
    </span>
    <span className={`abr-detail-value ${danger ? "abr-detail-value--danger" : ""}`}>{value}</span>
  </div>
);

export default AdminPhysicalBorrowsPage;
