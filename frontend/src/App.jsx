import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

// Route guards
import PrivateRoute from "./components/PrivateRoute";
import AdminRoute from "./components/AdminRoute";

// Layouts
import UserLayout from "./layouts/UserLayout";
import AdminLayout from "./layouts/AdminLayout";
import LibraryLayout from "./layouts/LibraryLayout";
import LibraryAdminLayout from "./layouts/LibraryAdminLayout";

// User pages
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import LibrariesPage from "./pages/LibrariesPage";
import BooksPage from "./pages/BooksPage";
import BookProfilePage from "./pages/BookProfilePage";
import PhysicalBorrowsPage from "./pages/PhysicalBorrowsPage";
import DigitalBorrowsPage from "./pages/DigitalBorrowsPage";
import ProfilePage from "./pages/ProfilePage";

// Library admin pages
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminBooksPage from "./pages/admin/AdminBooksPage";
import AdminMembersPage from "./pages/admin/AdminMembersPage";
import AdminPhysicalBorrowsPage from "./pages/admin/AdminPhysicalBorrowsPage";
import AdminDigitalBorrowsPage from "./pages/admin/AdminDigitalBorrowsPage";

// System admin pages
import SystemDashboard from "./pages/system/SystemDashboard";
import SystemUsersPage from "./pages/system/SystemUsersPage";
import SystemLibrariesPage from "./pages/system/SystemLibrariesPage";
import SystemLibraryRequestsPage from "./pages/system/SystemLibraryRequestsPage";
import SystemAuditLogsPage from "./pages/system/SystemAuditLogsPage";

import "./App.css";

const SYSTEM_ADMIN_NAV = [
  { to: "/system",           end: true,  icon: "📊", label: "داشبورد سیستم" },
  { to: "/system/users",     end: false, icon: "👤", label: "مدیریت کاربران" },
  { to: "/system/libraries", end: false, icon: "🏛️", label: "کتابخانه‌ها" },
  { to: "/system/library-requests", end: false, icon: "📨", label: "درخواست‌های کتابخانه" },
  { to: "/system/audit-logs", end: false, icon: "📜", label: "رویدادنگاری امنیتی" },
];

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* ── User routes (with Navbar) ── */}
          <Route element={<UserLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/libraries" element={<LibrariesPage />} />
            <Route
              path="/profile"
              element={<PrivateRoute><ProfilePage /></PrivateRoute>}
            />

            {/* Library-specific user pages */}
            <Route path="/libraries/:libraryId" element={<LibraryLayout />}>
              <Route
                path="books"
                element={<PrivateRoute><BooksPage /></PrivateRoute>}
              />
              <Route
                path="books/:bookId"
                element={<PrivateRoute><BookProfilePage /></PrivateRoute>}
              />
              <Route path="borrows" element={<Navigate to="physical" replace />} />
              <Route
                path="borrows/physical"
                element={<PrivateRoute><PhysicalBorrowsPage /></PrivateRoute>}
              />
              <Route
                path="borrows/digital"
                element={<PrivateRoute><DigitalBorrowsPage /></PrivateRoute>}
              />
            </Route>
          </Route>

          {/* ── Library admin panel ── */}
          <Route
            path="/libraries/:libraryId/admin"
            element={<LibraryAdminLayout />}
          >
            <Route index element={<AdminDashboard />} />
            <Route path="books" element={<AdminBooksPage />} />
            <Route path="members" element={<AdminMembersPage />} />
            <Route path="borrows" element={<Navigate to="physical" replace />} />
            <Route path="borrows/physical" element={<AdminPhysicalBorrowsPage />} />
            <Route path="borrows/digital" element={<AdminDigitalBorrowsPage />} />
          </Route>

          {/* ── System admin panel ── */}
          <Route
            path="/system"
            element={
              <AdminRoute>
                <AdminLayout
                  title="پنل مدیریت سیستم"
                  navItems={SYSTEM_ADMIN_NAV}
                  backTo="/"
                />
              </AdminRoute>
            }
          >
            <Route index element={<SystemDashboard />} />
            <Route path="users" element={<SystemUsersPage />} />
            <Route path="libraries" element={<SystemLibrariesPage />} />
            <Route path="library-requests" element={<SystemLibraryRequestsPage />} />
            <Route path="audit-logs" element={<SystemAuditLogsPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
