import React, { useEffect, useState } from "react";
import { adminAPI } from "../services/api";
import "./AdminUsersPage.css";

const ROLE_LABELS = {
  SYSTEM_ADMIN: "مدیر سیستم",
  USER: "کاربر عادی",
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

const AdminUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => { fetchUsers(); }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getUsers("ACTIVE");
      setUsers(response.data.data || response.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "خطا در بارگذاری کاربران");
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری کاربران...</div>;

  return (
    <div className="admin-page">
      {/* Header */}
      <div className="admin-header">
        <div className="admin-header-inner">
          <div>
            <h1 className="admin-main-title">مدیریت کاربران</h1>
            <p className="admin-main-sub">مشاهده و مدیریت کاربران فعال سیستم</p>
          </div>
          <div className="admin-count-badge">
            {users.length} کاربر فعال
          </div>
        </div>
      </div>

      <div className="admin-body">
        {error && <div className="error-message">{error}</div>}

        {users.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">👥</span>
            <p>هیچ کاربر فعالی یافت نشد</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="modern-table">
              <thead>
                <tr>
                  <th>شناسه</th>
                  <th>ایمیل</th>
                  <th>نام</th>
                  <th>تلفن</th>
                  <th>نقش</th>
                  <th>وضعیت</th>
                  <th>آخرین ورود</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td className="user-id">{user.id}</td>
                    <td className="user-email">{user.email}</td>
                    <td>
                      {user.firstName || user.lastName
                        ? `${user.firstName || ""} ${user.lastName || ""}`.trim()
                        : "—"}
                    </td>
                    <td>{user.phoneNumber || "—"}</td>
                    <td>
                      <span className="badge badge-info">
                        {ROLE_LABELS[user.systemRole] || user.systemRole}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${STATUS_CLASS[user.accountStatus] || "badge-muted"}`}>
                        {STATUS_LABELS[user.accountStatus] || user.accountStatus}
                      </span>
                    </td>
                    <td className="user-date">
                      {user.lastLoginAt
                        ? new Date(user.lastLoginAt).toLocaleString("fa-IR")
                        : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminUsersPage;
