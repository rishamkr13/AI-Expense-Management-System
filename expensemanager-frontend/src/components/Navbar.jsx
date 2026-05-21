import { NavLink, useNavigate } from "react-router-dom";
import { useState } from "react";
import "./Navbar.css";
import Loading from "./Loading";

function Navbar() {
  const navigate = useNavigate();
  const [loggingOut, setLoggingOut] = useState(false);

  const handleLogout = () => {
    setLoggingOut(true);

    setTimeout(() => {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      navigate("/login", { replace: true });
    }, 700);
  };

  if (loggingOut) {
    return <Loading text="Logging out securely..." />;
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="logo-icon">₹</div>
        <div>
          <h2>AI Expense</h2>
          <p>Smart Manager</p>
        </div>
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/dashboard" className={({ isActive }) => isActive ? "nav-item active" : "nav-item"}>
          <span>📊</span>
          Dashboard
        </NavLink>

        <NavLink to="/incomes" className={({ isActive }) => isActive ? "nav-item active" : "nav-item"}>
          <span>💰</span>
          Incomes
        </NavLink>

        <NavLink to="/expenses" className={({ isActive }) => isActive ? "nav-item active" : "nav-item"}>
          <span>💸</span>
          Expenses
        </NavLink>

        <NavLink to="/budgets" className={({ isActive }) => isActive ? "nav-item active" : "nav-item"}>
          <span>🎯</span>
          Budgets
        </NavLink>

        <NavLink to="/receipts" className={({ isActive }) => isActive ? "nav-item active" : "nav-item"}>
          <span>🧾</span>
          Receipts
        </NavLink>

        <NavLink to="/analytics" className={({ isActive }) => isActive ? "nav-item active" : "nav-item"}>
          <span>📈</span>
          Analytics
        </NavLink>

        <NavLink to="/chatbot" className={({ isActive }) => isActive ? "nav-item active" : "nav-item"}>
          <span>🤖</span>
          AI Chatbot
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <button className="logout-btn" onClick={handleLogout}>
          <span>🚪</span>
          Logout
        </button>
      </div>
    </aside>
  );
}

export default Navbar;