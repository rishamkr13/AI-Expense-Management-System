import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
  LayoutDashboard,
  TrendingUp,
  TrendingDown,
  PiggyBank,
  BarChart2,
  Bot,
  Receipt,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
} from "lucide-react";
import "./Sidebar.css";

const navItems = [
  {
    path: "/dashboard",
    label: "Dashboard",
    icon: LayoutDashboard,
  },
  {
    path: "/incomes",
    label: "Incomes",
    icon: TrendingUp,
  },
  {
    path: "/expenses",
    label: "Expenses",
    icon: TrendingDown,
  },
  {
    path: "/budgets",
    label: "Budgets",
    icon: PiggyBank,
  },
  {
    path: "/analytics",
    label: "Analytics",
    icon: BarChart2,
  },
  {
    path: "/receipts",
    label: "Receipts",
    icon: Receipt,
  },
  {
    path: "/chatbot",
    label: "AI Chatbot",
    icon: Bot,
  },
];

const Sidebar = ({ collapsed, onToggle }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const initials = user?.name
    ? user.name
        .trim()
        .split(" ")
        .filter(Boolean)
        .map((name) => name[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : "U";

  return (
    <aside className={`sidebar ${collapsed ? "sidebar--collapsed" : ""}`}>
      {/* Logo */}
      <div className="sidebar-logo">
        <span className="sidebar-logo-icon">💰</span>

        <span className="sidebar-logo-text">ExpenseAI</span>

        <button
          type="button"
          className="sidebar-toggle"
          onClick={onToggle}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          title={collapsed ? "Expand" : "Collapse"}
        >
          {collapsed ? (
            <PanelLeftOpen size={18} strokeWidth={2.4} />
          ) : (
            <PanelLeftClose size={18} strokeWidth={2.4} />
          )}
        </button>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        {navItems.map((item) => {
          const Icon = item.icon;

          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? "sidebar-link--active" : ""}`
              }
              title={collapsed ? item.label : ""}
            >
              <span className="sidebar-link-icon">
                <Icon size={22} strokeWidth={2.2} />
              </span>

              <span className="sidebar-link-label">{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="sidebar-footer">
        <div className="sidebar-user" title={collapsed ? user?.name || "User" : ""}>
          <div className="sidebar-avatar">{initials}</div>

          <div className="sidebar-user-info">
            <p className="sidebar-user-name">{user?.name || "User"}</p>
            <p className="sidebar-user-email">{user?.email || ""}</p>
          </div>
        </div>

        <button className="sidebar-logout" onClick={handleLogout} title="Logout">
          <LogOut size={20} strokeWidth={2.2} />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;