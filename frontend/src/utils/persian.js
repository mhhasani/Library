/**
 * تبدیل اعداد لاتین به فارسی
 */
export const toPersian = (value) => {
  if (value === null || value === undefined) return "";
  const persianDigits = ["۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"];
  return String(value).replace(/\d/g, (d) => persianDigits[d]);
};

/**
 * فرمت‌بندی عدد با جداکننده‌ی هزارگان و تبدیل به فارسی
 */
export const toPersianNum = (value) => {
  if (value === null || value === undefined || value === "") return "";
  const num = Number(value);
  if (isNaN(num)) return toPersian(value);
  return toPersian(num.toLocaleString("fa-IR"));
};

/**
 * فرمت‌بندی تاریخ ISO به شمسی فارسی
 */
export const toJalali = (isoDate) => {
  if (!isoDate) return "—";
  try {
    return new Date(isoDate).toLocaleDateString("fa-IR", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  } catch {
    return isoDate;
  }
};

/**
 * فرمت‌بندی تاریخ ISO کوتاه
 */
export const toJalaliShort = (isoDate) => {
  if (!isoDate) return "—";
  try {
    return new Date(isoDate).toLocaleDateString("fa-IR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  } catch {
    return isoDate;
  }
};
