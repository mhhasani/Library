import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./HomePage.css";

const HomePage = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="home-container">
      <div className="home-hero">
        <h1>📚 Library Management System</h1>
        <p>Your digital gateway to knowledge and learning</p>
        <div className="home-actions">
          <Link to="/libraries" className="btn btn-primary">
            Browse Libraries
          </Link>
          {!isAuthenticated ? (
            <Link to="/register" className="btn btn-secondary">
              Register
            </Link>
          ) : (
            <Link to="/books" className="btn btn-secondary">
              Browse Books
            </Link>
          )}
        </div>
      </div>
      <div className="home-features">
        <div className="feature">
          <h3>📖 Browse Books</h3>
          <p>Access thousands of books from our extensive collection</p>
        </div>
        <div className="feature">
          <h3>📤 Borrow Books</h3>
          <p>Borrow books and keep track of your borrowed items</p>
        </div>
        <div className="feature">
          <h3>👤 Manage Profile</h3>
          <p>Manage your account and borrow history</p>
        </div>
      </div>
    </div>
  );
};

export default HomePage;
