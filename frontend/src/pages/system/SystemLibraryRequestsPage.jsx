import React, { useEffect, useState, useCallback } from "react";
import { libraryRequestAPI } from "../../services/api";
import ConfirmDialog from "../../components/ConfirmDialog";
import { toPersianNum } from "../../utils/persian";
import "../admin/AdminBorrowsPage.css";

const STATUS_LABEL = { PENDING: "در انتظار بررسی", APPROVED: "تأیید شد", REJECTED: "رد شد" };
const STATUS_CLASS = { PENDING: "badge-warning", APPROVED: "badge-success", REJECTED: "badge-danger" };

const TABS = [
  { key: "PENDING", label: "⏳ در انتظار بررسی" },
  { key: "APPROVED", label: "✅ تأییدشده" },
  { key: "REJECTED", label: "❌ ردشده" },
  { key: "ALL", label: "📋 همه" },
];

const SystemLibraryRequestsPage = () => {
  const [tab, setTab] = useState("PENDING");
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [busy, setBusy] = useState({});

  // approve-with-edit modal
  const [approveTarget, setApproveTarget] = useState(null);
  const [form, setForm] = useState({ name: "", description: "", autoMembershipApproval: false, defaultBorrowDurationDays: 14 });

  // reject modal
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState("");

  const showSuccess = (m) => { setSuccessMsg(m); setTimeout(() => setSuccessMsg(""), 3500); };

  const fetchRows = useCallback(async () => {
    try {
      setLoading(true);
      const res = await libraryRequestAPI.all(tab === "ALL" ? undefined : tab);
      setRows(res.data?.data || res.data || []);
      setError("");
    } catch {
      setError("خطا در بارگذاری درخواست‌ها");
    } finally {
      setLoading(false);
    }
  }, [tab]);

  useEffect(() => { fetchRows(); }, [fetchRows]);

  const openApprove = (r) => {
    setApproveTarget(r);
    setForm({
      name: r.name || "",
      description: r.description || "",
      autoMembershipApproval: !!r.autoMembershipApproval,
      defaultBorrowDurationDays: r.defaultBorrowDurationDays || 14,
    });
  };

  const confirmApprove = async () => {
    try {
      setBusy((p) => ({ ...p, [approveTarget.id]: true }));
      await libraryRequestAPI.approve(approveTarget.id, {
        name: form.name,
        description: form.description,
        autoMembershipApproval: form.autoMembershipApproval,
        defaultBorrowDurationDays: Number(form.defaultBorrowDurationDays),
      });
      showSuccess(`کتابخانه «${form.name}» ایجاد شد`);
      setApproveTarget(null);
      fetchRows();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در تأیید درخواست");
    } finally {
      setBusy((p) => ({ ...p, [approveTarget?.id]: false }));
    }
  };

  const confirmReject = async () => {
    try {
      setBusy((p) => ({ ...p, [rejectTarget.id]: true }));
      await libraryRequestAPI.reject(rejectTarget.id, rejectReason);
      showSuccess("درخواست رد شد");
      setRejectTarget(null);
      setRejectReason("");
      fetchRows();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در رد درخواست");
    } finally {
      setBusy((p) => ({ ...p, [rejectTarget?.id]: false }));
    }
  };

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">🏛️ درخواست‌های ایجاد کتابخانه</h1>
        <p className="ap-subtitle">بررسی، اصلاح و تأیید/رد درخواست‌های ایجاد کتابخانه</p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

      <div className="abr-tabs">
        {TABS.map((t) => (
          <button key={t.key} className={`abr-tab ${tab === t.key ? "abr-tab--active" : ""}`} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : rows.length === 0 ? (
        <div className="empty-state"><span className="empty-icon">📋</span><p>موردی یافت نشد</p></div>
      ) : (
        <div className="abr-cards">
          {rows.map((r) => (
            <div key={r.id} className="abr-card">
              <div className="abr-card-head">
                <div>
                  <div className="abr-book-title">{r.name}</div>
                  <div className="abr-user-line">👤 {r.requesterName} · {r.requesterEmail}</div>
                </div>
                <span className={`badge ${STATUS_CLASS[r.status] || "badge-muted"}`}>
                  {STATUS_LABEL[r.status] || r.status}
                </span>
              </div>
              <div className="abr-detail-grid">
                {r.description && <Detail label="توضیحات" value={r.description} wide />}
                <Detail label="مدت امانت پیش‌فرض" value={`${toPersianNum(r.defaultBorrowDurationDays)} روز`} />
                <Detail label="تأیید خودکار عضویت" value={r.autoMembershipApproval ? "فعال" : "غیرفعال"} />
                {r.status === "REJECTED" && r.rejectionReason && <Detail label="دلیل رد" value={r.rejectionReason} wide danger />}
                {r.status === "APPROVED" && r.createdLibraryId && <Detail label="کتابخانه ایجادشده" value={`#${r.createdLibraryId}`} />}
              </div>
              {r.status === "PENDING" && (
                <div className="abr-card-actions">
                  <button className="btn btn-success btn-sm" onClick={() => openApprove(r)}>✓ بررسی و تأیید</button>
                  <button className="btn btn-outline btn-sm" style={{ color: "#dc2626", borderColor: "#dc2626" }}
                    onClick={() => { setRejectTarget(r); setRejectReason(""); }}>رد</button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Approve-with-edit modal */}
      {approveTarget && (
        <div className="ap-modal-overlay" onClick={() => setApproveTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 500 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">بررسی و تأیید درخواست</h2>
            <p style={{ fontSize: "0.85rem", color: "#6b7280", marginBottom: "1rem" }}>
              متقاضی (مالک آینده): <strong>{approveTarget.requesterName}</strong>
            </p>
            <div className="ap-form-grid">
              <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                <label>نام کتابخانه *</label>
                <input value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
              </div>
              <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                <label>توضیحات</label>
                <textarea rows={2} value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
              </div>
              <div className="ap-form-group">
                <label>مدت امانت پیش‌فرض (روز)</label>
                <input type="number" min={1} value={form.defaultBorrowDurationDays}
                  onChange={(e) => setForm((f) => ({ ...f, defaultBorrowDurationDays: e.target.value }))} />
              </div>
              <div className="ap-form-group">
                <label>تأیید خودکار عضویت</label>
                <select value={form.autoMembershipApproval ? "1" : "0"}
                  onChange={(e) => setForm((f) => ({ ...f, autoMembershipApproval: e.target.value === "1" }))}>
                  <option value="0">غیرفعال</option>
                  <option value="1">فعال</option>
                </select>
              </div>
            </div>
            <div className="ap-modal-actions">
              <button className="btn btn-success" onClick={confirmApprove} disabled={!form.name.trim() || busy[approveTarget?.id]}>
                {busy[approveTarget?.id] ? "در حال ایجاد..." : "تأیید و ایجاد کتابخانه"}
              </button>
              <button className="btn btn-outline" onClick={() => setApproveTarget(null)}>انصراف</button>
            </div>
          </div>
        </div>
      )}

      {/* Reject modal */}
      {rejectTarget && (
        <div className="ap-modal-overlay" onClick={() => setRejectTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 440 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">رد درخواست</h2>
            <p style={{ fontSize: "0.85rem", color: "#6b7280", marginBottom: "1rem" }}>
              کتابخانه: <strong>{rejectTarget.name}</strong>
            </p>
            <div className="ap-form-group">
              <label>دلیل رد <span style={{ color: "#dc2626" }}>*</span></label>
              <textarea rows={3} value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} placeholder="دلیل رد (الزامی)..." />
            </div>
            <div className="ap-modal-actions">
              <button className="btn btn-danger" onClick={confirmReject} disabled={!rejectReason.trim() || busy[rejectTarget?.id]}>
                {busy[rejectTarget?.id] ? "در حال اجرا..." : "رد درخواست"}
              </button>
              <button className="btn btn-outline" onClick={() => setRejectTarget(null)}>انصراف</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const Detail = ({ label, value, wide, danger }) => (
  <div className={`abr-detail ${wide ? "abr-detail--wide" : ""}`}>
    <span className="abr-detail-label">{label}</span>
    <span className={`abr-detail-value ${danger ? "abr-detail-value--danger" : ""}`}>{value}</span>
  </div>
);

export default SystemLibraryRequestsPage;
