import React, { useEffect, useState, useCallback, useRef } from "react";
import { useParams } from "react-router-dom";
import { bookAPI } from "../../services/api";
import "./AdminBooksPage.css";

const EMPTY_FORM = {
  title: "",
  author: "",
  publisher: "",
  publicationYear: "",
  description: "",
  autoDigitalBorrowEnabled: false,
  initialCopies: "",
};

const UploadProgressBar = ({ progress }) => (
  <div className="upload-progress-wrap">
    <div className="upload-progress-bar" style={{ width: `${progress}%` }} />
    <span className="upload-progress-text">{progress}%</span>
  </div>
);

const AdminBooksPage = () => {
  const { libraryId } = useParams();

  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  // Add/Edit modal
  const [showModal, setShowModal] = useState(false);
  const [editingBook, setEditingBook] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  // Cover image upload state
  const [coverFile, setCoverFile] = useState(null);
  const [coverPreview, setCoverPreview] = useState(null);
  const [coverProgress, setCoverProgress] = useState(0);
  const [coverUploading, setCoverUploading] = useState(false);
  const coverInputRef = useRef(null);

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Copy management
  const [copyTarget, setCopyTarget] = useState(null);
  const [copyCount, setCopyCount] = useState(1);
  const [addingCopies, setAddingCopies] = useState(false);

  // Initial PDF file for create form
  const [createPdfFile, setCreatePdfFile] = useState(null);
  const [createPdfProgress, setCreatePdfProgress] = useState(0);
  const [createPdfUploading, setCreatePdfUploading] = useState(false);
  const createPdfInputRef = useRef(null);

  // Digital book management
  const [digitalTarget, setDigitalTarget] = useState(null);
  const [digitalBooks, setDigitalBooks] = useState([]);
  const [digitalLoading, setDigitalLoading] = useState(false);
  const [digitalFile, setDigitalFile] = useState(null);
  const [digitalVersionName, setDigitalVersionName] = useState("");
  const [digitalProgress, setDigitalProgress] = useState(0);
  const [digitalUploading, setDigitalUploading] = useState(false);
  const [digitalError, setDigitalError] = useState("");
  const digitalInputRef = useRef(null);

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
    } catch {
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
    setCoverFile(null);
    setCoverPreview(null);
    setCoverProgress(0);
    setCreatePdfFile(null);
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
      autoDigitalBorrowEnabled: book.autoDigitalBorrowEnabled || false,
    });
    setCoverFile(null);
    setCoverPreview(book.coverImageUrl || null);
    setCoverProgress(0);
    setShowModal(true);
  };

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
  };

  const handleCoverFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setCoverFile(file);
    setCoverPreview(URL.createObjectURL(file));
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!libraryId) return;
    try {
      setSaving(true);
      setError("");
      const payload = {
        ...form,
        publicationYear: form.publicationYear ? Number(form.publicationYear) : undefined,
        publisher: form.publisher || undefined,
        description: form.description || undefined,
      };

      let savedBook;
      if (editingBook) {
        const res = await bookAPI.updateBook(libraryId, editingBook.id, payload);
        savedBook = res.data?.data || res.data;
        showSuccess("کتاب با موفقیت ویرایش شد");
      } else {
        const res = await bookAPI.createBook(libraryId, payload);
        savedBook = res.data?.data || res.data;
        showSuccess("کتاب با موفقیت افزوده شد");
      }

      if (savedBook?.id) {
        // Upload cover image if selected
        if (coverFile) {
          try {
            setCoverUploading(true);
            await bookAPI.uploadCoverImage(libraryId, savedBook.id, coverFile, (p) => setCoverProgress(p));
          } catch {
            setError("کتاب ذخیره شد ولی آپلود تصویر جلد با خطا مواجه شد");
          } finally {
            setCoverUploading(false);
          }
        }

        // For new books: add physical copies and/or upload PDF
        if (!editingBook) {
          const copies = Number(form.initialCopies);
          if (copies > 0) {
            try {
              await bookAPI.addCopies(libraryId, savedBook.id, copies);
            } catch {
              setError("کتاب ذخیره شد ولی افزودن نسخه فیزیکی با خطا مواجه شد");
            }
          }
          if (createPdfFile) {
            try {
              setCreatePdfUploading(true);
              setCreatePdfProgress(0);
              await bookAPI.uploadDigitalBook(libraryId, savedBook.id, createPdfFile, undefined, (p) => setCreatePdfProgress(p));
            } catch (err) {
              setError(err.response?.data?.error || err.response?.data?.message || "کتاب ذخیره شد ولی آپلود PDF با خطا مواجه شد");
            } finally {
              setCreatePdfUploading(false);
            }
          }
        }
      }

      setShowModal(false);
      fetchBooks();
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || "خطا در ذخیره کتاب");
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

  // Digital book management
  const openDigitalModal = async (book) => {
    setDigitalTarget(book);
    setDigitalBooks([]);
    setDigitalFile(null);
    setDigitalVersionName("");
    setDigitalProgress(0);
    setDigitalError("");
    setDigitalLoading(true);
    try {
      const res = await bookAPI.listDigitalBooks(libraryId, book.id);
      setDigitalBooks(res.data?.data || []);
    } catch {
      setDigitalError("خطا در بارگذاری نسخه‌های دیجیتال");
    } finally {
      setDigitalLoading(false);
    }
  };

  const handleDigitalFileChange = (e) => {
    setDigitalFile(e.target.files?.[0] || null);
  };

  const handleUploadDigital = async (e) => {
    e.preventDefault();
    if (!digitalFile || !digitalTarget) return;
    try {
      setDigitalUploading(true);
      setDigitalProgress(0);
      setDigitalError("");
      const res = await bookAPI.uploadDigitalBook(
        libraryId, digitalTarget.id, digitalFile,
        digitalVersionName || undefined,
        (p) => setDigitalProgress(p)
      );
      const newBook = res.data?.data;
      setDigitalBooks((prev) => [...prev, newBook]);
      setDigitalFile(null);
      setDigitalVersionName("");
      setDigitalProgress(0);
      if (digitalInputRef.current) digitalInputRef.current.value = "";
      fetchBooks();
    } catch (err) {
      setDigitalError(err.response?.data?.error || err.response?.data?.message || "خطا در آپلود فایل دیجیتال");
    } finally {
      setDigitalUploading(false);
    }
  };

  const handleDeleteDigital = async (digitalBookId) => {
    try {
      await bookAPI.deleteDigitalBook(libraryId, digitalTarget.id, digitalBookId);
      setDigitalBooks((prev) => prev.filter((d) => d.id !== digitalBookId));
      fetchBooks();
    } catch (err) {
      setDigitalError(err.response?.data?.message || "خطا در حذف نسخه دیجیتال");
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return "";
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">مدیریت کتاب‌ها</h1>
        <p className="ap-subtitle">افزودن، ویرایش و مدیریت نسخه‌های کتاب‌های کتابخانه</p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

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
                <th>جلد</th>
                <th>عنوان</th>
                <th>نویسنده</th>
                <th>سال</th>
                <th>موجود</th>
                <th>دیجیتال</th>
                <th>عملیات</th>
              </tr>
            </thead>
            <tbody>
              {books.map((book) => (
                <tr key={book.id}>
                  <td>
                    {book.coverImageUrl ? (
                      <img src={book.coverImageUrl} alt="جلد" className="abk-cover-thumb" />
                    ) : (
                      <div className="abk-cover-placeholder">📖</div>
                    )}
                  </td>
                  <td>
                    <div className="abk-title">{book.title}</div>
                    {book.publisher && <div className="abk-publisher">{book.publisher}</div>}
                  </td>
                  <td>{book.author}</td>
                  <td>{book.publicationYear || "—"}</td>
                  <td>
                    <span className={`badge ${book.availableCopiesCount > 0 ? "badge-success" : "badge-danger"}`}>
                      {book.availableCopiesCount ?? 0} موجود
                    </span>
                  </td>
                  <td>
                    {book.hasDigitalVersions ? (
                      <span className="badge badge-info">دارد</span>
                    ) : (
                      <span className="badge badge-muted">ندارد</span>
                    )}
                  </td>
                  <td>
                    <div className="abk-actions">
                      <button className="btn-icon btn-icon--edit" title="ویرایش" onClick={() => openEditModal(book)}>✏️</button>
                      <button className="btn-icon btn-icon--copy" title="افزودن نسخه فیزیکی" onClick={() => { setCopyTarget(book); setCopyCount(1); }}>📦</button>
                      <button className="btn-icon btn-icon--digital" title="مدیریت نسخه دیجیتال" onClick={() => openDigitalModal(book)}>💾</button>
                      <button className="btn-icon btn-icon--delete" title="حذف" onClick={() => setDeleteTarget(book)}>🗑️</button>
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
            <h2 className="ap-modal-title">{editingBook ? "ویرایش کتاب" : "افزودن کتاب جدید"}</h2>
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
                  <label>تصویر جلد</label>
                  <div className="cover-upload-area">
                    {coverPreview && (
                      <img src={coverPreview} alt="پیش‌نمایش جلد" className="cover-preview-img" />
                    )}
                    <div className="cover-upload-controls">
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        onClick={() => coverInputRef.current?.click()}
                      >
                        {coverPreview ? "تغییر تصویر" : "انتخاب تصویر"}
                      </button>
                      {coverFile && <span className="cover-filename">{coverFile.name}</span>}
                      <input
                        ref={coverInputRef}
                        type="file"
                        accept="image/*"
                        style={{ display: "none" }}
                        onChange={handleCoverFileChange}
                      />
                    </div>
                    {coverUploading && <UploadProgressBar progress={coverProgress} />}
                  </div>
                </div>
                {!editingBook && (
                  <>
                    <div className="ap-form-group">
                      <label>تعداد نسخه فیزیکی اولیه</label>
                      <input
                        name="initialCopies"
                        type="number"
                        value={form.initialCopies}
                        onChange={handleFormChange}
                        placeholder="۰ = بدون نسخه فیزیکی"
                        min="0"
                        max="100"
                      />
                    </div>
                    <div className="ap-form-group">
                      <label>فایل PDF <span style={{ color: "var(--color-text-muted)", fontSize: "0.8rem" }}>(اختیاری)</span></label>
                      <div className="digital-file-pick">
                        <button
                          type="button"
                          className="btn btn-outline btn-sm"
                          onClick={() => createPdfInputRef.current?.click()}
                        >
                          انتخاب فایل PDF
                        </button>
                        {createPdfFile
                          ? <span className="cover-filename">{createPdfFile.name}</span>
                          : <span style={{ color: "var(--color-text-muted)", fontSize: "0.85rem" }}>فایلی انتخاب نشده</span>
                        }
                        <input
                          ref={createPdfInputRef}
                          type="file"
                          accept=".pdf,application/pdf"
                          style={{ display: "none" }}
                          onChange={(e) => setCreatePdfFile(e.target.files?.[0] || null)}
                        />
                      </div>
                      {createPdfUploading && <UploadProgressBar progress={createPdfProgress} />}
                    </div>
                  </>
                )}
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      name="autoDigitalBorrowEnabled"
                      checked={form.autoDigitalBorrowEnabled}
                      onChange={handleFormChange}
                    />
                    تأیید خودکار امانت دیجیتال
                  </label>
                </div>
              </div>
              <div className="ap-modal-actions">
                <button className="btn btn-primary" type="submit" disabled={saving || coverUploading || createPdfUploading}>
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
            <p className="ap-confirm-msg">آیا از حذف کتاب «{deleteTarget.title}» اطمینان دارید؟ این عمل قابل بازگشت نیست.</p>
            <div className="ap-confirm-actions">
              <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
                {deleting ? "در حال حذف..." : "بله، حذف کن"}
              </button>
              <button className="btn btn-outline" onClick={() => setDeleteTarget(null)}>انصراف</button>
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
                <input type="number" value={copyCount} onChange={(e) => setCopyCount(e.target.value)} min={1} max={50} required />
              </div>
              <div className="ap-modal-actions">
                <button className="btn btn-primary" type="submit" disabled={addingCopies}>
                  {addingCopies ? "در حال افزودن..." : "افزودن نسخه‌ها"}
                </button>
                <button className="btn btn-outline" type="button" onClick={() => setCopyTarget(null)}>انصراف</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Digital Book Management Modal */}
      {digitalTarget && (
        <div className="ap-modal-overlay" onClick={() => setDigitalTarget(null)}>
          <div className="ap-modal ap-modal--wide" onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">مدیریت نسخه‌های دیجیتال</h2>
            <p style={{ fontSize: "0.88rem", color: "#6b7280", marginBottom: "1.25rem" }}>
              کتاب: <strong>{digitalTarget.title}</strong>
            </p>

            {digitalError && <div className="error-message" style={{ marginBottom: "1rem" }}>{digitalError}</div>}

            {/* Existing digital books */}
            {digitalLoading ? (
              <div className="loading" style={{ fontSize: "0.9rem" }}>در حال بارگذاری...</div>
            ) : digitalBooks.length > 0 ? (
              <div className="digital-books-list">
                {digitalBooks.map((db) => (
                  <div key={db.id} className="digital-book-item">
                    <span className="digital-format-badge">{db.fileFormat}</span>
                    <span className="digital-filename">{db.originalFilename}</span>
                    <span className="digital-size">{formatFileSize(db.fileSizeBytes)}</span>
                    {db.versionName && <span className="digital-version">{db.versionName}</span>}
                    <button
                      className="btn-icon btn-icon--delete"
                      title="حذف"
                      onClick={() => handleDeleteDigital(db.id)}
                    >🗑️</button>
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ color: "var(--color-text-muted)", fontSize: "0.9rem", marginBottom: "1rem" }}>هنوز نسخه دیجیتالی آپلود نشده است.</p>
            )}

            <div className="digital-upload-divider" />

            {/* Upload new digital book */}
            <form onSubmit={handleUploadDigital}>
              <div className="ap-form-grid">
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label>فایل PDF *</label>
                  <div className="digital-file-pick">
                    <button
                      type="button"
                      className="btn btn-outline btn-sm"
                      onClick={() => digitalInputRef.current?.click()}
                    >
                      انتخاب فایل PDF
                    </button>
                    {digitalFile
                      ? <span className="cover-filename">{digitalFile.name} ({formatFileSize(digitalFile.size)})</span>
                      : <span style={{ color: "var(--color-text-muted)", fontSize: "0.85rem" }}>فایلی انتخاب نشده</span>
                    }
                    <input
                      ref={digitalInputRef}
                      type="file"
                      accept=".pdf,application/pdf"
                      style={{ display: "none" }}
                      onChange={handleDigitalFileChange}
                    />
                  </div>
                  {digitalUploading && <UploadProgressBar progress={digitalProgress} />}
                </div>
              </div>
              <div className="ap-modal-actions">
                <button className="btn btn-primary" type="submit" disabled={digitalUploading || !digitalFile}>
                  {digitalUploading ? `در حال آپلود... ${digitalProgress}%` : "آپلود نسخه دیجیتال"}
                </button>
                <button className="btn btn-outline" type="button" onClick={() => setDigitalTarget(null)}>بستن</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminBooksPage;
