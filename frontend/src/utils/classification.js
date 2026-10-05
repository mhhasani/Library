/** Security classification levels, in ascending order of sensitivity (mirrors the backend enum). */
export const CLASSIFICATION_LEVELS = [
  { value: "UNCLASSIFIED", label: "فاقد طبقه‌بندی", badge: "badge-info" },
  { value: "CONFIDENTIAL", label: "محرمانه", badge: "badge-warning" },
  { value: "HIGHLY_CONFIDENTIAL", label: "خیلی محرمانه", badge: "badge-warning" },
  { value: "SECRET", label: "سری", badge: "badge-danger" },
  { value: "TOP_SECRET", label: "به‌کلی سری", badge: "badge-danger" },
];

const BY_VALUE = Object.fromEntries(CLASSIFICATION_LEVELS.map((l) => [l.value, l]));

export const classificationLabel = (value) => BY_VALUE[value]?.label || "—";
export const classificationBadge = (value) => BY_VALUE[value]?.badge || "badge-info";
