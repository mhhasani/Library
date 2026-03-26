import React, { useEffect, useState } from "react";
import { adminAPI, libraryAPI } from "../../services/api";
import "./SystemLibrariesPage.css";

const SystemLibrariesPage = () => {
  const [libraries, setLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [actionLoading, setActionLoading] = useState({});

  // Create library
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState({
    name: "",
    description: "",
    autoMembershipApproval: false,
    defaultBorrowDurationDays: 14,
  });
  const [creating, setCreating] = useState(false);

  // Edit library
  const [editTarget, setEditTarget] = useState(null);
  const [editForm, setEditForm] = useState({});
  const [editSaving, setEditSaving] = useState(false);

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(""), 3500);
  };

  const fetchLibraries = async () => {
    try {
      setLoading(true);
      const res = await adminAPI.getLibraries();
      setLibraries(res.data?.data || res.data || []);
      setError("");
    } catch (err) {
      setError("خطا در بارگذاری کتابخانه‌ها");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchLibraries(); }, []);

  const handleToggleActive = async (lib) => {
    try {
      setActionLoading((p) => ({ ...p, [lib.id]: true }));
      if (lib.isActive) {
        await libraryAPI.deleteLibrary(lib.id);
        showSuccess(`کتابخانه «${lib.name}» غیرفعال شد`);
      } else {
        await libraryAPI.updateLibrary(lib.id, {
          name: lib.name,
          description: lib.description,
          autoMembershipApproval: lib.autoMembershipApproval,
          defaultBorrowDurationDays: lib.defaultBorrowDurationDays,
        });
        showSuccess(`کتابخانه «${lib.name}» به‌روزرسانی شد`);
      }
      fetchLibraries();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در عملیات");
    } finally {
      setActionLoading((p) => ({ ...p, [lib.id]: false }));
    }
  };

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((p) => ({ ...p, [name]: type === "checkbox" ? checked : value }));
  };

  const openEditModal = (lib) => {
    setEditTarget(lib);
    setEditForm({
      name: lib.name,
      description: lib.description || "",
      autoMembershipApproval: lib.autoMembershipApproval,
      defaultBorrowDurationDays: lib.defaultBorrowDurationDays,
    });
  };

  const handleEditFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setEditForm((p) => ({ ...p, [name]: type === "checkbox" ? checked : value }));
  };

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    try {
      setEditSaving(true);
      await libraryAPI.updateLibrary(editTarget.id, {
        ...editForm,
        defaultBorrowDurationDays: Number(editForm.defaultBorrowDurationDays),
      });
      showSuccess(`کتابخانه «${editForm.name}» ویرایش شد`);
      setEditTarget(null);
      fetchLibraries();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در ویرایش کتابخانه");
    } finally {
      setEditSaving(false);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      setCreating(true);
      await libraryAPI.createLibrary({
        ...form,
        defaultBorrowDurationDays: Number(form.defaultBorrowDurationDays),
      });
      showSuccess("کتابخانه با موفقیت ایجاد شد");
      setForm({ name: "", description: "", autoMembershipApproval: false, defaultBorrowDurationDays: 14 });
      setShowCreateForm(false);
      fetchLibraries();
    } catch (err) {
      setError(err.response?.data?.message || "خطا در ایجاد کتابخانه");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div>
      <div className="ap-header">
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "1rem" }}>
          <div>
            <h1 className="ap-title">مدیریت کتابخانه‌ها</h1>
            <p className="ap-subtitle">ایجاد و مدیریت تمامی کتابخانه‌های سامانه</p>
          </div>
          <button className="btn btn-accent" onClick={() => setShowCreateForm(!showCreateForm)}>
            {showCreateForm ? "× بستن فرم" : "+ ایجاد کتابخانه"}
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMsg && <div className="success-message">{successMsg}</div>}

      {/* Create Form */}
      {showCreateForm && (
        <div className="ap-card" style={{ marginBottom: "1.5rem", border: "1.5px solid rgba(200,160,76,0.3)" }}>
          <h3 style={{ fontSize: "1rem", fontWeight: 700, color: "#1b3560", marginBottom: "1.25rem" }}>
            ایجاد کتابخانه جدید
          </h3>
          <form onSubmit={handleCreate}>
            <div className="ap-form-grid">
              <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                <label>نام کتابخانه *</label>
                <input name="name" value={form.name} onChange={handleFormChange} required placeholder="مثلاً: کتابخانه مرکزی" />
              </div>
              <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                <label>توضیحات</label>
                <textarea name="description" value={form.description} onChange={handleFormChange} rows={2} placeholder="توضیح مختصری..." />
              </div>
              <div className="ap-form-group">
                <label>مدت امانت پیش‌فرض (روز)</label>
                <input name="defaultBorrowDurationDays" type="number" min="1" value={form.defaultBorrowDurationDays} onChange={handleFormChange} />
              </div>
              <div className="ap-form-group" style={{ justifyContent: "center" }}>
                <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
                  <input type="checkbox" name="autoMembershipApproval" checked={form.autoMembershipApproval} onChange={handleFormChange} />
                  تأیید خودکار عضویت
                </label>
              </div>
            </div>
            <div className="ap-modal-actions">
              <button className="btn btn-primary" type="submit" disabled={creating}>
                {creating ? "در حال ایجاد..." : "ایجاد کتابخانه"}
              </button>
              <button className="btn btn-outline" type="button" onClick={() => setShowCreateForm(false)}>
                انصراف
              </button>
            </div>
          </form>
        </div>
      )}

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : libraries.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">🏛️</span>
          <p>هیچ کتابخانه‌ای ثبت نشده است</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="modern-table">
            <thead>
              <tr>
                <th>#</th>
                <th>نام کتابخانه</th>
                <th>مالک</th>
                <th>مدت امانت</th>
                <th>عضویت خودکار</th>
                <th>وضعیت</th>
                <th>عملیات</th>
              </tr>
            </thead>
            <tbody>
              {libraries.map((lib) => (
                <tr key={lib.id}>
                  <td style={{ fontSize: "0.75rem", color: "#9ca3af" }}>{lib.id}</td>
                  <td>
                    <div style={{ fontWeight: 600, color: "#1b3560" }}>{lib.name}</div>
                    {lib.description && (
                      <div style={{ fontSize: "0.75rem", color: "#9ca3af" }}>{lib.description}</div>
                    )}
                  </td>
                  <td style={{ fontSize: "0.82rem" }}>{lib.ownerName || "—"}</td>
                  <td style={{ fontSize: "0.82rem" }}>{lib.defaultBorrowDurationDays} روز</td>
                  <td>
                    <span className={`badge ${lib.autoMembershipApproval ? "badge-success" : "badge-muted"}`}>
                      {lib.autoMembershipApproval ? "فعال" : "غیرفعال"}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${lib.isActive ? "badge-success" : "badge-danger"}`}>
                      {lib.isActive ? "فعال" : "غیرفعال"}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                      <button
                        className="btn btn-sm btn-outline"
                        style={{ fontSize: "0.78rem" }}
                        onClick={() => openEditModal(lib)}
                      >
                        ✏️ ویرایش
                      </button>
                      {lib.isActive && (
                        <button
                          className="btn btn-sm btn-outline"
                          style={{ color: "#dc2626", borderColor: "#dc2626", fontSize: "0.78rem" }}
                          onClick={() => handleToggleActive(lib)}
                          disabled={actionLoading[lib.id]}
                        >
                          {actionLoading[lib.id] ? "..." : "غیرفعال‌کردن"}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {/* Edit Library Modal */}
      {editTarget && (
        <div className="ap-modal-overlay" onClick={() => setEditTarget(null)}>
          <div className="ap-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="ap-modal-title">ویرایش کتابخانه</h2>
            <form onSubmit={handleSaveEdit}>
              <div className="ap-form-grid">
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label>نام کتابخانه *</label>
                  <input name="name" value={editForm.name} onChange={handleEditFormChange} required />
                </div>
                <div className="ap-form-group" style={{ gridColumn: "1 / -1" }}>
                  <label>توضیحات</label>
                  <textarea name="description" value={editForm.description} onChange={handleEditFormChange} rows={2} />
                </div>
                <div className="ap-form-group">
                  <label>مدت امانت پیش‌فرض (روز)</label>
                  <input
                    name="defaultBorrowDurationDays"
                    type="number"
                    min="1"
                    value={editForm.defaultBorrowDurationDays}
                    onChange={handleEditFormChange}
                  />
                </div>
                <div className="ap-form-group" style={{ justifyContent: "center" }}>
                  <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
                    <input
                      type="checkbox"
                      name="autoMembershipApproval"
                      checked={editForm.autoMembershipApproval}
                      onChange={handleEditFormChange}
                    />
                    تأیید خودکار عضویت
                  </label>
                </div>
              </div>
              <div className="ap-modal-actions">
                <button className="btn btn-primary" type="submit" disabled={editSaving}>
                  {editSaving ? "در حال ذخیره..." : "ذخیره تغییرات"}
                </button>
                <button className="btn btn-outline" type="button" onClick={() => setEditTarget(null)}>
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

export default SystemLibrariesPage;
