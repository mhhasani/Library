import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8080/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Token refresh logic
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

const clearAuthAndRedirect = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
  window.location.href = "/login";
};

// Auto-refresh on 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url.includes("/v1/auth/")
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) {
        isRefreshing = false;
        clearAuthAndRedirect();
        return Promise.reject(error);
      }

      try {
        const res = await axios.post(
          `${API_BASE_URL}/v1/auth/refresh`,
          {},
          { headers: { Authorization: `Bearer ${refreshToken}` } }
        );
        const data = res.data?.data || res.data;
        const newAccessToken = data.accessToken;
        localStorage.setItem("token", newAccessToken);
        if (data.refreshToken) {
          localStorage.setItem("refreshToken", data.refreshToken);
        }
        processQueue(null, newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        clearAuthAndRedirect();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// Auth endpoints
export const authAPI = {
  register: (userData) =>
    api.post("/v1/auth/register", {
      email: userData.email,
      password: userData.password,
      firstName: userData.firstName,
      lastName: userData.lastName,
      phoneNumber: userData.phoneNumber,
    }),
  login: (credentials) =>
    api.post("/v1/auth/login", {
      email: credentials.email,
      password: credentials.password,
    }),
  refresh: () => {
    const refreshToken = localStorage.getItem("refreshToken");
    return axios.post(
      `${API_BASE_URL}/v1/auth/refresh`,
      {},
      { headers: { Authorization: `Bearer ${refreshToken}` } }
    );
  },
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
  deleteBook: (libraryId, bookId) =>
    api.delete(`/v1/libraries/${libraryId}/books/${bookId}`),
  addCopies: (libraryId, bookId, numberOfCopies) =>
    api.post(`/v1/libraries/${libraryId}/books/${bookId}/copies`, {
      numberOfCopies,
    }),
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
  getPendingBorrows: (libraryId) =>
    api.get(`/v1/libraries/${libraryId}/borrows/pending`),
  createBorrow: (libraryId, borrowData) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowData.bookId}`, {
      borrowType: borrowData.borrowType,
      bookCopyId: borrowData.bookCopyId,
    }),
  approveBorrow: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/approve`),
  rejectBorrow: (libraryId, borrowId, reason) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/reject`, null, {
      params: reason ? { reason } : undefined,
    }),
  returnBorrow: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/return`),
  reserveBook: (libraryId, bookId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${bookId}/reserve`),
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
  getUsers: (status) =>
    api.get("/v1/admin/users", {
      params: status ? { status } : undefined,
    }),
  updateUserStatus: (userId, status) =>
    api.patch(`/v1/admin/users/${userId}/status`, { status }),
  updateUserRole: (userId, role) =>
    api.patch(`/v1/admin/users/${userId}/role`, { role }),
  getLibraries: () => api.get("/v1/admin/libraries"),
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
  approveMembership: (libraryId, userId) =>
    api.post(`/v1/libraries/${libraryId}/membership/${userId}/approve`),
  rejectMembership: (libraryId, userId, reason) =>
    api.post(`/v1/libraries/${libraryId}/membership/${userId}/reject`, null, {
      params: reason ? { reason } : undefined,
    }),
  getAllBorrows: (libraryId, status) =>
    api.get(`/v1/libraries/${libraryId}/borrows/admin/all`, {
      params: status ? { status } : undefined,
    }),
};

// User profile endpoints
export const userAPI = {
  getProfile: () => api.get("/v1/users/me"),
  updateProfile: (data) => api.put("/v1/users/me", data),
  changePassword: (data) => api.put("/v1/users/me/password", data),
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
