import React, { useEffect, useState, useCallback } from "react";
import { adminAPI } from "../../services/api";
import { toPersian, toPersianNum } from "../../utils/persian";
import { useDebounce } from "../../hooks/useDebounce";
import Pagination from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";
import {
  CLASSIFICATION_LEVELS,
  classificationBadge,
  classificationLabel,
} from "../../utils/classification";
import "./SystemUsersPage.css";

const ROLE_LABELS = {
  SUPER_ADMIN: "ادمین اصلی",
  SYSTEM_ADMIN: "مدیر سیستم",
  USER: "کاربر عادی",
};
const ROLE_CLASS = {
  SUPER_ADMIN: "badge-danger",
  SYSTEM_ADMIN: "badge-warning",
  USER: "badge-info",
};
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

const userInitial = (u) => (u.firstName?.trim()?.[0] || u.email?.[0] || "؟").toUpperCase();

const STATUS_FILTERS = [
  { value: "", label: "همه" },
  { value: "ACTIVE", label: "فعال" },
  { value: "SUSPENDED", label: "معلق" },
];

const SystemUsersPage = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [statusFilter, setStatusFilter] = useState("ACTIVE");
  const [actionLoading, setActionLoading] = useState({});
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const debouncedSearch = useDebounce(search, 400);

  const isSuperAdmin = currentUser?.systemRole === "SUPER_ADMIN";

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(""), 3500);
  };

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const res = await adminAPI.getUsers({
        status: statusFilter || undefined,
        search: debouncedSearch || undefined,
        page,
        size: 12,
      });
      const d = res.data?.data || res.data;
      setUsers(d?.content || []);
      setTotalPages(d?.totalPages ?? 0);
      setError("");
    } catch (err) {
      setError("خطا در بارگذاری کاربران");
    } finally {
      setLoading(false);
    }
  }, [statusFilter, debouncedSearch, page]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);
  useEffect(() => {
    setPage(0);
  }, [statusFilter, debouncedSearch]);

  const handleToggleStatus = async (user) => {
    const newStatus = user.accountStatus === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
    try {
      setActionLoading((p) => ({ ...p, [user.id]: "status" }));
      await adminAPI.updateUserStatus(user.id, newStatus);
      showSuccess(
        `وضعیت ${user.email} به «${STATUS_LABELS[newStatus]}» تغییر کرد`,
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
    const label = newRole === "SYSTEM_ADMIN" ? "مدیر سیستم" : "کاربر عادی";
    if (
      !window.confirm(`آیا نقش ${user.email} را به «${label}» تغییر می‌دهید؟`)
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

  const handleClearanceChange = async (user, clearance) => {
    const label = classificationLabel(clearance);
    if (!window.confirm(`سطح دسترسی ${user.email} به «${label}» تغییر کند؟`)) return;
    try {
      setActionLoading((p) => ({ ...p, [user.id]: "clearance" }));
      await adminAPI.updateUserClearance(user.id, clearance);
      showSuccess(`سطح دسترسی ${user.email} به «${label}» تغییر کرد`);
      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در تغییر سطح دسترسی");
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
        <span className="su-count">{toPersianNum(users.length)} کاربر</span>
      </div>

      <div className="abr-toolbar">
        <input
          className="abr-search"
          placeholder="🔍 جستجوی کاربر بر اساس نام، ایمیل یا تلفن..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
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
                <th>کاربر</th>
                <th>تلفن</th>
                <th>نقش</th>
                <th>وضعیت</th>
                <th>سطح دسترسی</th>
                <th>آخرین ورود</th>
                <th>عملیات</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td className="su-id">{toPersian(u.id)}</td>
                  <td>
                    <div className="su-user">
                      <span className="su-avatar">{userInitial(u)}</span>
                      <div>
                        <div className="su-user-name">
                          {u.firstName || u.lastName
                            ? `${u.firstName || ""} ${u.lastName || ""}`.trim()
                            : "—"}
                        </div>
                        <div className="su-user-email">{u.email}</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ fontSize: "0.82rem" }}>
                    {u.phoneNumber || "—"}
                  </td>
                  <td>
                    <span
                      className={`badge ${ROLE_CLASS[u.systemRole] || "badge-info"}`}
                    >
                      {ROLE_LABELS[u.systemRole] || u.systemRole}
                    </span>
                  </td>
                  <td>
                    <span
                      className={`badge ${STATUS_CLASS[u.accountStatus] || "badge-muted"}`}
                    >
                      {STATUS_LABELS[u.accountStatus] || u.accountStatus}
                    </span>
                  </td>
                  <td>
                    {isSuperAdmin && u.id !== currentUser?.id ? (
                      <select
                        className="su-clearance-select"
                        value={u.clearance || "UNCLASSIFIED"}
                        onChange={(e) => handleClearanceChange(u, e.target.value)}
                        disabled={!!actionLoading[u.id]}
                        title="سطح دسترسی به اطلاعات طبقه‌بندی‌شده"
                      >
                        {CLASSIFICATION_LEVELS.map((l) => (
                          <option key={l.value} value={l.value}>{l.label}</option>
                        ))}
                      </select>
                    ) : (
                      <span className={`badge ${classificationBadge(u.clearance)}`}>
                        {classificationLabel(u.clearance)}
                      </span>
                    )}
                  </td>
                  <td className="su-date">
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
                      {isSuperAdmin && u.systemRole !== "SUPER_ADMIN" && (
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
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={page} totalPages={totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  );
};

export default SystemUsersPage;
