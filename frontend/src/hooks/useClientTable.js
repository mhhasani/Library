import { useMemo, useState } from "react";

/**
 * Client-side search + pagination for bounded lists.
 *  items:  array of records
 *  fields: array of keys or (item)=>value selectors to search across
 *  pageSize: rows per page
 * Returns { query, setQuery, page, setPage, totalPages, pageItems, total }.
 */
export function useClientTable(items, fields, pageSize = 10) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return items;
    return items.filter((it) =>
      fields.some((f) => {
        const v = typeof f === "function" ? f(it) : it[f];
        return v != null && String(v).toLowerCase().includes(q);
      })
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items, query]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const pageItems = filtered.slice(safePage * pageSize, safePage * pageSize + pageSize);

  return { query, setQuery, page: safePage, setPage, totalPages, pageItems, total: filtered.length };
}
