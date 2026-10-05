import React, { useEffect, useState, useCallback, useRef } from "react";
import { useParams } from "react-router-dom";
import { bookAPI, subjectAPI } from "../../services/api";
import { toPersian, toPersianNum } from "../../utils/persian";
import ClassificationBadge from "../../components/ClassificationBadge";
import { CLASSIFICATION_LEVELS } from "../../utils/classification";
import "./AdminBooksPage.css";

const EMPTY_FORM = {
  title: "",
  author: "",
  publisher: "",
  publicationYear: "",
  subjectIds: [],
  description: "",
  autoDigitalBorrowEnabled: false,
  classification: "UNCLASSIFIED",
  initialCopies: "",
};

const UploadProgressBar = ({ progress }) => (
  <div className="upload-progress-wrap">
    <div className="upload-progress-bar" style={{ width: `${progress}%` }} />
    <span className="upload-progress-text">{toPersian(progress)}٪</span>
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

  // Subjects
  const [subjects, setSubjects] = useState([]);
  const [showSubjectModal, setShowSubjectModal] = useState(false);
  const [newSubjectName, setNewSubjectName] = useState("");
  const [savingSubject, setSavingSubject] = useState(false);
  const [deleteSubjectTarget, setDeleteSubjectTarget] = useState(null); // {id, name, bookCount}

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

  // Soft-deleted books (restore panel)
  const [showDeletedModal, setShowDeletedModal] = useState(false);
  const [deletedBooks, setDeletedBooks] = useState([]);
  const [deletedLoading, setDeletedLoading] = useState(false);
  const [restoringId, setRestoringId] = useState(null);

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

  const fetchSubjects = useCallback(async () => {
    if (!libraryId) return;
    try {
      const res = await subjectAPI.getSubjects(libraryId);
      setSubjects(res.data?.data || []);
    } catch { /* ignore */ }
  }, [libraryId]);

  useEffect(() => { fetchSubjects(); }, [fetchSubjects]);

  const fetchDeletedBooks = useCallback(async () => {
    if (!libraryId) return;
    try {
      setDeletedLoading(true);
      const res = await bookAPI.getDeletedBooks(libraryId, { page: 0, size: 100 });
      const data = res.data?.data;
      setDeletedBooks(data?.content ?? (Array.isArray(data) ? data : []));
    } catch {
      setError("خطا در بارگذاری کتاب‌های حذف‌شده");
    } finally {
      setDeletedLoading(false);
    }
  }, [libraryId]);

  const openDeletedModal = () => {
    setShowDeletedModal(true);
    fetchDeletedBooks();
  };

  const handleRestore = async (book) => {
    try {
      setRestoringId(book.id);
      await bookAPI.restoreBook(libraryId, book.id);
      showSuccess(`کتاب «${book.title}» بازگردانی شد`);
      setDeletedBooks((prev) => prev.filter((b) => b.id !== book.id));
      fetchBooks();
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || "خطا در بازگردانی کتاب");
    } finally {
      setRestoringId(null);
    }
  };

  const openAddModal = () => {
    setEditingBook(null);
    setForm(EMPTY_FORM);
    setCoverFile(null);
    setCoverPreview(null);
    setCoverProgress(0);
    setCreatePdfFile(null);
    setShowModal(true);
  };

  const openEditModal = async (book) => {
    setEditingBook(book);
    setForm({
      title: book.title || "",
      author: book.author || "",
      publisher: book.publisher || "",
      publicationYear: book.publicationYear || "",
      subjectIds: book.subjectIds || [],
      description: book.description || "",
      autoDigitalBorrowEnabled: book.autoDigitalBorrowEnabled || false,
      classification: book.classification || "UNCLASSIFIED",
      initialCopies: "",
    });
    setCoverFile(null);
    setCoverPreview(book.coverImageUrl || null);
    setCoverProgress(0);
    // reset inline physical-copy + digital management state
    setCopyCount(book.totalCopiesCount ?? 0);
    setDigitalFile(null);
    setDigitalVersionName("");
    setDigitalProgress(0);
    setDigitalError("");
    setDigitalBooks([]);
    setShowModal(true);
    // load this book's digital versions for in-modal management
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

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
  };

  const toggleSubjectId = (id) => {
    setForm((prev) => ({
      ...prev,
      subjectIds: prev.subjectIds.includes(id)
        ? prev.subjectIds.filter((s) => s !== id)
        : [...prev.subjectIds, id],
    }));
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
        title: form.title,
        author: form.author,
        publisher: form.publisher || undefined,
        publicationYear: form.publicationYear ? Number(form.publicationYear) : undefined,
        subjectIds: form.subjectIds.length > 0 ? form.subjectIds : undefined,
        description: form.description || undefined,
        autoDigitalBorrowEnabled: form.autoDigitalBorrowEnabled,
        classification: form.classification,
      };

      const wasEditing = !!editingBook;

      // One request does everything this modal touches: metadata + cover + digital PDF + copy
      // count. Previously this was up to 4 sequential calls (create/update, cover, copies, PDF).
      let targetCopyCount;
      if (wasEditing) {
        const n = Number(copyCount);
        if (!Number.isNaN(n) && n !== (editingBook.totalCopiesCount ?? 0)) targetCopyCount = n;
      } else {
        const n = Number(form.initialCopies);
        if (n > 0) targetCopyCount = n;
      }
      const pdfFile = wasEditing ? digitalFile : createPdfFile;
      const pdfVersionName = wasEditing ? (digitalVersionName || undefined) : undefined;

      if (coverFile) setCoverUploading(true);
      if (pdfFile) { wasEditing ? setDigitalUploading(true) : setCreatePdfUploading(true); }

      try {
        await bookAPI.saveBook(libraryId, {
          bookId: editingBook?.id,
          book: payload,
          cover: coverFile || undefined,
          digitalFile: pdfFile || undefined,
          digitalVersionName: pdfVersionName,
          copyCount: targetCopyCount,
          onProgress: (p) => {
            if (coverFile) setCoverProgress(p);
            if (pdfFile) (wasEditing ? setDigitalProgress : setCreatePdfProgress)(p);
          },
        });
      } catch (err) {
        setError(err.response?.data?.error || err.response?.data?.message ||
          (wasEditing ? "خطا در ذخیره‌ی تغییرات کتاب" : "خطا در افزودن کتاب"));
        return;
      } finally {
        setCoverUploading(false);
        setDigitalUploading(false);
        setCreatePdfUploading(false);
      }

      if (wasEditing && pdfFile) {
        setDigitalFile(null);
        setDigitalVersionName("");
        if (digitalInputRef.current) digitalInputRef.current.value = "";
      }

      showSuccess(wasEditing ? "کتاب با موفقیت ویرایش شد" : "کتاب با موفقیت افزوده شد");
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
      showSuccess(`${toPersianNum(copyCount)} نسخه به کتاب افزوده شد`);
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
    const target = editingBook || digitalTarget;
    if (!digitalFile || !target) return;
    try {
      setDigitalUploading(true);
      setDigitalProgress(0);
      setDigitalError("");
      const res = await bookAPI.uploadDigitalBook(
        libraryId, target.id, digitalFile,
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
    const target = editingBook || digitalTarget;
    try {
      await bookAPI.deleteDigitalBook(libraryId, target.id, digitalBookId);
      setDigitalBooks((prev) => prev.filter((d) => d.id !== digitalBookId));
      fetchBooks();
    } catch (err) {
      setDigitalError(err.response?.data?.message || "خطا در حذف نسخه دیجیتال");
    }
  };

  const handleAddSubject = async () => {
    if (!newSubjectName.trim()) return;
    try {
      setSavingSubject(true);
      await subjectAPI.createSubject(libraryId, newSubjectName.trim());
      setNewSubjectName("");
      await fetchSubjects();
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || "خطا در افزودن موضوع");
    } finally {
      setSavingSubject(false);
    }
  };

  const handleDeleteSubject = (subject) => {
    setDeleteSubjectTarget(subject);
  };

  const confirmDeleteSubject = async () => {
    if (!deleteSubjectTarget) return;
    try {
      await subjectAPI.deleteSubject(libraryId, deleteSubjectTarget.id);
      setDeleteSubjectTarget(null);
      await fetchSubjects();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در حذف موضوع");
      setDeleteSubjectTarget(null);
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
        <button className="btn btn-ghost btn-sm" onClick={() => setShowSubjectModal(true)}>
          🏷️ مدیریت موضوعات
        </button>
        <button className="btn btn-ghost btn-sm" onClick={openDeletedModal}>
          🗑️ کتاب‌های حذف‌شده
        </button>
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
                      <img src={`${book.coverImageUrl}?v=${encodeURIComponent(book.updatedAt || "")}`} alt="جلد" className="abk-cover-thumb" />
                    ) : (
                      <div className="abk-cover-placeholder">📖</div>
                    )}
                  </td>
                  <td>
                    <div className="abk-title">
                      {book.title} <ClassificationBadge level={book.classification} />
                    </div>
                    {book.publisher && <div className="abk-publisher">{book.publisher}</div>}
                  </td>
                  <td>{book.author}</td>
                  <td>{book.publicationYear ? toPersian(book.publicationYear) : "—"}</td>
                  <td>
                    <span className={`badge ${book.availableCopiesCount > 0 ? "badge-success" : "badge-danger"}`}>
                      {toPersianNum(book.availableCopiesCount ?? 0)} موجود
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
                      <button className="btn-icon btn-icon--edit" title="ویرایش (اطلاعات، نسخه چاپی و دیجیتال)" onClick={() => openEditModal(book)}>✏️</button>
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
            {error && <div className="error-message" style={{ marginBottom: "1rem" }}>{error}</div>}
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
                <div className="ap-form-group">
                  <label>سطح طبقه‌بندی</label>
                  <select name="classification" value={form.classification} onChange={handleFormChange}>
                    {CLASSIFICATION_LEVELS.map((l) => (
                      <option key={l.value} value={l.value}>{l.label}</option>
                    ))}
                  </select>
                </div>
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label>موضوعات (می‌توانید چند موضوع انتخاب کنید)</label>
                  {subjects.length === 0 ? (
                    <small style={{ color: "var(--color-text-muted)", fontSize: "0.75rem" }}>
                      ابتدا از «مدیریت موضوعات» موضوع تعریف کنید
                    </small>
                  ) : (
                    <div className="subject-chip-list">
                      {subjects.map((s) => (
                        <button
                          key={s.id}
                          type="button"
                          className={`subject-chip ${form.subjectIds.includes(s.id) ? "subject-chip--selected" : ""}`}
                          onClick={() => toggleSubjectId(s.id)}
                        >
                          {s.name}
                        </button>
                      ))}
                    </div>
                  )}
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

                {editingBook && (
                  <div className="ap-form-group abk-edit-section" style={{ gridColumn: "1 / -1" }}>
                    <label>تعداد نسخه‌های چاپی</label>
                    <div className="abk-inline-row">
                      <span className="abk-inline-count">
                        موجود: {toPersian(editingBook.availableCopiesCount ?? 0)} از {toPersian(editingBook.totalCopiesCount ?? 0)}
                      </span>
                      <input
                        type="number" min="0" max="1000" value={copyCount}
                        onChange={(e) => setCopyCount(e.target.value)}
                        style={{ width: 110 }}
                      />
                    </div>
                    <span style={{ fontSize: "0.75rem", color: "var(--color-text-muted)" }}>
                      با «ذخیره تغییرات» پایین فرم اعمال می‌شود؛ نمی‌توان کمتر از تعداد نسخه‌های در حال امانت تنظیم کرد.
                    </span>
                  </div>
                )}

                {editingBook && (
                  <div className="ap-form-group abk-edit-section" style={{ gridColumn: "1 / -1" }}>
                    <label>نسخه‌های دیجیتال (PDF)</label>
                    {digitalError && <div className="error-message" style={{ marginBottom: "0.5rem" }}>{digitalError}</div>}
                    {digitalLoading ? (
                      <p style={{ fontSize: "0.85rem", color: "var(--color-text-muted)" }}>در حال بارگذاری...</p>
                    ) : digitalBooks.length === 0 ? (
                      <p style={{ fontSize: "0.85rem", color: "var(--color-text-muted)" }}>نسخه دیجیتالی ثبت نشده است.</p>
                    ) : (
                      <ul className="abk-digital-list">
                        {digitalBooks.map((db) => (
                          <li key={db.id} className="abk-digital-item">
                            <span>📄 {db.versionName || db.fileFormat || "PDF"}</span>
                            <button type="button" className="btn-icon btn-icon--delete" title="حذف"
                              onClick={() => handleDeleteDigital(db.id)}>🗑️</button>
                          </li>
                        ))}
                      </ul>
                    )}
                    <div className="abk-inline-row" style={{ marginTop: "0.5rem" }}>
                      <button type="button" className="btn btn-outline btn-sm" onClick={() => digitalInputRef.current?.click()}>
                        انتخاب فایل PDF
                      </button>
                      {digitalFile
                        ? <span className="cover-filename">{digitalFile.name}</span>
                        : <span style={{ color: "var(--color-text-muted)", fontSize: "0.82rem" }}>فایلی انتخاب نشده</span>}
                      <input
                        type="text" value={digitalVersionName}
                        onChange={(e) => setDigitalVersionName(e.target.value)}
                        placeholder="نام نسخه (اختیاری)" style={{ width: 150 }}
                      />
                      <button type="button" className="btn btn-primary btn-sm"
                        onClick={handleUploadDigital} disabled={!digitalFile || digitalUploading}>
                        {digitalUploading ? "..." : "⬆ آپلود"}
                      </button>
                      <input ref={digitalInputRef} type="file" accept=".pdf,application/pdf"
                        style={{ display: "none" }} onChange={handleDigitalFileChange} />
                    </div>
                    {digitalUploading && <UploadProgressBar progress={digitalProgress} />}
                  </div>
                )}

                {!editingBook && (
                  <>
                    <div className="ap-form-group">
                      <label>تعداد نسخه چاپی اولیه</label>
                      <input
                        name="initialCopies"
                        type="number"
                        value={form.initialCopies}
                        onChange={handleFormChange}
                        placeholder="۰ = بدون نسخه چاپی"
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
                {editingBook && (
                  <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                    <span style={{ fontSize: "0.8rem", color: "var(--color-text-muted)" }}>
                      💡 نسخه‌های دیجیتال برای همه‌ی اعضای کتابخانه به‌صورت آنی قابل دانلود است.
                    </span>
                  </div>
                )}
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
            <h2 className="ap-modal-title">افزودن نسخه چاپی</h2>
            <p className="ap-subtitle" style={{ marginBottom: "1.25rem" }}>
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
            <p className="ap-subtitle" style={{ marginBottom: "1.25rem" }}>
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
                  {digitalUploading ? `در حال آپلود... ${toPersian(digitalProgress)}٪` : "آپلود نسخه دیجیتال"}
                </button>
                <button className="btn btn-outline" type="button" onClick={() => setDigitalTarget(null)}>بستن</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Subject Management Modal */}
      {showSubjectModal && (
        <div className="ap-modal-overlay" onClick={() => setShowSubjectModal(false)}>
          <div className="ap-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 420 }}>
            <h2 className="ap-modal-title">🏷️ مدیریت موضوعات</h2>
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
              موضوعات تعریف‌شده در این کتابخانه:
            </p>

            {subjects.length === 0 ? (
              <p className="subject-manage-empty">هنوز موضوعی تعریف نشده</p>
            ) : (
              <ul className="subject-manage-list">
                {subjects.map((s) => (
                  <li key={s.id} className="subject-manage-item">
                    <span className="subject-manage-name">{s.name}</span>
                    <button className="btn-icon btn-icon--delete" title="حذف" onClick={() => handleDeleteSubject(s)}>🗑️</button>
                  </li>
                ))}
              </ul>
            )}

            <div className="subject-manage-add-row">
              <input
                className="subject-manage-input"
                placeholder="نام موضوع جدید..."
                value={newSubjectName}
                onChange={(e) => setNewSubjectName(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleAddSubject()}
              />
              <button className="btn btn-primary btn-sm" onClick={handleAddSubject} disabled={savingSubject || !newSubjectName.trim()}>
                افزودن
              </button>
            </div>

            <div className="ap-modal-actions" style={{ marginTop: "1rem" }}>
              <button className="btn btn-outline" onClick={() => setShowSubjectModal(false)}>بستن</button>
            </div>
          </div>
        </div>
      )}

      {/* Subject Delete Confirmation */}
      {deleteSubjectTarget && (
        <div className="ap-confirm-overlay">
          <div className="ap-confirm-box">
            <div className="ap-confirm-icon">🏷️</div>
            <div className="ap-confirm-title">حذف موضوع</div>
            {deleteSubjectTarget.bookCount > 0 ? (
              <p className="ap-confirm-msg">
                موضوع «{deleteSubjectTarget.name}» در{" "}
                <strong>{toPersianNum(deleteSubjectTarget.bookCount)} کتاب</strong> استفاده شده است.
                <br />
                با حذف این موضوع، از همه آن کتاب‌ها نیز حذف می‌شود. ادامه می‌دهید؟
              </p>
            ) : (
              <p className="ap-confirm-msg">
                آیا از حذف موضوع «{deleteSubjectTarget.name}» اطمینان دارید؟
              </p>
            )}
            <div className="ap-confirm-actions">
              <button className="btn btn-danger" onClick={confirmDeleteSubject}>
                بله، حذف کن
              </button>
              <button className="btn btn-outline" onClick={() => setDeleteSubjectTarget(null)}>انصراف</button>
            </div>
          </div>
        </div>
      )}

      {/* Deleted Books / Restore Modal */}
      {showDeletedModal && (
        <div className="ap-modal-overlay" onClick={() => setShowDeletedModal(false)}>
          <div className="ap-modal ap-modal--wide" onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">🗑️ کتاب‌های حذف‌شده</h2>
            <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
              این کتاب‌ها در سراسر پنل مخفی هستند و قابل امانت نیستند. با «بازگردانی» دوباره فعال می‌شوند.
            </p>

            {deletedLoading ? (
              <div className="loading" style={{ fontSize: "0.9rem" }}>در حال بارگذاری...</div>
            ) : deletedBooks.length === 0 ? (
              <p className="subject-manage-empty">کتاب حذف‌شده‌ای وجود ندارد.</p>
            ) : (
              <ul className="subject-manage-list">
                {deletedBooks.map((b) => (
                  <li key={b.id} className="subject-manage-item">
                    <span className="subject-manage-name">
                      {b.title}
                      {b.author && <span style={{ color: "var(--color-text-muted)", fontWeight: 400 }}> — {b.author}</span>}
                    </span>
                    <button
                      className="btn btn-outline-success btn-sm"
                      onClick={() => handleRestore(b)}
                      disabled={restoringId === b.id}
                    >
                      {restoringId === b.id ? "..." : "↩ بازگردانی"}
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="ap-modal-actions">
              <button className="btn btn-outline" onClick={() => setShowDeletedModal(false)}>بستن</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminBooksPage;
