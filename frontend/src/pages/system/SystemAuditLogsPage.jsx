import React, { useCallback, useEffect, useState } from "react";
import { auditAPI } from "../../services/api";
import { toPersian, toPersianNum } from "../../utils/persian";
import { saveBlob } from "../../utils/download";
import { useDebounce } from "../../hooks/useDebounce";
import Pagination from "../../components/Pagination";
import PersianDateTimePicker from "../../components/PersianDateTimePicker";
import "./SystemUsersPage.css";
import "./SystemAuditLogsPage.css";

const ACTION_LABELS = {
  APPLICATION_START: "شروع به کار سامانه",
  APPLICATION_STOP: "توقف سامانه",
  LOGIN: "ورود",
  LOGOUT: "خروج",
  SESSION_EXPIRED: "انقضای نشست",
  SESSION_TERMINATED: "خاتمه‌ی نشست",
  REAUTHENTICATION: "احراز هویت مجدد",
  AUTHENTICATION_REQUIRED: "دسترسی بدون احراز هویت",
  ACCESS_DENIED: "دسترسی غیرمجاز",
  CSRF_REJECTED: "درخواست جعلی (CSRF)",
  SESSION_BINDING_MISMATCH: "عدم تطابق نشست",
  USER_ROLE_CHANGE: "تغییر نقش کاربر",
  USER_STATUS_CHANGE: "تغییر وضعیت کاربر",
  USER_CLEARANCE_CHANGE: "تغییر سطح دسترسی طبقه‌بندی",
  LIBRARY_ROLE_CHANGE: "تغییر نقش در کتابخانه",
  MEMBERSHIP_DECISION: "تصمیم درباره‌ی عضویت",
  SECURITY_SETTINGS_CHANGE: "تغییر تنظیمات امنیتی",
  PASSWORD_RESET_BY_ADMIN: "تنظیم رمز توسط مدیر",
  API_WRITE: "تغییر داده",
  AUDIT_LOG_EXPORT: "خروجی رویدادنگاری",
  AUDIT_CHAIN_VERIFY: "بررسی صحت رویدادنگاری",
};

const OUTCOME_FILTERS = [
  { value: "", label: "همه" },
  { value: "SUCCESS", label: "موفق" },
  { value: "FAILURE", label: "ناموفق" },
];

const PAGE_SIZE = 20;

const formatTime = (iso) => {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("fa-IR", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
};

const SystemAuditLogsPage = () => {
  const [records, setRecords] = useState([]);
  const [actions, setActions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null);
  const [action, setAction] = useState("");
  const [outcome, setOutcome] = useState("");
  const [search, setSearch] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [busy, setBusy] = useState(false);
  const debouncedSearch = useDebounce(search, 400);

  const filterParams = useCallback(
    () => ({
      action: action || undefined,
      outcome: outcome || undefined,
      search: debouncedSearch || undefined,
      from: from || undefined,
      to: to || undefined,
    }),
    [action, outcome, debouncedSearch, from, to],
  );

  useEffect(() => {
    auditAPI.actions()
      .then((res) => setActions(res.data?.data || []))
      .catch(() => setActions(Object.keys(ACTION_LABELS)));
  }, []);

  const fetchRecords = useCallback(async () => {
    try {
      setLoading(true);
      const res = await auditAPI.search({ ...filterParams(), page, size: PAGE_SIZE });
      const d = res.data?.data;
      setRecords(d?.content || []);
      setTotalPages(d?.totalPages ?? 0);
      setTotalElements(d?.totalElements ?? 0);
      setError("");
    } catch {
      setError("خطا در بارگذاری رویدادها");
    } finally {
      setLoading(false);
    }
  }, [filterParams, page]);

  useEffect(() => { fetchRecords(); }, [fetchRecords]);
  useEffect(() => { setPage(0); }, [action, outcome, debouncedSearch, from, to]);

  const handleExport = async () => {
    try {
      setBusy(true);
      const res = await auditAPI.exportCsv(filterParams());
      saveBlob(res.data, "audit-log.csv");
    } catch {
      setError("خطا در تهیه‌ی خروجی");
    } finally {
      setBusy(false);
    }
  };

  const handleVerify = async () => {
    try {
      setBusy(true);
      const res = await auditAPI.verify();
      const r = res.data?.data;
      setNotice(r?.valid
        ? { ok: true, text: `صحت ${toPersianNum(r.recordsChecked)} رکورد رویدادنگاری تأیید شد؛ هیچ دست‌کاری‌ای یافت نشد.` }
        : { ok: false, text: `هشدار: زنجیره‌ی رویدادنگاری از رکورد شماره‌ی ${toPersian(r?.firstInvalidId)} به بعد دست‌کاری شده است.` });
      fetchRecords();
    } catch {
      setError("خطا در بررسی صحت رویدادنگاری");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="ap-header">
        <h1 className="ap-title">رویدادنگاری امنیتی</h1>
        <p className="ap-subtitle">
          گزارش ورودها، دسترسی‌های غیرمجاز، تغییرات دسترسی و فعالیت‌های حساس کاربران
        </p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {notice && (
        <div className={notice.ok ? "success-message" : "error-message"}>{notice.text}</div>
      )}

      <div className="ap-toolbar">
        <div className="su-filter-group">
          {OUTCOME_FILTERS.map((f) => (
            <button
              key={f.value}
              className={`su-filter-btn ${outcome === f.value ? "su-filter-btn--active" : ""}`}
              onClick={() => setOutcome(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
        <span className="su-count">{toPersianNum(totalElements)} رویداد</span>
      </div>

      <div className="al-filters">
        <select className="al-select" value={action} onChange={(e) => setAction(e.target.value)}>
          <option value="">همه‌ی رویدادها</option>
          {actions.map((a) => (
            <option key={a} value={a}>{ACTION_LABELS[a] || a}</option>
          ))}
        </select>
        <input
          className="abr-search al-search"
          placeholder="🔍 جستجو بر اساس ایمیل، IP یا جزئیات..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <div className="al-range">
          <PersianDateTimePicker value={from} onChange={setFrom} placeholder="از تاریخ" />
          <PersianDateTimePicker value={to} onChange={setTo} placeholder="تا تاریخ" />
        </div>
        <div className="al-actions">
          <button className="btn btn-outline" onClick={handleVerify} disabled={busy}>
            🛡️ بررسی صحت
          </button>
          <button className="btn btn-primary" onClick={handleExport} disabled={busy}>
            ⬇️ خروجی CSV
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading">در حال بارگذاری...</div>
      ) : records.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📜</span>
          <p>رویدادی یافت نشد</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="modern-table">
            <thead>
              <tr>
                <th>#</th>
                <th>زمان</th>
                <th>رویداد</th>
                <th>نتیجه</th>
                <th>کاربر</th>
                <th>IP</th>
                <th>جزئیات</th>
              </tr>
            </thead>
            <tbody>
              {records.map((r) => (
                <tr key={r.id}>
                  <td className="su-id">{toPersian(r.id)}</td>
                  <td className="al-time">{formatTime(r.timestamp)}</td>
                  <td>{ACTION_LABELS[r.action] || r.action}</td>
                  <td>
                    <span className={`badge ${r.outcome === "SUCCESS" ? "badge-success" : "badge-danger"}`}>
                      {r.outcome === "SUCCESS" ? "موفق" : "ناموفق"}
                    </span>
                  </td>
                  <td className="al-actor">{r.actorEmail || (r.actorId ? `#${toPersian(r.actorId)}` : "—")}</td>
                  <td className="al-ip" dir="ltr">{r.ipAddress || "—"}</td>
                  <td className="al-details" dir="ltr" title={r.userAgent || ""}>{r.details || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  );
};

export default SystemAuditLogsPage;
