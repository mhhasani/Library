import React, { useState } from "react";
import { adminAPI } from "../services/api";

/**
 * Lets the super admin assign a temporary password. The identity provider checks it against
 * the password policy; the user must replace it at the next login.
 */
const TemporaryPasswordModal = ({ user, onClose, onDone }) => {
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (password !== confirm) {
      setError("رمز عبور و تکرار آن یکسان نیستند");
      return;
    }
    try {
      setSaving(true);
      setError("");
      await adminAPI.assignTemporaryPassword(user.id, password);
      onDone(`رمز عبور موقت برای ${user.email} تنظیم شد؛ کاربر در ورود بعدی باید آن را تغییر دهد`);
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || "خطا در تنظیم رمز عبور");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="ap-modal-overlay" onClick={onClose}>
      <form className="ap-modal" style={{ maxWidth: 440 }} onClick={(e) => e.stopPropagation()} onSubmit={submit}>
        <h2 className="ap-modal-title">تنظیم رمز عبور موقت</h2>
        <p className="ap-subtitle" style={{ marginBottom: "1rem" }}>
          کاربر: <strong dir="ltr">{user.email}</strong> — نشست‌های باز کاربر بسته می‌شود.
        </p>
        {error && <div className="error-message">{error}</div>}
        <div className="ap-form-group">
          <label htmlFor="temp-password">رمز عبور موقت</label>
          <input id="temp-password" type="password" autoComplete="new-password" value={password}
                 onChange={(e) => setPassword(e.target.value)} minLength={8} maxLength={128} required />
        </div>
        <div className="ap-form-group">
          <label htmlFor="temp-password-confirm">تکرار رمز عبور</label>
          <input id="temp-password-confirm" type="password" autoComplete="new-password" value={confirm}
                 onChange={(e) => setConfirm(e.target.value)} minLength={8} maxLength={128} required />
        </div>
        <div className="ap-modal-actions">
          <button className="btn btn-primary" type="submit" disabled={saving}>
            {saving ? "در حال تنظیم..." : "تنظیم رمز موقت"}
          </button>
          <button className="btn btn-outline" type="button" onClick={onClose}>انصراف</button>
        </div>
      </form>
    </div>
  );
};

export default TemporaryPasswordModal;
