import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Navbar.css";

const Navbar = () => {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  const activeLibraryRole = localStorage.getItem("activeLibraryRole");
  const activeLibraryStatus = localStorage.getItem("activeLibraryStatus");
  const isLibraryAdmin =
    isAuthenticated &&
    activeLibraryRole === "ADMIN" &&
    activeLibraryStatus === "APPROVED";

  const handleLogout = () => {
    logout();
    navigate("/");
    setMenuOpen(false);
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-logo">
          <span className="logo-icon">📚</span>
          <span className="logo-text">سامانه کتابخانه</span>
        </Link>

        <button
          className="hamburger"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="منو"
        >
          <span />
          <span />
          <span />
        </button>

        <div className={`navbar-links ${menuOpen ? "open" : ""}`}>
          <Link
            to="/libraries"
            className={`nav-link ${isActive("/libraries") ? "active" : ""}`}
            onClick={() => setMenuOpen(false)}
          >
            کتابخانه‌ها
          </Link>

          {isAuthenticated && (
            <>
              <Link
                to="/books"
                className={`nav-link ${isActive("/books") ? "active" : ""}`}
                onClick={() => setMenuOpen(false)}
              >
                کتاب‌ها
              </Link>
              <Link
                to="/borrows"
                className={`nav-link ${isActive("/borrows") ? "active" : ""}`}
                onClick={() => setMenuOpen(false)}
              >
                امانت‌های من
              </Link>
              {isLibraryAdmin && (
                <Link
                  to="/admin"
                  className={`nav-link nav-link--admin ${location.pathname.startsWith("/admin") ? "active" : ""}`}
                  onClick={() => setMenuOpen(false)}
                >
                  🗂 پنل کتابدار
                </Link>
              )}
              {user?.systemRole === "SYSTEM_ADMIN" && (
                <Link
                  to="/system"
                  className={`nav-link nav-link--system ${location.pathname.startsWith("/system") ? "active" : ""}`}
                  onClick={() => setMenuOpen(false)}
                >
                  ⚙️ پنل سیستم
                </Link>
              )}
            </>
          )}

          <div className="navbar-actions">
            {isAuthenticated ? (
              <>
                <Link
                  to="/profile"
                  className="nav-user-chip"
                  onClick={() => setMenuOpen(false)}
                >
                  <span className="user-avatar">
                    {user?.email?.charAt(0).toUpperCase()}
                  </span>
                  <span className="user-email">{user?.email}</span>
                </Link>
                <button onClick={handleLogout} className="btn-logout">
                  خروج
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="nav-btn-outline"
                  onClick={() => setMenuOpen(false)}
                >
                  ورود
                </Link>
                <Link
                  to="/register"
                  className="nav-btn-fill"
                  onClick={() => setMenuOpen(false)}
                >
                  ثبت‌نام
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
