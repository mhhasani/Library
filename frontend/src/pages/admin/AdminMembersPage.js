import React, { useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { libraryAdminAPI } from "../../services/api";
import "./AdminMembersPage.css";

const ROLE_LABEL = { ADMIN: "مدیر", MEMBER: "عضو" };
const STATUS_LABEL = { PENDING: "در انتظار", APPROVED: "تأیید شده", REJECTED: "رد شده" };
const STATUS_CLASS = {
  PENDING: "badge-warning",
  APPROVED: "badge-success",
  REJECTED: "badge-danger",
};

const AdminMembersPage = () => {
  const { libraryId } = useParams();

  const [members, setMembers] = useState([]);
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

  const fetchMembers = useCallback(async () => {
    if (!libraryId) return;
    try {
      setLoading(true);
      const res = await libraryAdminAPI.getMembers(libraryId);
      setMembers(res.data?.data || res.data || []);
      setError("");
    } catch (err) {
      setError("خطا در بارگذاری اعضا");
    } finally {
      setLoading(false);
    }
  }, [libraryId]);

  useEffect(() => { fetchMembers(); }, [fetchMembers]);

  const handleApprove = async (member) => {
    try {
      setActionLoading((p) => ({ ...p, [member.userId]: "approve" }));
      await libraryAdminAPI.approveMembership(libraryId, member.userId);
      showSuccess(`عضویت ${member.userEmail} تأیید شد`);
      fetchMembers();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در تأیید عضویت");
    } finally {
      setActionLoading((p) => ({ ...p, [member.userId]: null }));
    }
  };

  const handleRejectConfirm = async () => {
    if (!rejectTarget) return;
    try {
      setActionLoading((p) => ({ ...p, [rejectTarget.userId]: "reject" }));
      await libraryAdminAPI.rejectMembership(libraryId, rejectTarget.userId, rejectReason);
      showSuccess(`عضویت ${rejectTarget.userEmail} رد شد`);
      setRejectTarget(null);
      setRejectReason("");
      fetchMembers();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در رد عضویت");
    } finally {
      setActionLoading((p) => ({ ...p, [rejectTarget?.userId]: null }));
    }
  };

  const pending = members.filter((m) => m.status === "PENDING");
  const rest = members.filter((m) => m.status !== "PENDING");

  if (loading) return <div className="loading">در حال بارگذاری اعضا...</div>;

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">اعضا و درخواست‌های عضویت</h1>
        <p className="ap-subtitle">تأیید یا رد درخواست‌های عضویت و مدیریت اعضا</p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

      {/* Pending requests */}
      {pending.length > 0 && (
        <div className="ap-card amm-section">
          <h2 className="amm-section-title">
            ⏳ درخواست‌های در انتظار
            <span className="badge badge-warning amm-count">{pending.length}</span>
          </h2>
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr>
                  <th>ایمیل</th>
                  <th>نام</th>
                  <th>تاریخ درخواست</th>
                  <th>عملیات</th>
                </tr>
              </thead>
              <tbody>
                {pending.map((m) => (
                  <tr key={m.id}>
                    <td>{m.userEmail}</td>
                    <td>{m.userName?.trim() || "—"}</td>
                    <td style={{ fontSize: "0.82rem", color: "#9ca3af" }}>
                      {m.createdAt
                        ? new Date(m.createdAt).toLocaleDateString("fa-IR")
                        : "—"}
                    </td>
                    <td>
                      <div className="amm-actions">
                        <button
                          className="btn btn-success btn-sm"
                          onClick={() => handleApprove(m)}
                          disabled={!!actionLoading[m.userId]}
                        >
                          {actionLoading[m.userId] === "approve" ? "..." : "✓ تأیید"}
                        </button>
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => { setRejectTarget(m); setRejectReason(""); }}
                          disabled={!!actionLoading[m.userId]}
                          style={{ color: "#dc2626", borderColor: "#dc2626" }}
                        >
                          رد
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* All members */}
      <div className="ap-card">
        <h2 className="amm-section-title">👥 همه اعضا ({rest.length})</h2>
        {rest.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">👥</span>
            <p>هنوز عضوی تأیید نشده است</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr>
                  <th>ایمیل</th>
                  <th>نام</th>
                  <th>نقش</th>
                  <th>وضعیت</th>
                  <th>تاریخ عضویت</th>
                  <th>عملیات</th>
                </tr>
              </thead>
              <tbody>
                {rest.map((m) => (
                  <tr key={m.id}>
                    <td>{m.userEmail}</td>
                    <td>{m.userName?.trim() || "—"}</td>
                    <td>
                      <span className="badge badge-info">
                        {ROLE_LABEL[m.role] || m.role}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${STATUS_CLASS[m.status] || "badge-muted"}`}>
                        {STATUS_LABEL[m.status] || m.status}
                      </span>
                    </td>
                    <td style={{ fontSize: "0.82rem", color: "#9ca3af" }}>
                      {m.createdAt
                        ? new Date(m.createdAt).toLocaleDateString("fa-IR")
                        : "—"}
                    </td>
                    <td>
                      {m.status === "APPROVED" && m.role !== "ADMIN" && (
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => { setRejectTarget(m); setRejectReason(""); }}
                          disabled={!!actionLoading[m.userId]}
                          style={{ fontSize: "0.78rem", color: "#dc2626", borderColor: "#dc2626" }}
                        >
                          لغو عضویت
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Reject Modal */}
      {rejectTarget && (
        <div className="ap-modal-overlay" onClick={() => setRejectTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">رد / لغو عضویت</h2>
            <p style={{ fontSize: "0.88rem", color: "#6b7280", marginBottom: "1rem" }}>
              کاربر: <strong>{rejectTarget.userEmail}</strong>
            </p>
            <div className="ap-form-group">
              <label>دلیل (اختیاری)</label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={3}
                placeholder="دلیل رد یا لغو عضویت را بنویسید..."
              />
            </div>
            <div className="ap-modal-actions">
              <button
                className="btn btn-danger"
                onClick={handleRejectConfirm}
                disabled={!!actionLoading[rejectTarget?.userId]}
              >
                {actionLoading[rejectTarget?.userId] ? "در حال اجرا..." : "تأیید رد"}
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

export default AdminMembersPage;
