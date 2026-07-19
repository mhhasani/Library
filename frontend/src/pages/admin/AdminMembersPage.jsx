import React, { useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { libraryAdminAPI, libraryAPI } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import { toPersianNum } from "../../utils/persian";
import { useClientTable } from "../../hooks/useClientTable";
import Pagination from "../../components/Pagination";
import "./AdminMembersPage.css";

const MEMBER_SEARCH_FIELDS = ["userEmail", "userName", (m) => m.role];

const memberInitial = (m) => (m.userName?.trim()?.[0] || m.userEmail?.[0] || "؟").toUpperCase();

const ROLE_LABEL = { ADMIN: "مدیر", MEMBER: "عضو" };
const STATUS_LABEL = { PENDING: "در انتظار", APPROVED: "تأیید شده", REJECTED: "رد شده" };
const STATUS_CLASS = {
  PENDING: "badge-warning",
  APPROVED: "badge-success",
  REJECTED: "badge-danger",
};

const AdminMembersPage = () => {
  const { libraryId } = useParams();
  const { user } = useAuth();

  const [members, setMembers] = useState([]);
  const [ownerId, setOwnerId] = useState(null);
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
      try {
        const libRes = await libraryAPI.getLibrary(libraryId);
        const lib = libRes.data?.data || libRes.data;
        setOwnerId(lib?.ownerId ?? null);
      } catch { /* ignore */ }
      setError("");
    } catch (err) {
      setError("خطا در بارگذاری اعضا");
    } finally {
      setLoading(false);
    }
  }, [libraryId]);

  useEffect(() => { fetchMembers(); }, [fetchMembers]);

  const isSystemAdmin = user?.systemRole === "SYSTEM_ADMIN";
  const canManageRoles = isSystemAdmin || (ownerId != null && user?.id === ownerId);

  const handleSetRole = async (member, role) => {
    try {
      setActionLoading((p) => ({ ...p, [member.userId]: "role" }));
      await libraryAdminAPI.setMemberRole(libraryId, member.userId, role);
      showSuccess(role === "ADMIN"
        ? `«${member.userName?.trim() || member.userEmail}» به مدیر ارتقا یافت`
        : `«${member.userName?.trim() || member.userEmail}» به عضو تغییر یافت`);
      fetchMembers();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در تغییر نقش");
    } finally {
      setActionLoading((p) => ({ ...p, [member.userId]: null }));
    }
  };

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
  const restTable = useClientTable(rest, MEMBER_SEARCH_FIELDS, 10);

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
            <span className="badge badge-warning amm-count">{toPersianNum(pending.length)}</span>
          </h2>
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr>
                  <th>عضو</th>
                  <th>تاریخ درخواست</th>
                  <th>عملیات</th>
                </tr>
              </thead>
              <tbody>
                {pending.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <div className="amm-member">
                        <span className="amm-avatar">{memberInitial(m)}</span>
                        <div>
                          <div className="amm-member-name">{m.userName?.trim() || "—"}</div>
                          <div className="amm-member-email">{m.userEmail}</div>
                        </div>
                      </div>
                    </td>
                    <td className="amm-date">
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
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => { setRejectTarget(m); setRejectReason(""); }}
                          disabled={!!actionLoading[m.userId]}
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
        <h2 className="amm-section-title">👥 همه اعضا ({toPersianNum(rest.length)})</h2>
        <div className="abr-toolbar">
          <input
            className="abr-search"
            placeholder="🔍 جستجوی عضو بر اساس نام، ایمیل یا نقش..."
            value={restTable.query}
            onChange={(e) => restTable.setQuery(e.target.value)}
          />
        </div>
        {rest.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">👥</span>
            <p>هنوز عضوی تأیید نشده است</p>
          </div>
        ) : restTable.pageItems.length === 0 ? (
          <div className="empty-state"><span className="empty-icon">🔍</span><p>عضوی مطابق جستجو یافت نشد</p></div>
        ) : (
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr>
                  <th>عضو</th>
                  <th>نقش</th>
                  <th>وضعیت</th>
                  <th>تاریخ عضویت</th>
                  <th>عملیات</th>
                </tr>
              </thead>
              <tbody>
                {restTable.pageItems.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <div className="amm-member">
                        <span className="amm-avatar">{memberInitial(m)}</span>
                        <div>
                          <div className="amm-member-name">{m.userName?.trim() || "—"}</div>
                          <div className="amm-member-email">{m.userEmail}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      {m.userId === ownerId ? (
                        <span className="badge badge-success">👑 مالک</span>
                      ) : (
                        <span className={`badge ${m.role === "ADMIN" ? "badge-warning" : "badge-info"}`}>
                          {ROLE_LABEL[m.role] || m.role}
                        </span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${STATUS_CLASS[m.status] || "badge-muted"}`}>
                        {STATUS_LABEL[m.status] || m.status}
                      </span>
                    </td>
                    <td className="amm-date">
                      {m.createdAt
                        ? new Date(m.createdAt).toLocaleDateString("fa-IR")
                        : "—"}
                    </td>
                    <td>
                      <div className="amm-actions">
                        {canManageRoles && m.status === "APPROVED" && m.userId !== ownerId && m.role !== "ADMIN" && (
                          <button
                            className="btn btn-outline-success btn-sm"
                            onClick={() => handleSetRole(m, "ADMIN")}
                            disabled={!!actionLoading[m.userId]}
                          >
                            {actionLoading[m.userId] === "role" ? "..." : "⬆️ ارتقا به مدیر"}
                          </button>
                        )}
                        {canManageRoles && m.status === "APPROVED" && m.userId !== ownerId && m.role === "ADMIN" && (
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handleSetRole(m, "MEMBER")}
                            disabled={!!actionLoading[m.userId]}
                          >
                            {actionLoading[m.userId] === "role" ? "..." : "⬇️ تنزل به عضو"}
                          </button>
                        )}
                        {m.status === "APPROVED" && m.role !== "ADMIN" && (
                          <button
                            className="btn btn-outline-danger btn-sm"
                            onClick={() => { setRejectTarget(m); setRejectReason(""); }}
                            disabled={!!actionLoading[m.userId]}
                          >
                            لغو عضویت
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={restTable.page} totalPages={restTable.totalPages} onChange={restTable.setPage} />
          </div>
        )}
      </div>

      {/* Reject Modal */}
      {rejectTarget && (
        <div className="ap-modal-overlay" onClick={() => setRejectTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 420 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">رد / لغو عضویت</h2>
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
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
