import React from "react";
import { classificationBadge, classificationLabel } from "../utils/classification";

/** Shows a book's security label. Unclassified data is unlabeled unless `showUnclassified`. */
const ClassificationBadge = ({ level, showUnclassified = false }) => {
  if (!level || (level === "UNCLASSIFIED" && !showUnclassified)) return null;
  return (
    <span className={`badge ${classificationBadge(level)}`} title="سطح طبقه‌بندی">
      🔒 {classificationLabel(level)}
    </span>
  );
};

export default ClassificationBadge;
