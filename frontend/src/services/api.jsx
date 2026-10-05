import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  // Same-origin API: the encrypted HttpOnly session cookie is sent automatically and the
  // CSRF token is echoed from the XSRF-TOKEN cookie (axios defaults). No credentials or
  // tokens are ever stored in the browser by this app.
  withCredentials: true,
});

/** Where the browser goes to log in through the identity provider (Keycloak). */
export const loginUrl = ({ register = false, reauth = false, action, returnTo } = {}) => {
  const params = new URLSearchParams();
  if (register) params.set("register", "true");
  if (reauth) params.set("reauth", "true");
  if (action) params.set("action", action);
  if (returnTo) params.set("returnTo", returnTo);
  const query = params.toString();
  return `${API_BASE_URL}/oauth2/authorization/keycloak${query ? `?${query}` : ""}`;
};

const currentPath = () => window.location.pathname + window.location.search;

// Prevent stale cached GET responses
api.interceptors.request.use((config) => {
  if ((config.method || "get").toLowerCase() === "get") {
    config.headers["Cache-Control"] = "no-cache";
    config.params = { ...(config.params || {}), _t: Date.now() };
  }
  return config;
});

/**
 * Session problems reported by the server, handled in one place:
 *   REAUTH_REQUIRED  → sensitive operation: confirm and send the user through Keycloak again
 *   NOTICE_REQUIRED  → the post-login security notice must be acknowledged first
 *   other 401        → session ended (expired, invalid or logged out elsewhere)
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const code = error.response?.data?.code;
    const isSessionProbe = error.config?.url?.includes("/v1/auth/");

    if (status === 401 && code === "REAUTH_REQUIRED") {
      if (window.confirm("این عملیات حساس است و باید دوباره هویت خود را تأیید کنید. ادامه می‌دهید؟")) {
        window.location.href = loginUrl({ reauth: true, returnTo: currentPath() });
      }
    } else if (status === 403 && code === "NOTICE_REQUIRED") {
      window.dispatchEvent(new Event("security-notice-required"));
    } else if (status === 401 && !isSessionProbe) {
      window.dispatchEvent(new CustomEvent("session-ended", { detail: code || "SESSION_EXPIRED" }));
    }
    return Promise.reject(error);
  }
);

// Session endpoints (login itself happens at Keycloak via loginUrl)
export const authAPI = {
  session: () => api.get("/v1/auth/session"),
  acknowledgeNotice: () => api.post("/v1/auth/notice"),
  logout: () => api.post("/v1/auth/logout"),
};

// Book endpoints (require libraryId)
export const bookAPI = {
  getBooks: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/books`, { params }),
  searchBooks: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/books/search`, { params }),
  // Subjects are now managed via /v1/libraries/{id}/subjects

  getBook: (libraryId, bookId) =>
    api.get(`/v1/libraries/${libraryId}/books/${bookId}`),
  createBook: (libraryId, bookData) =>
    api.post(`/v1/libraries/${libraryId}/books`, bookData),
  updateBook: (libraryId, bookId, bookData) =>
    api.put(`/v1/libraries/${libraryId}/books/${bookId}`, bookData),
  // Consolidated create/update: metadata + cover + digital PDF + copy count in ONE request.
  // bookId omitted -> POST (create); bookId given -> PUT (full) or PATCH (partial, only
  // supplied fields/assets change) depending on `partial`. Any of cover/digitalFile/copyCount
  // may be omitted (undefined/null).
  saveBook: (libraryId, { bookId, book, cover, digitalFile, digitalVersionName, copyCount, partial, onProgress } = {}) => {
    const formData = new FormData();
    if (book) formData.append("book", new Blob([JSON.stringify(book)], { type: "application/json" }));
    if (cover) formData.append("cover", cover);
    if (digitalFile) formData.append("digital", digitalFile);
    if (digitalVersionName) formData.append("digitalVersionName", digitalVersionName);
    if (copyCount != null) formData.append("copyCount", copyCount);

    const config = {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total));
      },
    };
    const url = bookId
      ? `/v1/libraries/${libraryId}/books/${bookId}`
      : `/v1/libraries/${libraryId}/books`;
    if (!bookId) return api.post(url, formData, config);
    return partial ? api.patch(url, formData, config) : api.put(url, formData, config);
  },
  deleteBook: (libraryId, bookId) =>
    api.delete(`/v1/libraries/${libraryId}/books/${bookId}`),
  getDeletedBooks: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/books/deleted`, { params }),
  restoreBook: (libraryId, bookId) =>
    api.post(`/v1/libraries/${libraryId}/books/${bookId}/restore`),
  addCopies: (libraryId, bookId, numberOfCopies) =>
    api.post(`/v1/libraries/${libraryId}/books/${bookId}/copies`, {
      numberOfCopies,
    }),
  setCopyCount: (libraryId, bookId, count) =>
    api.put(`/v1/libraries/${libraryId}/books/${bookId}/copies/count`, null, { params: { count } }),
  uploadCoverImage: (libraryId, bookId, file, onProgress) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post(`/v1/libraries/${libraryId}/books/${bookId}/cover`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total));
        }
      },
    });
  },
  // Digital book endpoints
  listDigitalBooks: (libraryId, bookId) =>
    api.get(`/v1/libraries/${libraryId}/books/${bookId}/digital`),
  uploadDigitalBook: (libraryId, bookId, file, versionName, onProgress) => {
    const formData = new FormData();
    formData.append("file", file);
    if (versionName) formData.append("versionName", versionName);
    return api.post(`/v1/libraries/${libraryId}/books/${bookId}/digital`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total));
        }
      },
    });
  },
  deleteDigitalBook: (libraryId, bookId, digitalBookId) =>
    api.delete(`/v1/libraries/${libraryId}/books/${bookId}/digital/${digitalBookId}`),
  downloadDigitalBook: (libraryId, bookId, digitalBookId) =>
    api.get(`/v1/libraries/${libraryId}/books/${bookId}/digital/${digitalBookId}/download`, {
      responseType: "blob",
    }),
};

// Borrow endpoints (require libraryId)
export const borrowAPI = {
  getBorrows: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/borrows`, { params }),
  getPendingBorrows: (libraryId, type) =>
    api.get(`/v1/libraries/${libraryId}/borrows/pending`, {
      params: type ? { type } : undefined,
    }),
  createBorrow: (libraryId, borrowData) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowData.bookId}`, {
      borrowType: borrowData.borrowType,
      bookCopyId: borrowData.bookCopyId,
      deliveryAddress: borrowData.deliveryAddress,
      deliveryExtension: borrowData.deliveryExtension,
      requestedDurationDays: borrowData.requestedDurationDays,
      saveToProfile: borrowData.saveToProfile,
    }),
  updateRequest: (libraryId, borrowId, data) =>
    api.put(`/v1/libraries/${libraryId}/borrows/${borrowId}/request`, {
      borrowType: "PHYSICAL",
      ...data,
    }),
  approveBorrow: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/approve`),
  approvePhysical: (libraryId, borrowId, data) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/approve-physical`, data),
  updateDelivery: (libraryId, borrowId, data) =>
    api.patch(`/v1/libraries/${libraryId}/borrows/${borrowId}/delivery`, data),
  confirmReceipt: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/confirm-receipt`),
  confirmReturn: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/confirm-return`),
  confirmHandover: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/confirm-handover`),
  cancelReturnRequest: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/cancel-return-request`),
  searchLibraryBorrows: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/borrows/admin/search`, { params }),
  cancelByUser: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/cancel`),
  cancelByLibrarian: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/cancel-admin`),
  requestReturn: (libraryId, borrowId, data) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/request-return`, data),
  scheduleReturnPickup: (libraryId, borrowId, data) =>
    api.patch(`/v1/libraries/${libraryId}/borrows/${borrowId}/return-schedule`, data),
  getBorrowerSummary: (libraryId, userId) =>
    api.get(`/v1/libraries/${libraryId}/borrows/user/${userId}/summary`),
  getBorrowEvents: (libraryId, borrowId) =>
    api.get(`/v1/libraries/${libraryId}/borrows/${borrowId}/events`),
  rejectBorrow: (libraryId, borrowId, reason) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/reject`, null, {
      params: reason ? { reason } : undefined,
    }),
  returnBorrow: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/return`),
  reserveBook: (libraryId, bookId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${bookId}/reserve`),
};

// Library creation requests
export const libraryRequestAPI = {
  submit: (data) => api.post("/v1/library-requests", data),
  mine: () => api.get("/v1/library-requests/mine"),
  all: (status) => api.get("/v1/library-requests", { params: status ? { status } : undefined }),
  approve: (id, override) => api.post(`/v1/library-requests/${id}/approve`, override || {}),
  reject: (id, reason) =>
    api.post(`/v1/library-requests/${id}/reject`, null, { params: reason ? { reason } : undefined }),
};

// Public cross-library book search
export const searchAPI = {
  global: (query) => api.get("/v1/books/search", { params: { query, size: 24 } }),
};

// Current-user cross-library views (borrows, downloads, favorites)
export const meAPI = {
  borrows: (params) => api.get("/v1/me/borrows", { params }),
  toggleFavorite: (bookId) => api.post(`/v1/me/favorites/${bookId}`),
  favoriteIds: () => api.get("/v1/me/favorites/ids"),
  favorites: (params) => api.get("/v1/me/favorites", { params }),
};

// Notification endpoints
export const notificationAPI = {
  list: () => api.get("/v1/notifications"),
  listPaged: (params) => api.get("/v1/notifications/paged", { params }),
  // Polled in the background: must not count as user activity (session idle timeout)
  unreadCount: () =>
    api.get("/v1/notifications/unread-count", { headers: { "X-Background-Request": "true" } }),
  markRead: (id) => api.post(`/v1/notifications/${id}/read`),
  markAllRead: () => api.post("/v1/notifications/read-all"),
};

// Library endpoints
export const libraryAPI = {
  getLibraries: () => api.get("/v1/libraries"),
  getPublicLibraries: () => api.get("/v1/libraries/public/active"),
  getLibrary: (libraryId) => api.get(`/v1/libraries/${libraryId}`),
  createLibrary: (libraryData) => api.post("/v1/libraries", libraryData),
  updateLibrary: (libraryId, libraryData) =>
    api.put(`/v1/libraries/${libraryId}`, libraryData),
  deleteLibrary: (libraryId) => api.delete(`/v1/libraries/${libraryId}`),
  requestMembership: (libraryId) =>
    api.post(`/v1/libraries/${libraryId}/membership/request`),
  approveMembership: (libraryId, userId) =>
    api.post(`/v1/libraries/${libraryId}/membership/${userId}/approve`),
  rejectMembership: (libraryId, userId) =>
    api.post(`/v1/libraries/${libraryId}/membership/${userId}/reject`),
};

// Admin endpoints
export const adminAPI = {
  // params: { status?, search?, page?, size? } → Page<UserDTO>
  getUsers: (params) => api.get("/v1/admin/users", { params }),
  updateUserStatus: (userId, status) =>
    api.patch(`/v1/admin/users/${userId}/status`, { status }),
  updateUserRole: (userId, role) =>
    api.patch(`/v1/admin/users/${userId}/role`, { role }),
  updateUserClearance: (userId, clearance) =>
    api.patch(`/v1/admin/users/${userId}/clearance`, { clearance }),
  assignTemporaryPassword: (userId, password) =>
    api.post(`/v1/admin/users/${userId}/temporary-password`, { password }),
  // params: { search?, page?, size? } → Page<LibraryDTO>
  getLibraries: (params) => api.get("/v1/admin/libraries", { params }),
};

// Output label printed on every page/report (classification, user, IP, time)
export const outputLabelAPI = {
  current: () => api.get("/v1/output-label"),
};

// Security settings (view: system admin, change: super admin)
export const securitySettingsAPI = {
  get: () => api.get("/v1/admin/security-settings"),
  update: (settings) => api.put("/v1/admin/security-settings", settings),
  sensitiveOperations: () => api.get("/v1/admin/security-settings/sensitive-operations"),
};

// Security audit trail (system admin)
export const auditAPI = {
  // params: { action?, outcome?, search?, from?, to?, page?, size? } → Page<AuditLogDTO>
  search: (params) => api.get("/v1/admin/audit-logs", { params }),
  actions: () => api.get("/v1/admin/audit-logs/actions"),
  exportCsv: (params) =>
    api.get("/v1/admin/audit-logs/export", { params, responseType: "blob" }),
  verify: () => api.post("/v1/admin/audit-logs/verify"),
};

// Library stats endpoints (admin)
export const libraryStatsAPI = {
  getMostBorrowed: (libraryId, limit = 10) =>
    api.get(`/v1/libraries/${libraryId}/stats/most-borrowed`, { params: { limit } }),
  getUnderused: (libraryId) =>
    api.get(`/v1/libraries/${libraryId}/stats/underused`),
  getUserActivity: (libraryId, limit = 10) =>
    api.get(`/v1/libraries/${libraryId}/stats/user-activity`, { params: { limit } }),
};

// Library admin endpoints
export const libraryAdminAPI = {
  getMembers: (libraryId) =>
    api.get(`/v1/libraries/${libraryId}/members`),
  getPendingMembers: (libraryId) =>
    api.get(`/v1/libraries/${libraryId}/members/pending`),
  searchMembers: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/members/search`, { params }),
  approveMembership: (libraryId, userId) =>
    api.post(`/v1/libraries/${libraryId}/membership/${userId}/approve`),
  rejectMembership: (libraryId, userId, reason) =>
    api.post(`/v1/libraries/${libraryId}/membership/${userId}/reject`, null, {
      params: reason ? { reason } : undefined,
    }),
  getAllBorrows: (libraryId, status, type) =>
    api.get(`/v1/libraries/${libraryId}/borrows/admin/all`, {
      params: { ...(status ? { status } : {}), ...(type ? { type } : {}) },
    }),
  setMemberRole: (libraryId, userId, role) =>
    api.patch(`/v1/libraries/${libraryId}/members/${userId}/role`, null, { params: { role } }),
};

// User profile endpoints
export const userAPI = {
  getProfile: () => api.get("/v1/users/me"),
  updateProfile: (data) => api.put("/v1/users/me", data),
};

// Subject endpoints (per library)
export const subjectAPI = {
  getSubjects: (libraryId) => api.get(`/v1/libraries/${libraryId}/subjects`),
  createSubject: (libraryId, name) => api.post(`/v1/libraries/${libraryId}/subjects`, { name }),
  deleteSubject: (libraryId, subjectId) => api.delete(`/v1/libraries/${libraryId}/subjects/${subjectId}`),
};

// Public stats endpoint
export const statsAPI = {
  getStats: () => api.get("/v1/stats"),
};

export default api;
