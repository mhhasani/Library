import React, { useState, useEffect, useRef, useCallback } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { notificationAPI } from "../services/api";
import { toPersianNum } from "../utils/persian";
import "./NotificationBell.css";

const TYPE_ICON = {
  NEW_PHYSICAL_REQUEST: "📥",
  PHYSICAL_APPROVED: "✅",
  PHYSICAL_REJECTED: "❌",
  BOOK_DISPATCHED: "🚚",
  RECEIPT_CONFIRMED: "📦",
  RETURN_DUE: "⏰",
  RETURN_CONFIRMED: "🔄",
  BORROW_CANCELLED_BY_USER: "🚫",
  BORROW_CANCELLED_BY_LIBRARIAN: "🚫",
  RETURN_REQUESTED: "↩️",
  RETURN_PICKUP_SCHEDULED: "🗓️",
  BOOK_HANDED_OVER: "📤",
};

const DW = 340; // dropdown width

const timeAgo = (iso) => {
  if (!iso) return "";
  const diff = (Date.now() - new Date(iso).getTime()) / 1000;
  if (diff < 60) return "هم‌اکنون";
  if (diff < 3600) return `${toPersianNum(Math.floor(diff / 60))} دقیقه پیش`;
  if (diff < 86400) return `${toPersianNum(Math.floor(diff / 3600))} ساعت پیش`;
  return new Date(iso).toLocaleDateString("fa-IR");
};

const NotificationBell = ({ align = "start" }) => {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const btnRef = useRef(null);
  const dropRef = useRef(null);
  const listRef = useRef(null);
  const PAGE_SIZE = 15;

  const loadCount = useCallback(async () => {
    try {
      const res = await notificationAPI.unreadCount();
      const data = res.data?.data || res.data;
      setUnread(data?.count ?? 0);
    } catch { /* ignore */ }
  }, []);

  // Load the first page (called when the dropdown opens)
  const loadList = useCallback(async () => {
    try {
      const res = await notificationAPI.listPaged({ page: 0, size: PAGE_SIZE });
      const data = res.data?.data || res.data;
      setItems(data?.content || []);
      setPage(0);
      setHasMore(!data?.last && (data?.totalPages ?? 0) > 1);
    } catch { /* ignore */ }
  }, []);

  // Load the next page (called on scroll-to-bottom)
  const loadMore = useCallback(async () => {
    if (loadingMore || !hasMore) return;
    try {
      setLoadingMore(true);
      const next = page + 1;
      const res = await notificationAPI.listPaged({ page: next, size: PAGE_SIZE });
      const data = res.data?.data || res.data;
      setItems((prev) => [...prev, ...(data?.content || [])]);
      setPage(next);
      setHasMore(!data?.last);
    } catch { /* ignore */ } finally {
      setLoadingMore(false);
    }
  }, [page, hasMore, loadingMore]);

  const onListScroll = useCallback((e) => {
    const el = e.currentTarget;
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 40) loadMore();
  }, [loadMore]);

  const reposition = useCallback(() => {
    const el = btnRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const top = rect.bottom + 8;
    let left = align === "end" ? rect.right - DW : rect.left;
    left = Math.max(8, Math.min(left, window.innerWidth - DW - 8));
    setPos({ top, left });
  }, [align]);

  useEffect(() => {
    loadCount();
    const id = setInterval(loadCount, 30000);
    return () => clearInterval(id);
  }, [loadCount]);

  // Reposition while open
  useEffect(() => {
    if (!open) return;
    reposition();
    const onMove = () => reposition();
    window.addEventListener("scroll", onMove, true);
    window.addEventListener("resize", onMove);
    return () => {
      window.removeEventListener("scroll", onMove, true);
      window.removeEventListener("resize", onMove);
    };
  }, [open, reposition]);

  // Close on outside click (button or portal dropdown)
  useEffect(() => {
    const onClick = (e) => {
      if (btnRef.current?.contains(e.target)) return;
      if (dropRef.current?.contains(e.target)) return;
      setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) { reposition(); loadList(); }
  };

  const handleItemClick = async (n) => {
    try {
      if (!n.read) {
        await notificationAPI.markRead(n.id);
        setUnread((c) => Math.max(0, c - 1));
        setItems((list) => list.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      }
    } catch { /* ignore */ }
    setOpen(false);
    if (n.link) navigate(n.link);
  };

  const handleMarkAll = async () => {
    try {
      await notificationAPI.markAllRead();
      setUnread(0);
      setItems((list) => list.map((x) => ({ ...x, read: true })));
    } catch { /* ignore */ }
  };

  return (
    <div className="nb-wrap">
      <button className="nb-btn" ref={btnRef} onClick={toggle} aria-label="اعلان‌ها">
        🔔
        {unread > 0 && <span className="nb-badge">{toPersianNum(unread > 99 ? "99+" : unread)}</span>}
      </button>

      {open && createPortal(
        <div
          className="nb-dropdown"
          ref={dropRef}
          style={{ position: "fixed", top: pos.top, left: pos.left, width: DW }}
        >
          <div className="nb-head">
            <span>اعلان‌ها</span>
            {items.some((n) => !n.read) && (
              <button className="nb-markall" onClick={handleMarkAll}>خواندن همه</button>
            )}
          </div>
          <div className="nb-list" ref={listRef} onScroll={onListScroll}>
            {items.length === 0 ? (
              <div className="nb-empty">اعلانی ندارید</div>
            ) : (
              items.map((n) => (
                <button
                  key={n.id}
                  className={`nb-item ${n.read ? "" : "nb-item--unread"}`}
                  onClick={() => handleItemClick(n)}
                >
                  <span className="nb-item-icon">{TYPE_ICON[n.type] || "🔔"}</span>
                  <span className="nb-item-body">
                    <span className="nb-item-title">{n.title}</span>
                    {n.message && <span className="nb-item-msg">{n.message}</span>}
                    <span className="nb-item-time">{timeAgo(n.createdAt)}</span>
                  </span>
                  {!n.read && <span className="nb-dot" />}
                </button>
              ))
            )}
            {loadingMore && <div className="nb-loading">در حال بارگذاری...</div>}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
};

export default NotificationBell;
