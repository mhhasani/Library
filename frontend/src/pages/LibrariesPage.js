import React, { useEffect, useState, useCallback } from "react";
import { libraryAPI } from "../services/api";
import { useAuth } from "../context/AuthContext";
import "./LibrariesPage.css";

const LibrariesPage = () => {
  const { isAuthenticated, user } = useAuth();
  const [publicLibraries, setPublicLibraries] = useState([]);
  const [userLibraries, setUserLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    autoMembershipApproval: false,
    defaultBorrowDurationDays: 14,
  });
  const [activeLibraryId, setActiveLibraryId] = useState(
    localStorage.getItem("activeLibraryId") || "",
  );
  const [activeLibraryName, setActiveLibraryName] = useState(
    localStorage.getItem("activeLibraryName") || "",
  );

  const fetchLibraries = useCallback(async () => {
    try {
      setLoading(true);
      const publicRes = await libraryAPI.getPublicLibraries();
      setPublicLibraries(publicRes.data.data || publicRes.data);

      if (isAuthenticated) {
        const userRes = await libraryAPI.getLibraries();
        setUserLibraries(userRes.data.data || userRes.data);
      }

      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load libraries");
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchLibraries();
  }, [fetchLibraries]);

  const handleRequestMembership = async (libraryId) => {
    try {
      await libraryAPI.requestMembership(libraryId);
      setSuccessMessage("Membership request submitted successfully");
    } catch (err) {
      setError(err.response?.data?.message || "Failed to request membership");
    }
  };

  const handleSetActiveLibrary = (library) => {
    localStorage.setItem("activeLibraryId", library.id);
    localStorage.setItem("activeLibraryName", library.name);
    setActiveLibraryId(String(library.id));
    setActiveLibraryName(library.name);
    setSuccessMessage(`Active library set to: ${library.name}`);
  };

  const getUserLibraryStatus = (libraryId) => {
    const membership = userLibraries.find((lib) => lib.id === libraryId);
    if (!membership) return null;
    return {
      role: membership.userRole,
      status: membership.userStatus,
    };
  };

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleCreateLibrary = async (e) => {
    e.preventDefault();
    try {
      setError("");
      setSuccessMessage("");
      await libraryAPI.createLibrary({
        name: formData.name,
        description: formData.description || undefined,
        autoMembershipApproval: formData.autoMembershipApproval,
        defaultBorrowDurationDays: Number(formData.defaultBorrowDurationDays),
      });
      setSuccessMessage("Library created successfully");
      setFormData({
        name: "",
        description: "",
        autoMembershipApproval: false,
        defaultBorrowDurationDays: 14,
      });
      fetchLibraries();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to create library");
    }
  };

  if (loading) return <div className="loading">Loading libraries...</div>;

  return (
    <div className="libraries-container">
      <h2>Libraries</h2>

      {error && <div className="error-message">{error}</div>}
      {successMessage && (
        <div className="success-message">{successMessage}</div>
      )}

      {activeLibraryId && (
        <div className="active-library">
          Active Library: <strong>{activeLibraryName}</strong>
        </div>
      )}

      {isAuthenticated && user?.systemRole === "SYSTEM_ADMIN" && (
        <section className="libraries-section">
          <h3>Create Library</h3>
          <form className="library-form" onSubmit={handleCreateLibrary}>
            <div className="form-group">
              <label htmlFor="name">Library Name</label>
              <input
                id="name"
                name="name"
                type="text"
                value={formData.name}
                onChange={handleFormChange}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="description">Description</label>
              <textarea
                id="description"
                name="description"
                rows="3"
                value={formData.description}
                onChange={handleFormChange}
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="defaultBorrowDurationDays">
                  Default Borrow Days
                </label>
                <input
                  id="defaultBorrowDurationDays"
                  name="defaultBorrowDurationDays"
                  type="number"
                  min="1"
                  value={formData.defaultBorrowDurationDays}
                  onChange={handleFormChange}
                />
              </div>
              <div className="form-group checkbox-group">
                <label>
                  <input
                    type="checkbox"
                    name="autoMembershipApproval"
                    checked={formData.autoMembershipApproval}
                    onChange={handleFormChange}
                  />
                  Auto-approve memberships
                </label>
              </div>
            </div>
            <button className="primary-btn" type="submit">
              Create Library
            </button>
          </form>
        </section>
      )}

      {isAuthenticated && (
        <section className="libraries-section">
          <h3>Your Libraries</h3>
          {userLibraries.length === 0 ? (
            <p className="no-libraries">
              You are not a member of any libraries yet.
            </p>
          ) : (
            <div className="libraries-grid">
              {userLibraries.map((library) => (
                <div key={library.id} className="library-card">
                  <h4>{library.name}</h4>
                  <p>{library.description}</p>
                  <div className="library-meta">
                    <span>Role: {library.userRole || "Member"}</span>
                    <span>Status: {library.userStatus || "N/A"}</span>
                  </div>
                  <button
                    className="primary-btn"
                    onClick={() => handleSetActiveLibrary(library)}
                  >
                    Set Active
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      <section className="libraries-section">
        <h3>Public Libraries</h3>
        {publicLibraries.length === 0 ? (
          <p className="no-libraries">No public libraries available.</p>
        ) : (
          <div className="libraries-grid">
            {publicLibraries.map((library) => {
              const membershipInfo = getUserLibraryStatus(library.id);
              const isMember = !!membershipInfo;
              const isPending = membershipInfo?.status === "PENDING";
              return (
                <div key={library.id} className="library-card">
                  <h4>{library.name}</h4>
                  <p>{library.description}</p>
                  <div className="library-meta">
                    <span>Owner: {library.ownerName}</span>
                  </div>
                  {isAuthenticated ? (
                    isMember ? (
                      <p className="login-note">
                        {isPending
                          ? "Membership request pending"
                          : "Already a member"}
                      </p>
                    ) : (
                      <button
                        className="secondary-btn"
                        onClick={() => handleRequestMembership(library.id)}
                      >
                        Request Membership
                      </button>
                    )
                  ) : (
                    <p className="login-note">Login to request membership</p>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};

export default LibrariesPage;
