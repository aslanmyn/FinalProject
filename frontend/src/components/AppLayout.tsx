import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { logout } from "../lib/api";
import { clearAuthSession, getUserPermissions, getUserRole } from "../lib/auth";
import { bindNotificationLiveSocket, loadNotificationCenterData } from "../lib/notifications";
import { disconnectStomp } from "../lib/ws";
import type { UserRole } from "../types/auth";

type NavItem = { to: string; label: string };
type NavGroup = { title: string; items: NavItem[] };

function getNav(role: string | null, permissions: string[]): NavGroup[] {
  if (role === "STUDENT") {
    return [
      {
        title: "Academic",
        items: [
          { to: "/app/student", label: "Overview" },
          { to: "/app/student/registration", label: "Registration" },
          { to: "/app/student/planner", label: "Planner & Risk" },
          { to: "/app/student/schedule", label: "Schedule" },
          { to: "/app/student/enrollments", label: "Enrollments" },
          { to: "/app/student/journal", label: "Journal" },
          { to: "/app/student/transcript", label: "Transcript" },
          { to: "/app/student/attendance", label: "Attendance" },
          { to: "/app/student/exams", label: "Exam Schedule" }
        ]
      },
      {
        title: "Services",
        items: [
          { to: "/app/student/assistant", label: "AI Assistant" },
          { to: "/app/student/workflows", label: "Workflows" },
          { to: "/app/student/notifications", label: "Notifications" },
          { to: "/app/chat", label: "Chat" },
          { to: "/app/student/requests", label: "Requests" },
          { to: "/app/student/financial", label: "Financial" },
          { to: "/app/student/files", label: "Files" },
          { to: "/app/student/news", label: "News" }
        ]
      }
    ];
  }
  if (role === "PROFESSOR") {
    return [
      {
        title: "Teaching",
        items: [
          { to: "/app/teacher", label: "Overview" },
          { to: "/app/teacher/sections", label: "Sections" },
          { to: "/app/teacher/risk", label: "Risk Dashboard" },
          { to: "/app/teacher/notifications", label: "Notifications" },
          { to: "/app/teacher/assistant", label: "AI Assistant" },
          { to: "/app/chat", label: "Chat" },
          { to: "/app/teacher/attendance", label: "Attendance" },
          { to: "/app/teacher/gradebook", label: "Gradebook" },
          { to: "/app/teacher/announcements", label: "Announcements" },
          { to: "/app/teacher/materials", label: "Materials" },
          { to: "/app/teacher/notes", label: "Student Notes" },
          { to: "/app/teacher/grade-changes", label: "Grade Changes" }
        ]
      }
    ];
  }
  if (role === "ADMIN") {
    const canAccessSuper = permissions.includes("SUPER");
    const canAccessRegistrar = canAccessSuper || permissions.includes("REGISTRAR");
    const canAccessFinance = canAccessSuper || permissions.includes("FINANCE");
    const canAccessSupport = canAccessSuper || permissions.includes("SUPPORT");
    const canAccessContent = canAccessSuper || permissions.includes("CONTENT");
    const adminItems: NavItem[] = [
      { to: "/app/admin", label: "Overview" },
      { to: "/app/admin/notifications", label: "Notifications" }
    ];

    if (canAccessSuper) {
      adminItems.push(
        { to: "/app/admin/analytics", label: "Analytics" },
        { to: "/app/admin/workflows", label: "Workflows" },
        { to: "/app/admin/assistant", label: "AI Assistant" },
        { to: "/app/admin/users", label: "Users" }
      );
    }
    if (canAccessRegistrar) {
      adminItems.push(
        { to: "/app/admin/registration", label: "Registration Ops" },
        { to: "/app/admin/academic", label: "Academic Setup" }
      );
    }
    if (canAccessFinance) {
      adminItems.push({ to: "/app/admin/finance", label: "Finance" });
    }
    if (canAccessContent) {
      adminItems.push({ to: "/app/admin/moderation", label: "Moderation & News" });
    }
    if (canAccessSupport) {
      adminItems.push({ to: "/app/admin/requests", label: "Requests" });
    }

    return [
      {
        title: "Administration",
        items: adminItems
      }
    ];
  }
  return [];
}

export default function AppLayout() {
  const navigate = useNavigate();
  const role = getUserRole();
  const permissions = getUserPermissions();
  const nav = getNav(role, permissions);
  const [unreadNotifications, setUnreadNotifications] = useState(0);

  useEffect(() => {
    if (!role) {
      setUnreadNotifications(0);
      return undefined;
    }

    let active = true;

    async function loadUnreadCount(currentRole: UserRole) {
      try {
        const data = await loadNotificationCenterData(currentRole);
        if (active) {
          setUnreadNotifications(data.unreadCount);
        }
      } catch {
        if (active) {
          setUnreadNotifications(0);
        }
      }
    }

    void loadUnreadCount(role);

    const unsubscribe = bindNotificationLiveSocket((payload) => {
      if (active) {
        setUnreadNotifications(payload.unreadCount);
      }
    });

    return () => {
      active = false;
      unsubscribe();
      disconnectStomp();
    };
  }, [role]);

  async function handleLogout() {
    try {
      await logout();
    } finally {
      clearAuthSession();
      navigate("/login", { replace: true });
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar-global">
        <div className="topbar-left">
          <span className="topbar-brand">KBTU</span>
          <span className="topbar-separator" />
          <span className="topbar-context">Portal</span>
        </div>
        <div className="topbar-right">
          <NavLink className="topbar-pub-link" to="/">Home</NavLink>
          <NavLink className="topbar-pub-link" to="/professors">Professors</NavLink>
          <NavLink className="topbar-pub-link" to="/news">News</NavLink>
          <span className="topbar-role-badge">{role}</span>
          <button className="topbar-logout" onClick={handleLogout}>Sign out</button>
        </div>
      </header>

      <div className="panel-layout">
        <aside className="sidebar">
          <nav className="sidebar-nav">
            {nav.map((group) => (
              <div key={group.title} className="sidebar-group">
                <div className="sidebar-group-title">{group.title}</div>
                {group.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) => `sidebar-link${isActive ? " active" : ""}`}
                  >
                    <span>{item.label}</span>
                    {item.to.endsWith("/notifications") && unreadNotifications > 0 ? (
                      <span className="sidebar-badge">
                        {unreadNotifications > 99 ? "99+" : unreadNotifications}
                      </span>
                    ) : null}
                  </NavLink>
                ))}
              </div>
            ))}
          </nav>
        </aside>

        <main className="panel-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
