import React, { useEffect, useState } from "react";
import { adminAPI } from "../services/api";
import "./AdminUsersPage.css";

const AdminUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getUsers("ACTIVE");
      setUsers(response.data.data || response.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading users...</div>;

  return (
    <div className="admin-users-container">
      <h2>Active Users</h2>
      {error && <div className="error-message">{error}</div>}

      {users.length === 0 ? (
        <p className="no-users">No active users found.</p>
      ) : (
        <div className="users-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Email</th>
                <th>Name</th>
                <th>Phone</th>
                <th>Role</th>
                <th>Status</th>
                <th>Last Login</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.email}</td>
                  <td>
                    {user.firstName} {user.lastName}
                  </td>
                  <td>{user.phoneNumber || "—"}</td>
                  <td>{user.systemRole}</td>
                  <td>{user.accountStatus}</td>
                  <td>
                    {user.lastLoginAt
                      ? new Date(user.lastLoginAt).toLocaleString()
                      : "—"}
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

export default AdminUsersPage;
