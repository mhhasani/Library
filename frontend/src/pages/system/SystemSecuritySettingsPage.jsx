import React, { useEffect, useState } from "react";
import { securitySettingsAPI } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import { toPersian } from "../../utils/persian";
import "./SystemSecuritySettingsPage.css";

/** Numeric settings with the allowed ranges (also enforced by the server and the database). */
const NUMERIC_FIELDS = [
  { key: "maxFailedLogins", label: "تعداد تلاش ناموفق مجاز پیش از قفل حساب", min: 1, max: 6, unit: "بار" },
  { key: "lockoutMinutes", label: "مدت قفل موقت حساب", min: 1, max: 1440, unit: "دقیقه" },
  { key: "failureResetMinutes", label: "بازه‌ی شمارش تلاش‌های ناموفق", min: 1, max: 1440, unit: "دقیقه" },
  { key: "sessionIdleMinutes", label: "پایان نشست پس از عدم فعالیت", min: 15, max: 30, unit: "دقیقه" },
  { key: "passwordHistory", label: "تعداد رمزهای قبلی غیرقابل‌تکرار", min: 1, max: 3, unit: "رمز" },
  { key: "passwordMaxAgeDays", label: "اعتبار رمز عبور", min: 1, max: 365, unit: "روز" },
  { key: "reauthWindowMinutes", label: "اعتبار احراز هویت برای عملیات حساس", min: 1, max: 60, unit: "دقیقه" },
];

const SystemSecuritySettingsPage = () => {
  const { user } = useAuth();
  const canEdit = user?.systemRole === "SUPER_ADMIN";
  const [form, setForm] = useState(null);
  const [operations, setOperations] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([securitySettingsAPI.get(), securitySettingsAPI.sensitiveOperations()])
      .then(([settingsRes, opsRes]) => {
        const data = settingsRes.data?.data;
        setForm({ ...data, sensitiveOperations: data?.sensitiveOperations || [] });
        setOperations(opsRes.data?.data || []);
      })
      .catch(() => setError("خطا در بارگذاری تنظیمات امنیتی"));
  }, []);

  const setNumber = (key, value) => setForm((f) => ({ ...f, [key]: value === "" ? "" : Number(value) }));

  const toggleOperation = (op) =>
    setForm((f) => ({
      ...f,
      sensitiveOperations: f.sensitiveOperations.includes(op)
        ? f.sensitiveOperations.filter((o) => o !== op)
        : [...f.sensitiveOperations, op],
    }));

  const outOfRange = form
    ? NUMERIC_FIELDS.find((f) => !(form[f.key] >= f.min && form[f.key] <= f.max))
    : null;

  const handleSave = async (e) => {
    e.preventDefault();
    if (outOfRange) {
      setError(`«${outOfRange.label}» باید بین ${toPersian(outOfRange.min)} و ${toPersian(outOfRange.max)} باشد`);
      return;
    }
    try {
      setSaving(true);
      setError("");
      const res = await securitySettingsAPI.update({
        maxFailedLogins: form.maxFailedLogins,
        lockoutMinutes: form.lockoutMinutes,
        failureResetMinutes: form.failureResetMinutes,
        sessionIdleMinutes: form.sessionIdleMinutes,
        passwordHistory: form.passwordHistory,
        passwordMaxAgeDays: form.passwordMaxAgeDays,
        mfaRequired: form.mfaRequired,
        reauthWindowMinutes: form.reauthWindowMinutes,
        sensitiveOperations: form.sensitiveOperations,
      });
      setForm({ ...res.data.data });
      setSuccess("تنظیمات ذخیره و در سامانه‌ی احراز هویت اعمال شد");
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || "خطا در ذخیره‌ی تنظیمات");
    } finally {
      setSaving(false);
    }
  };

  if (!form) {
    return error ? <div className="error-message">{error}</div> : <div className="loading">در حال بارگذاری...</div>;
  }

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">تنظیمات امنیتی</h1>
        <p className="ap-subtitle">
          قفل حساب، نشست، رمز عبور، ورود دومرحله‌ای و عملیات نیازمند احراز هویت مجدد
          {!canEdit && " — فقط مدیر اصلی می‌تواند این تنظیمات را تغییر دهد"}
        </p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      <form className="ss-card" onSubmit={handleSave}>
        <fieldset disabled={!canEdit || saving} className="ss-fieldset">
          <div className="ss-grid">
            {NUMERIC_FIELDS.map((f) => (
              <label key={f.key} className="ss-field">
                <span className="ss-label">{f.label}</span>
                <span className="ss-input-row">
                  <input
                    type="number"
                    min={f.min}
                    max={f.max}
                    value={form[f.key]}
                    onChange={(e) => setNumber(f.key, e.target.value)}
                    required
                  />
                  <span className="ss-unit">{f.unit}</span>
                </span>
                <span className="ss-hint">
                  بازه‌ی مجاز: {toPersian(f.min)} تا {toPersian(f.max)}
                </span>
              </label>
            ))}
          </div>

          <label className="ss-toggle">
            <input
              type="checkbox"
              checked={form.mfaRequired}
              onChange={(e) => setForm((f) => ({ ...f, mfaRequired: e.target.checked }))}
            />
            <span>ورود دومرحله‌ای (رمز یک‌بار مصرف) برای همه‌ی کاربران اجباری باشد</span>
          </label>

          <h3 className="ss-section-title">عملیات حساس (نیازمند احراز هویت مجدد)</h3>
          <div className="ss-ops">
            {operations.map((op) => (
              <label key={op.value} className="ss-op">
                <input
                  type="checkbox"
                  checked={form.sensitiveOperations.includes(op.value)}
                  onChange={() => toggleOperation(op.value)}
                />
                <span>{op.label}</span>
              </label>
            ))}
          </div>

          {canEdit && (
            <div className="ap-modal-actions">
              <button className="btn btn-primary" type="submit" disabled={saving}>
                {saving ? "در حال ذخیره..." : "ذخیره‌ی تنظیمات"}
              </button>
            </div>
          )}
        </fieldset>
      </form>
    </div>
  );
};

export default SystemSecuritySettingsPage;
