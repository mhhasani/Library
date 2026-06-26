import React from "react";
import "./ConfirmDialog.css";

/**
 * Reusable confirmation dialog. Controlled via `open`.
 *
 * <ConfirmDialog
 *   open={!!target}
 *   title="..." message="..."
 *   confirmLabel="تأیید" variant="danger" | "primary" | "success"
 *   loading={busy}
 *   onConfirm={...} onCancel={...}
 * />
 */
const ConfirmDialog = ({
  open,
  title = "تأیید عملیات",
  message,
  confirmLabel = "تأیید",
  cancelLabel = "انصراف",
  variant = "primary",
  loading = false,
  error = "",
  onConfirm,
  onCancel,
}) => {
  if (!open) return null;

  const btnClass =
    variant === "danger" ? "btn-danger" : variant === "success" ? "btn-success" : "btn-primary";

  return (
    <div className="cd-overlay" onClick={loading ? undefined : onCancel}>
      <div className="cd-box" onClick={(e) => e.stopPropagation()}>
        <h3 className="cd-title">{title}</h3>
        {message && <p className="cd-message">{message}</p>}
        {error && <div className="error-message" style={{ marginBottom: "0.75rem" }}>{error}</div>}
        <div className="cd-actions">
          <button className={`btn ${btnClass}`} onClick={onConfirm} disabled={loading}>
            {loading ? "در حال انجام..." : confirmLabel}
          </button>
          <button className="btn btn-outline" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDialog;
