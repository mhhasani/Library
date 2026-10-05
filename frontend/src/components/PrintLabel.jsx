import React, { useEffect, useState } from "react";
import { outputLabelAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import { toPersian } from "../utils/persian";
import "./PrintLabel.css";

const now = () => new Date().toLocaleString("fa-IR");

/**
 * Output label for printed pages (hidden on screen): classification, user id, client IP
 * and print time, repeated at the top and bottom of every printed page.
 */
const PrintLabel = () => {
  const { isAuthenticated } = useAuth();
  const [label, setLabel] = useState(null);
  const [printedAt, setPrintedAt] = useState(now);

  useEffect(() => {
    if (!isAuthenticated) {
      setLabel(null);
      return;
    }
    outputLabelAPI.current()
      .then((res) => setLabel(res.data?.data || null))
      .catch(() => setLabel(null));
  }, [isAuthenticated]);

  useEffect(() => {
    const stamp = () => setPrintedAt(now());
    window.addEventListener("beforeprint", stamp);
    return () => window.removeEventListener("beforeprint", stamp);
  }, []);

  if (!label) return null;
  const text = `طبقه‌بندی: ${label.classificationLabel} | شناسه‌ی کاربر: ${toPersian(label.userId)} (${label.userEmail}) | IP: ${label.clientIp} | زمان: ${printedAt}`;

  return (
    <>
      <div className="print-label print-label--top" aria-hidden="true">{text}</div>
      <div className="print-label print-label--bottom" aria-hidden="true">{text}</div>
    </>
  );
};

export default PrintLabel;
