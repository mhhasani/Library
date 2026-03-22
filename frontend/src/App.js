import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

// Route guards
import PrivateRoute from "./components/PrivateRoute";
import AdminRoute from "./components/AdminRoute";
import LibraryAdminRoute from "./components/LibraryAdminRoute";

// Layouts
import UserLayout from "./layouts/UserLayout";
import AdminLayout from "./layouts/AdminLayout";

// User pages
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import LibrariesPage from "./pages/LibrariesPage";
import BooksPage from "./pages/BooksPage";
import BorrowsPage from "./pages/BorrowsPage";
import ProfilePage from "./pages/ProfilePage";

// Library admin pages
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminBooksPage from "./pages/admin/AdminBooksPage";
import AdminMembersPage from "./pages/admin/AdminMembersPage";
import AdminBorrowsPage from "./pages/admin/AdminBorrowsPage";

// System admin pages
import SystemDashboard from "./pages/system/SystemDashboard";
import SystemUsersPage from "./pages/system/SystemUsersPage";
import SystemLibrariesPage from "./pages/system/SystemLibrariesPage";

import "./App.css";

const LIBRARY_ADMIN_NAV = [
  { to: "/admin",         end: true,  icon: "📊", label: "داشبورد" },
  { to: "/admin/books",   end: false, icon: "📖", label: "مدیریت کتاب‌ها" },
  { to: "/admin/members", end: false, icon: "👥", label: "اعضا و درخواست‌ها" },
  { to: "/admin/borrows", end: false, icon: "📋", label: "امانت‌ها" },
];

const SYSTEM_ADMIN_NAV = [
  { to: "/system",            end: true,  icon: "📊", label: "داشبورد سیستم" },
  { to: "/system/users",      end: false, icon: "👤", label: "مدیریت کاربران" },
  { to: "/system/libraries",  end: false, icon: "🏛️", label: "کتابخانه‌ها" },
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
              path="/books"
              element={<PrivateRoute><BooksPage /></PrivateRoute>}
            />
            <Route
              path="/borrows"
              element={<PrivateRoute><BorrowsPage /></PrivateRoute>}
            />
            <Route
              path="/profile"
              element={<PrivateRoute><ProfilePage /></PrivateRoute>}
            />
          </Route>

          {/* ── Library admin panel ── */}
          <Route
            path="/admin"
            element={
              <LibraryAdminRoute>
                <AdminLayout
                  title="پنل کتابدار"
                  navItems={LIBRARY_ADMIN_NAV}
                  backTo="/libraries"
                />
              </LibraryAdminRoute>
            }
          >
            <Route index element={<AdminDashboard />} />
            <Route path="books" element={<AdminBooksPage />} />
            <Route path="members" element={<AdminMembersPage />} />
            <Route path="borrows" element={<AdminBorrowsPage />} />
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
          </Route>
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
