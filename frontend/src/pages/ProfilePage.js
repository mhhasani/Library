import React from "react";
import { useAuth } from "../context/AuthContext";
import "./ProfilePage.css";

const ProfilePage = () => {
  const { user } = useAuth();

  if (!user) {
    return <div className="loading">Loading profile...</div>;
  }

  return (
    <div className="profile-container">
      <div className="profile-card">
        <h2>User Profile</h2>
        <div className="profile-info">
          <div className="info-group">
            <label>Name</label>
            <p>{user.name}</p>
          </div>
          <div className="info-group">
            <label>Email</label>
            <p>{user.email}</p>
          </div>
          {user.role && (
            <div className="info-group">
              <label>Role</label>
              <p>{user.role}</p>
            </div>
          )}
          {user.createdAt && (
            <div className="info-group">
              <label>Member Since</label>
              <p>{new Date(user.createdAt).toLocaleDateString()}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
