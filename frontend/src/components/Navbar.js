import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Navbar.css";

const Navbar = () => {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-brand">
          📚 Library Management
        </Link>
        <div className="navbar-menu">
          {isAuthenticated ? (
            <>
              <Link to="/libraries" className="nav-link">
                Libraries
              </Link>
              <Link to="/books" className="nav-link">
                Books
              </Link>
              <Link to="/borrows" className="nav-link">
                My Borrows
              </Link>
              {user?.systemRole === "SYSTEM_ADMIN" && (
                <Link to="/admin/users" className="nav-link">
                  Admin Users
                </Link>
              )}
              <Link to="/profile" className="nav-link">
                {user?.email || "Profile"}
              </Link>
              <button onClick={handleLogout} className="nav-link logout-btn">
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/libraries" className="nav-link">
                Libraries
              </Link>
              <Link to="/login" className="nav-link">
                Login
              </Link>
              <Link to="/register" className="nav-link">
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
