import React, { useState } from "react";
import { NavLink, Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import NotificationBell from "../components/NotificationBell";
import "./AdminLayout.css";

const AdminLayout = ({ title, subtitle, navItems, backTo = "/" }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <div className="admin-shell">
      {/* ── Sidebar ── */}
      <aside className={`admin-sidebar ${sidebarOpen ? "sidebar-open" : ""}`}>
        <div className="sidebar-header">
          <span className="sidebar-logo-icon">📚</span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="sidebar-title">{title}</div>
            {subtitle && <div className="sidebar-subtitle">{subtitle}</div>}
          </div>
          <NotificationBell align="end" />
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `sidebar-nav-item ${isActive ? "sidebar-nav-item--active" : ""}`
              }
              onClick={() => setSidebarOpen(false)}
            >
              <span className="sidebar-nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <span className="sidebar-user-avatar">
              {user?.email?.charAt(0).toUpperCase()}
            </span>
            <span className="sidebar-user-email">{user?.email}</span>
          </div>
          <Link to={backTo} className="sidebar-back-link">
            <span>←</span>
            <span>بازگشت به سایت</span>
          </Link>
          <button onClick={handleLogout} className="sidebar-logout-btn">
            خروج از حساب
          </button>
        </div>
      </aside>

      {/* ── Overlay (mobile) ── */}
      {sidebarOpen && (
        <div
          className="sidebar-overlay"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* ── Main ── */}
      <div className="admin-main">
        {/* Mobile topbar */}
        <div className="admin-topbar">
          <button
            className="topbar-hamburger"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            aria-label="منو"
          >
            <span />
            <span />
            <span />
          </button>
          <span className="topbar-title">{title}</span>
          {subtitle && (
            <span className="topbar-library">{subtitle}</span>
          )}
          <div style={{ marginInlineStart: "auto" }}>
            <NotificationBell />
          </div>
        </div>

        <div className="admin-content">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default AdminLayout;
