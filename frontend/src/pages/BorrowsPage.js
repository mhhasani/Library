import React, { useState, useEffect, useCallback } from "react";
import { borrowAPI } from "../services/api";
import "./BorrowsPage.css";

const BorrowsPage = () => {
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("all"); // all, active, returned
  const [libraryId] = useState(localStorage.getItem("activeLibraryId") || "");
  const [libraryName] = useState(
    localStorage.getItem("activeLibraryName") || "",
  );

  const fetchBorrows = useCallback(async () => {
    try {
      setLoading(true);
      if (!libraryId) {
        setBorrows([]);
        setError("Select a library first to view borrows.");
        return;
      }
      const response = await borrowAPI.getBorrows(libraryId, {
        status: filter === "all" ? undefined : filter,
      });
      setBorrows(response.data.data || response.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load borrows");
    } finally {
      setLoading(false);
    }
  }, [filter, libraryId]);

  useEffect(() => {
    fetchBorrows();
  }, [fetchBorrows]);

  const handleReturnBook = async (borrowId) => {
    try {
      if (!libraryId) {
        setError("Select a library first.");
        return;
      }
      await borrowAPI.returnBorrow(libraryId, borrowId);
      fetchBorrows();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to return book");
    }
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      ACTIVE: "active",
      RETURNED: "returned",
      OVERDUE: "overdue",
      APPROVED: "active",
      PENDING: "pending",
      REJECTED: "rejected",
    };
    return statusMap[status] || status.toLowerCase();
  };

  const isOverdue = (dueDate) => {
    return new Date(dueDate) < new Date();
  };

  if (loading) return <div className="loading">Loading your borrows...</div>;

  return (
    <div className="borrows-container">
      <h2>My Borrow History</h2>
      {libraryName && (
        <div className="active-library">Active Library: {libraryName}</div>
      )}
      {!libraryId && (
        <div className="error-message">
          Please select a library from the Libraries page.
        </div>
      )}

      <div className="filter-tabs">
        <button
          className={`tab ${filter === "all" ? "active" : ""}`}
          onClick={() => setFilter("all")}
        >
          All
        </button>
        <button
          className={`tab ${filter === "ACTIVE" ? "active" : ""}`}
          onClick={() => setFilter("ACTIVE")}
        >
          Active
        </button>
        <button
          className={`tab ${filter === "RETURNED" ? "active" : ""}`}
          onClick={() => setFilter("RETURNED")}
        >
          Returned
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {borrows.length === 0 ? (
        <p className="no-borrows">No borrow records found</p>
      ) : (
        <div className="borrows-table">
          <table>
            <thead>
              <tr>
                <th>Book Title</th>
                <th>Book ID</th>
                <th>Borrowed Date</th>
                <th>Due Date</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {borrows.map((borrow) => (
                <tr key={borrow.id}>
                  <td>{borrow.bookTitle || "—"}</td>
                  <td>{borrow.bookId || "—"}</td>
                  <td>{new Date(borrow.borrowDate).toLocaleDateString()}</td>
                  <td
                    className={
                      borrow.isOverdue ||
                      (borrow.status === "ACTIVE" && isOverdue(borrow.dueDate))
                        ? "overdue"
                        : ""
                    }
                  >
                    {new Date(borrow.dueDate).toLocaleDateString()}
                  </td>
                  <td>
                    <span
                      className={`badge badge-${getStatusBadge(borrow.status)}`}
                    >
                      {borrow.status}
                    </span>
                  </td>
                  <td>
                    {borrow.status === "ACTIVE" ? (
                      <button
                        className="return-btn"
                        onClick={() => handleReturnBook(borrow.id)}
                      >
                        Return
                      </button>
                    ) : (
                      <span className="action-none">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default BorrowsPage;
