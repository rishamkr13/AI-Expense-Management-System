import { useState } from "react";
import Sidebar from "./Sidebar";
import "./Layout.css";

const Layout = ({ children }) => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className={`layout${sidebarCollapsed ? " layout--sidebar-collapsed" : ""}`}>
      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed((prev) => !prev)}
      />

      <main className="layout-main">{children}</main>
    </div>
  );
};

export default Layout;