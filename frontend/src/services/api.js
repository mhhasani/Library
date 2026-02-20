import axios from "axios";

const API_BASE_URL =
  process.env.REACT_APP_API_URL || "http://localhost:8080/api";

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
  refresh: () => api.post("/v1/auth/refresh"),
};

// Book endpoints (require libraryId)
export const bookAPI = {
  getBooks: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/books`, { params }),
  searchBooks: (libraryId, params) =>
    api.get(`/v1/libraries/${libraryId}/books/search`, { params }),
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
      digitalBookId: borrowData.digitalBookId,
    }),
  approveBorrow: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/approve`),
  rejectBorrow: (libraryId, borrowId, reason) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/reject`, {
      rejectionReason: reason,
    }),
  returnBorrow: (libraryId, borrowId) =>
    api.post(`/v1/libraries/${libraryId}/borrows/${borrowId}/return`),
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
};

export default api;
