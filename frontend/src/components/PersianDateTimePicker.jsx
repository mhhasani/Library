import React from "react";
import DatePicker from "react-multi-date-picker";
import persian from "react-date-object/calendars/persian";
import persian_fa from "react-date-object/locales/persian_fa";
import TimePicker from "react-multi-date-picker/plugins/time_picker";
import "react-multi-date-picker/styles/colors/teal.css";
import "./PersianDateTimePicker.css";

const pad = (n) => String(n).padStart(2, "0");

// DateObject (from react-multi-date-picker) -> "YYYY-MM-DDTHH:mm:ss" in local (gregorian) time
const toLocalIso = (dateObject) => {
  if (!dateObject) return "";
  const d = dateObject.toDate ? dateObject.toDate() : dateObject;
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
};

/**
 * Persian (Jalali) date+time picker. Controlled via an ISO-local string value.
 *  value:    "YYYY-MM-DDTHH:mm:ss" or "" (gregorian, local)
 *  onChange: (isoLocalString) => void
 */
const PersianDateTimePicker = ({ value, onChange, placeholder = "انتخاب تاریخ و ساعت", minDate }) => {
  return (
    <DatePicker
      calendar={persian}
      locale={persian_fa}
      calendarPosition="bottom-right"
      format="YYYY/MM/DD  HH:mm"
      plugins={[<TimePicker key="tp" position="bottom" hideSeconds />]}
      value={value ? new Date(value) : ""}
      onChange={(dateObject) => onChange(toLocalIso(dateObject))}
      minDate={minDate}
      inputClass="pdtp-input"
      containerClassName="pdtp-container"
      placeholder={placeholder}
      editable={false}
      hideOnScroll
    />
  );
};

export default PersianDateTimePicker;
