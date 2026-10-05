import React, { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { toPersian } from "../utils/persian";
import "./SecurityNotice.css";

const formatTime = (iso) => (iso ? new Date(iso).toLocaleString("fa-IR") : "—");

/**
 * Post-login security notice. Until it is acknowledged the application's pages are not
 * rendered at all (and the server refuses data requests), so no data is shown or fetched.
 */
export const SecurityNoticeGate = ({ children }) => {
  const { noticeRequired } = useAuth();
  return noticeRequired ? <SecurityNotice /> : children;
};

const SecurityNotice = () => {
  const { session, acknowledgeNotice, logout } = useAuth();
  const [busy, setBusy] = useState(false);

  const accept = async () => {
    setBusy(true);
    try {
      await acknowledgeNotice();
    } finally {
      setBusy(false);
    }
  };

  const failed = session.failedLoginsSinceLastLogin;

  return (
    <div className="sn-overlay" role="dialog" aria-modal="true" aria-labelledby="sn-title">
      <div className="sn-dialog">
        <h2 id="sn-title" className="sn-title">🛡️ اطلاعیه‌ی امنیتی</h2>

        <ol className="sn-items">
          <li>
            شما با حساب <strong dir="ltr">{session.user?.email}</strong> وارد سامانه‌ی
            کتابخانه هوشمند <strong>{session.organizationName}</strong> شده‌اید.
          </li>
          <li>تمام فعالیت‌های شما در این سامانه ردگیری، ثبت و بازبینی می‌شود.</li>
          <li>
            مسئولیت حفاظت از اطلاعات حساس و طبقه‌بندی‌شده‌ای که به آن دسترسی پیدا می‌کنید بر عهده‌ی
            شماست و هرگونه افشا یا استفاده‌ی غیرمجاز پیگرد دارد.
          </li>
        </ol>

        <div className="sn-logins">
          <div className="sn-login-row">
            <span>آخرین ورود موفق قبلی:</span>
            <strong>
              {session.previousLoginAt ? formatTime(session.previousLoginAt) : "این نخستین ورود شماست"}
            </strong>
          </div>
          {session.previousLoginIp && (
            <div className="sn-login-row">
              <span>نشانی IP ورود قبلی:</span>
              <strong dir="ltr">{session.previousLoginIp}</strong>
            </div>
          )}
          <div className="sn-login-row">
            <span>تلاش‌های ناموفق ورود از آن زمان:</span>
            <strong className={failed > 0 ? "sn-warn" : ""}>
              {failed == null ? "نامشخص" : toPersian(failed)}
            </strong>
          </div>
          <div className="sn-login-row">
            <span>نشانی IP فعلی:</span>
            <strong dir="ltr">{session.currentLoginIp}</strong>
          </div>
          {failed > 0 && (
            <p className="sn-alert">
              اگر این تلاش‌های ناموفق از طرف شما نبوده، فوراً رمز عبور خود را تغییر دهید و به مدیر سامانه اطلاع دهید.
            </p>
          )}
        </div>

        <div className="sn-actions">
          <button className="btn btn-primary" onClick={accept} disabled={busy}>
            {busy ? "..." : "مطالعه کردم و می‌پذیرم"}
          </button>
          <button className="btn btn-outline" onClick={logout} disabled={busy}>
            خروج
          </button>
        </div>
      </div>
    </div>
  );
};

export default SecurityNotice;
