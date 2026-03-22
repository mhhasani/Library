import React, { useEffect, useState, useCallback } from "react";
import { useParams } from "react-router-dom";
import { bookAPI } from "../../services/api";
import "./AdminBooksPage.css";

const EMPTY_FORM = {
  title: "",
  author: "",
  publisher: "",
  publicationYear: "",
  description: "",
  coverImageUrl: "",
  autoDigitalBorrowEnabled: false,
};

const AdminBooksPage = () => {
  const { libraryId } = useParams();

  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Search
  const [searchQuery, setSearchQuery] = useState("");

  // Modal: add/edit
  const [showModal, setShowModal] = useState(false);
  const [editingBook, setEditingBook] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Copy management
  const [copyTarget, setCopyTarget] = useState(null);
  const [copyCount, setCopyCount] = useState(1);
  const [addingCopies, setAddingCopies] = useState(false);

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(""), 3500);
  };

  const fetchBooks = useCallback(async () => {
    if (!libraryId) return;
    try {
      setLoading(true);
      let res;
      if (searchQuery.trim()) {
        res = await bookAPI.searchBooks(libraryId, { query: searchQuery.trim(), page: 0, size: 100 });
      } else {
        res = await bookAPI.getBooks(libraryId, { page: 0, size: 100 });
      }
      const data = res.data?.data;
      setBooks(data?.content ?? (Array.isArray(data) ? data : []));
      setError("");
    } catch (err) {
      setError("خطا در بارگذاری کتاب‌ها");
    } finally {
      setLoading(false);
    }
  }, [libraryId, searchQuery]);

  useEffect(() => {
    const timer = setTimeout(fetchBooks, searchQuery ? 400 : 0);
    return () => clearTimeout(timer);
  }, [fetchBooks, searchQuery]);

  const openAddModal = () => {
    setEditingBook(null);
    setForm(EMPTY_FORM);
    setShowModal(true);
  };

  const openEditModal = (book) => {
    setEditingBook(book);
    setForm({
      title: book.title || "",
      author: book.author || "",
      publisher: book.publisher || "",
      publicationYear: book.publicationYear || "",
      description: book.description || "",
      coverImageUrl: book.coverImageUrl || "",
      autoDigitalBorrowEnabled: book.autoDigitalBorrowEnabled || false,
    });
    setShowModal(true);
  };

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!libraryId) return;
    try {
      setSaving(true);
      const payload = {
        ...form,
        publicationYear: form.publicationYear ? Number(form.publicationYear) : undefined,
        publisher: form.publisher || undefined,
        description: form.description || undefined,
        coverImageUrl: form.coverImageUrl || undefined,
      };
      if (editingBook) {
        await bookAPI.updateBook(libraryId, editingBook.id, payload);
        showSuccess("کتاب با موفقیت ویرایش شد");
      } else {
        await bookAPI.createBook(libraryId, payload);
        showSuccess("کتاب با موفقیت افزوده شد");
      }
      setShowModal(false);
      fetchBooks();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در ذخیره کتاب");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      setDeleting(true);
      await bookAPI.deleteBook(libraryId, deleteTarget.id);
      showSuccess("کتاب حذف شد");
      setDeleteTarget(null);
      fetchBooks();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در حذف کتاب");
    } finally {
      setDeleting(false);
    }
  };

  const handleAddCopies = async (e) => {
    e.preventDefault();
    if (!copyTarget || !libraryId) return;
    try {
      setAddingCopies(true);
      await bookAPI.addCopies(libraryId, copyTarget.id, Number(copyCount));
      showSuccess(`${copyCount} نسخه به کتاب افزوده شد`);
      setCopyTarget(null);
      setCopyCount(1);
      fetchBooks();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در افزودن نسخه");
    } finally {
      setAddingCopies(false);
    }
  };

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">مدیریت کتاب‌ها</h1>
        <p className="ap-subtitle">افزودن، ویرایش و مدیریت نسخه‌های کتاب‌های کتابخانه</p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

      {/* Toolbar */}
      <div className="ap-toolbar">
        <input
          className="abk-search"
          type="text"
          placeholder="جستجو بر اساس عنوان یا نویسنده..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        <button className="btn btn-accent" onClick={openAddModal}>
          + افزودن کتاب
        </button>
      </div>

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : books.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📚</span>
          <p>{searchQuery ? "کتابی با این مشخصات یافت نشد" : "هنوز کتابی ثبت نشده است"}</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="modern-table">
            <thead>
              <tr>
                <th>عنوان</th>
                <th>نویسنده</th>
                <th>سال</th>
                <th>نسخه‌های موجود</th>
                <th>عملیات</th>
              </tr>
            </thead>
            <tbody>
              {books.map((book) => (
                <tr key={book.id}>
                  <td>
                    <div className="abk-title">{book.title}</div>
                    {book.publisher && (
                      <div className="abk-publisher">{book.publisher}</div>
                    )}
                  </td>
                  <td>{book.author}</td>
                  <td>{book.publicationYear || "—"}</td>
                  <td>
                    <span
                      className={`badge ${
                        book.availableCopiesCount > 0
                          ? "badge-success"
                          : "badge-danger"
                      }`}
                    >
                      {book.availableCopiesCount ?? 0} موجود
                    </span>
                  </td>
                  <td>
                    <div className="abk-actions">
                      <button
                        className="btn-icon btn-icon--edit"
                        title="ویرایش"
                        onClick={() => openEditModal(book)}
                      >
                        ✏️
                      </button>
                      <button
                        className="btn-icon btn-icon--copy"
                        title="افزودن نسخه"
                        onClick={() => {
                          setCopyTarget(book);
                          setCopyCount(1);
                        }}
                      >
                        📦
                      </button>
                      <button
                        className="btn-icon btn-icon--delete"
                        title="حذف"
                        onClick={() => setDeleteTarget(book)}
                      >
                        🗑️
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="ap-modal-overlay" onClick={() => setShowModal(false)}>
          <div className="ap-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">
              {editingBook ? "ویرایش کتاب" : "افزودن کتاب جدید"}
            </h2>
            <form onSubmit={handleSave}>
              <div className="ap-form-grid">
                <div className="ap-form-group">
                  <label>عنوان *</label>
                  <input name="title" value={form.title} onChange={handleFormChange} required placeholder="عنوان کتاب" />
                </div>
                <div className="ap-form-group">
                  <label>نویسنده *</label>
                  <input name="author" value={form.author} onChange={handleFormChange} required placeholder="نام نویسنده" />
                </div>
                <div className="ap-form-group">
                  <label>ناشر</label>
                  <input name="publisher" value={form.publisher} onChange={handleFormChange} placeholder="نام ناشر" />
                </div>
                <div className="ap-form-group">
                  <label>سال انتشار</label>
                  <input name="publicationYear" type="number" value={form.publicationYear} onChange={handleFormChange} placeholder="مثلاً ۱۴۰۲" min="1000" max="2100" />
                </div>
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label>توضیحات</label>
                  <textarea name="description" value={form.description} onChange={handleFormChange} rows={3} placeholder="خلاصه‌ای از کتاب..." />
                </div>
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label>آدرس تصویر جلد</label>
                  <input name="coverImageUrl" value={form.coverImageUrl} onChange={handleFormChange} placeholder="https://..." />
                </div>
              </div>
              <div className="ap-modal-actions">
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  {saving ? "در حال ذخیره..." : editingBook ? "ذخیره تغییرات" : "افزودن کتاب"}
                </button>
                <button className="btn btn-outline" type="button" onClick={() => setShowModal(false)}>
                  انصراف
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirm */}
      {deleteTarget && (
        <div className="ap-confirm-overlay">
          <div className="ap-confirm-box">
            <div className="ap-confirm-icon">🗑️</div>
            <div className="ap-confirm-title">حذف کتاب</div>
            <p className="ap-confirm-msg">
              آیا از حذف کتاب «{deleteTarget.title}» اطمینان دارید؟ این عمل قابل بازگشت نیست.
            </p>
            <div className="ap-confirm-actions">
              <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
                {deleting ? "در حال حذف..." : "بله، حذف کن"}
              </button>
              <button className="btn btn-outline" onClick={() => setDeleteTarget(null)}>
                انصراف
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Copies Modal */}
      {copyTarget && (
        <div className="ap-modal-overlay" onClick={() => setCopyTarget(null)}>
          <div className="ap-modal" style={{ maxWidth: 380 }} onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">افزودن نسخه فیزیکی</h2>
            <p style={{ fontSize: "0.88rem", color: "#6b7280", marginBottom: "1.25rem" }}>
              کتاب: <strong>{copyTarget.title}</strong>
            </p>
            <form onSubmit={handleAddCopies}>
              <div className="ap-form-group">
                <label>تعداد نسخه برای افزودن</label>
                <input
                  type="number"
                  value={copyCount}
                  onChange={(e) => setCopyCount(e.target.value)}
                  min={1}
                  max={50}
                  required
                />
              </div>
              <div className="ap-modal-actions">
                <button className="btn btn-primary" type="submit" disabled={addingCopies}>
                  {addingCopies ? "در حال افزودن..." : "افزودن نسخه‌ها"}
                </button>
                <button className="btn btn-outline" type="button" onClick={() => setCopyTarget(null)}>
                  انصراف
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminBooksPage;
