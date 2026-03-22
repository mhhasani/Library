import React, { useEffect, useState, useCallback } from "react";
import { adminAPI } from "../../services/api";
import "./SystemUsersPage.css";

const ROLE_LABELS = { SYSTEM_ADMIN: "مدیر سیستم", USER: "کاربر عادی" };
const STATUS_LABELS = {
  ACTIVE: "فعال",
  SUSPENDED: "معلق",
  DELETED: "حذف شده",
  PENDING_VERIFICATION: "در انتظار تأیید",
};
const STATUS_CLASS = {
  ACTIVE: "badge-success",
  SUSPENDED: "badge-warning",
  DELETED: "badge-danger",
  PENDING_VERIFICATION: "badge-info",
};

const STATUS_FILTERS = [
  { value: "", label: "همه" },
  { value: "ACTIVE", label: "فعال" },
  { value: "SUSPENDED", label: "معلق" },
];

const SystemUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [statusFilter, setStatusFilter] = useState("ACTIVE");
  const [actionLoading, setActionLoading] = useState({});

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(""), 3500);
  };

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const res = await adminAPI.getUsers(statusFilter || undefined);
      setUsers(res.data?.data || res.data || []);
      setError("");
    } catch (err) {
      setError("خطا در بارگذاری کاربران");
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const handleToggleStatus = async (user) => {
    const newStatus = user.accountStatus === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
    try {
      setActionLoading((p) => ({ ...p, [user.id]: "status" }));
      await adminAPI.updateUserStatus(user.id, newStatus);
      showSuccess(
        `وضعیت ${user.email} به «${STATUS_LABELS[newStatus]}» تغییر کرد`
      );
      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در تغییر وضعیت");
    } finally {
      setActionLoading((p) => ({ ...p, [user.id]: null }));
    }
  };

  const handleToggleRole = async (user) => {
    const newRole =
      user.systemRole === "SYSTEM_ADMIN" ? "USER" : "SYSTEM_ADMIN";
    const label =
      newRole === "SYSTEM_ADMIN" ? "مدیر سیستم" : "کاربر عادی";
    if (
      !window.confirm(
        `آیا نقش ${user.email} را به «${label}» تغییر می‌دهید؟`
      )
    )
      return;
    try {
      setActionLoading((p) => ({ ...p, [user.id]: "role" }));
      await adminAPI.updateUserRole(user.id, newRole);
      showSuccess(`نقش ${user.email} به «${label}» تغییر کرد`);
      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در تغییر نقش");
    } finally {
      setActionLoading((p) => ({ ...p, [user.id]: null }));
    }
  };

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">مدیریت کاربران</h1>
        <p className="ap-subtitle">مشاهده، تعلیق و مدیریت نقش کاربران سامانه</p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

      {/* Toolbar */}
      <div className="ap-toolbar">
        <div className="su-filter-group">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              className={`su-filter-btn ${statusFilter === f.value ? "su-filter-btn--active" : ""}`}
              onClick={() => setStatusFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
        <span className="su-count">
          {users.length} کاربر
        </span>
      </div>

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : users.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">👥</span>
          <p>کاربری یافت نشد</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="modern-table">
            <thead>
              <tr>
                <th>#</th>
                <th>ایمیل</th>
                <th>نام</th>
                <th>تلفن</th>
                <th>نقش</th>
                <th>وضعیت</th>
                <th>آخرین ورود</th>
                <th>عملیات</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td style={{ fontSize: "0.75rem", color: "#9ca3af" }}>{u.id}</td>
                  <td style={{ fontWeight: 500 }}>{u.email}</td>
                  <td>
                    {u.firstName || u.lastName
                      ? `${u.firstName || ""} ${u.lastName || ""}`.trim()
                      : "—"}
                  </td>
                  <td style={{ fontSize: "0.82rem" }}>{u.phoneNumber || "—"}</td>
                  <td>
                    <span
                      className={`badge ${
                        u.systemRole === "SYSTEM_ADMIN"
                          ? "badge-warning"
                          : "badge-info"
                      }`}
                    >
                      {ROLE_LABELS[u.systemRole] || u.systemRole}
                    </span>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        STATUS_CLASS[u.accountStatus] || "badge-muted"
                      }`}
                    >
                      {STATUS_LABELS[u.accountStatus] || u.accountStatus}
                    </span>
                  </td>
                  <td style={{ fontSize: "0.78rem", color: "#9ca3af" }}>
                    {u.lastLoginAt
                      ? new Date(u.lastLoginAt).toLocaleDateString("fa-IR")
                      : "—"}
                  </td>
                  <td>
                    <div className="su-actions">
                      {u.accountStatus !== "DELETED" && (
                        <button
                          className={`btn btn-sm ${
                            u.accountStatus === "ACTIVE"
                              ? "btn-outline su-btn-suspend"
                              : "btn-success"
                          }`}
                          onClick={() => handleToggleStatus(u)}
                          disabled={!!actionLoading[u.id]}
                          title={
                            u.accountStatus === "ACTIVE"
                              ? "تعلیق کاربر"
                              : "فعال‌سازی مجدد"
                          }
                        >
                          {actionLoading[u.id] === "status"
                            ? "..."
                            : u.accountStatus === "ACTIVE"
                            ? "تعلیق"
                            : "فعال‌سازی"}
                        </button>
                      )}
                      <button
                        className="btn btn-sm btn-outline su-btn-role"
                        onClick={() => handleToggleRole(u)}
                        disabled={!!actionLoading[u.id]}
                        title="تغییر نقش"
                      >
                        {actionLoading[u.id] === "role"
                          ? "..."
                          : u.systemRole === "SYSTEM_ADMIN"
                          ? "↓ کاربر"
                          : "↑ ادمین"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default SystemUsersPage;
