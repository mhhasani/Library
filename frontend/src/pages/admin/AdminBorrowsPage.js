import React, { useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { borrowAPI, libraryAdminAPI } from "../../services/api";
import { toPersian } from "../../utils/persian";
import "./AdminBorrowsPage.css";

const STATUS_LABEL = {
  REQUESTED: "در انتظار",
  APPROVED: "فعال",
  RETURNED: "برگشت‌داده‌شده",
  REJECTED: "رد شده",
  EXPIRED: "منقضی",
};
const STATUS_CLASS = {
  REQUESTED: "badge-warning",
  APPROVED: "badge-success",
  RETURNED: "badge-info",
  REJECTED: "badge-danger",
  EXPIRED: "badge-muted",
};
const TYPE_LABEL = { PHYSICAL: "فیزیکی", DIGITAL: "دیجیتال" };

const TABS = [
  { key: "REQUESTED", label: "⏳ در انتظار تأیید" },
  { key: "APPROVED",  label: "✅ امانت‌های فعال" },
  { key: "ALL",       label: "📋 همه" },
];

const AdminBorrowsPage = () => {
  const { libraryId } = useParams();

  const [activeTab, setActiveTab] = useState("REQUESTED");
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [actionLoading, setActionLoading] = useState({});

  // Reject modal
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState("");

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(""), 3500);
  };

  const fetchBorrows = useCallback(async () => {
    if (!libraryId) return;
    try {
      setLoading(true);
      let res;
      if (activeTab === "REQUESTED") {
        res = await borrowAPI.getPendingBorrows(libraryId);
        setBorrows(res.data?.data || res.data || []);
      } else {
        const status = activeTab === "ALL" ? undefined : activeTab;
        res = await libraryAdminAPI.getAllBorrows(libraryId, status);
        setBorrows(res.data?.data || res.data || []);
      }
      setError("");
    } catch (err) {
      setError("خطا در بارگذاری امانت‌ها");
    } finally {
      setLoading(false);
    }
  }, [libraryId, activeTab]);

  useEffect(() => { fetchBorrows(); }, [fetchBorrows]);

  const handleApprove = async (borrow) => {
    try {
      setActionLoading((p) => ({ ...p, [borrow.id]: "approve" }));
      await borrowAPI.approveBorrow(libraryId, borrow.id);
      showSuccess(`امانت کتاب «${borrow.bookTitle}» تأیید شد`);
      fetchBorrows();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در تأیید");
    } finally {
      setActionLoading((p) => ({ ...p, [borrow.id]: null }));
    }
  };

  const handleRejectConfirm = async () => {
    if (!rejectTarget) return;
    try {
      setActionLoading((p) => ({ ...p, [rejectTarget.id]: "reject" }));
      await borrowAPI.rejectBorrow(libraryId, rejectTarget.id, rejectReason);
      showSuccess(`امانت کتاب «${rejectTarget.bookTitle}» رد شد`);
      setRejectTarget(null);
      setRejectReason("");
      fetchBorrows();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در رد درخواست");
    } finally {
      setActionLoading((p) => ({ ...p, [rejectTarget?.id]: null }));
    }
  };

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">مدیریت امانت‌ها</h1>
        <p className="ap-subtitle">تأیید، رد و پیگیری وضعیت امانت‌های کتابخانه</p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

      {/* Tabs */}
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

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : borrows.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📋</span>
          <p>موردی یافت نشد</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="modern-table">
            <thead>
              <tr>
                <th>کتاب</th>
                <th>کاربر</th>
                <th>نوع</th>
                <th>وضعیت</th>
                <th>تاریخ درخواست</th>
                <th>موعد تحویل</th>
                <th>عملیات</th>
              </tr>
            </thead>
            <tbody>
              {borrows.map((b) => (
                <tr key={b.id} className={b.isOverdue ? "abr-row--overdue" : ""}>
                  <td>
                    <div className="abr-book-title">{b.bookTitle}</div>
                    {b.copyNumber && (
                      <div className="abr-copy">نسخه #{toPersian(b.copyNumber)}</div>
                    )}
                    {b.status === "REJECTED" && b.rejectionReason && (
                      <div style={{ fontSize: "0.72rem", color: "#dc2626", marginTop: "0.2rem" }}>
                        {b.rejectionReason}
                      </div>
                    )}
                  </td>
                  <td style={{ fontSize: "0.82rem" }}>{b.userEmail}</td>
                  <td>
                    <span className="badge badge-info">
                      {TYPE_LABEL[b.borrowType] || b.borrowType}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${STATUS_CLASS[b.status] || "badge-muted"}`}>
                      {STATUS_LABEL[b.status] || b.status}
                    </span>
                    {b.isOverdue && (
                      <span className="badge badge-danger" style={{ marginRight: "0.3rem" }}>
                        تأخیر
                      </span>
                    )}
                  </td>
                  <td style={{ fontSize: "0.82rem", color: "#9ca3af" }}>
                    {b.createdAt
                      ? new Date(b.createdAt).toLocaleDateString("fa-IR")
                      : "—"}
                  </td>
                  <td style={{ fontSize: "0.82rem", color: b.isOverdue ? "#dc2626" : "#9ca3af" }}>
                    {b.dueDate
                      ? new Date(b.dueDate).toLocaleDateString("fa-IR")
                      : "—"}
                  </td>
                  <td>
                    {b.status === "REQUESTED" && (
                      <div className="abr-actions">
                        <button
                          className="btn btn-success btn-sm"
                          onClick={() => handleApprove(b)}
                          disabled={!!actionLoading[b.id]}
                        >
                          {actionLoading[b.id] === "approve" ? "..." : "✓ تأیید"}
                        </button>
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => { setRejectTarget(b); setRejectReason(""); }}
                          disabled={!!actionLoading[b.id]}
                          style={{ color: "#dc2626", borderColor: "#dc2626" }}
                        >
                          رد
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Reject Modal */}
      {rejectTarget && (
        <div className="ap-modal-overlay" onClick={() => setRejectTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">رد درخواست امانت</h2>
            <p style={{ fontSize: "0.88rem", color: "#6b7280", marginBottom: "1rem" }}>
              کتاب: <strong>{rejectTarget.bookTitle}</strong><br />
              کاربر: {rejectTarget.userEmail}
            </p>
            <div className="ap-form-group">
              <label>دلیل رد (اختیاری)</label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={3}
                placeholder="دلیل رد درخواست..."
              />
            </div>
            <div className="ap-modal-actions">
              <button
                className="btn btn-danger"
                onClick={handleRejectConfirm}
                disabled={!!actionLoading[rejectTarget?.id]}
              >
                {actionLoading[rejectTarget?.id] ? "در حال اجرا..." : "رد درخواست"}
              </button>
              <button className="btn btn-outline" onClick={() => setRejectTarget(null)}>
                انصراف
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminBorrowsPage;
