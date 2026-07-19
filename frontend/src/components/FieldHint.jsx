import React, { useEffect, useRef, useState } from "react";
import { FIELD_HINTS } from "../utils/fieldHints";
import "./FieldHint.css";

/**
 * A small "!" button next to a field label that reveals a plain-language
 * explanation of what the field means, on click. Used on both the borrower's
 * own borrow card and the admin's borrow-management panel so the same jargon
 * (e.g. "موعد برگرداندن", "تاریخ تحویل") is explained identically everywhere.
 */
const FieldHint = ({ hintKey, text }) => {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  const explanation = text || FIELD_HINTS[hintKey];

  useEffect(() => {
    if (!open) return;
    const onDocClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    const onEscape = (e) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDocClick);
    document.addEventListener("keydown", onEscape);
    return () => {
      document.removeEventListener("mousedown", onDocClick);
      document.removeEventListener("keydown", onEscape);
    };
  }, [open]);

  if (!explanation) return null;

  return (
    <span className="field-hint" ref={ref}>
      <button
        type="button"
        className="field-hint-btn"
        aria-label="توضیح این مورد چیست"
        aria-expanded={open}
        onClick={(e) => { e.stopPropagation(); setOpen((o) => !o); }}
      >
        !
      </button>
      {open && (
        <span className="field-hint-popover" role="tooltip">
          {explanation}
        </span>
      )}
    </span>
  );
};

export default FieldHint;
