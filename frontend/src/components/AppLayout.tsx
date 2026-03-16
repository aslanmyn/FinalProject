import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { logout } from "../lib/api";
import { clearAuthSession, getUserRole } from "../lib/auth";

type NavItem = { to: string; label: string };
type NavGroup = { title: string; items: NavItem[] };

function getNav(role: string | null): NavGroup[] {
  if (role === "STUDENT") {
    return [
      {
        title: "Academic",
        items: [
          { to: "/app/student", label: "Overview" },
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
    return [
      {
        title: "Administration",
        items: [
          { to: "/app/admin", label: "Overview" },
          { to: "/app/admin/academic", label: "Academic Setup" },
          { to: "/app/admin/finance", label: "Finance" },
          { to: "/app/admin/moderation", label: "Moderation & News" },
          { to: "/app/admin/users", label: "Users" },
          { to: "/app/admin/requests", label: "Requests" }
        ]
      }
    ];
  }
  return [];
}

export default function AppLayout() {
  const navigate = useNavigate();
  const role = getUserRole();
  const nav = getNav(role);

  async function handleLogout() {
    try {
      await logout();
    } finally {
      clearAuthSession();
      navigate("/login", { replace: true });
    }
  }

  return (
    <div className="panel-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>KBTU Portal</h2>
          <p className="muted">Role: {role || "UNKNOWN"}</p>
        </div>

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
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-actions">
          <NavLink className="sidebar-link" to="/">
            Public Home
          </NavLink>
          <NavLink className="sidebar-link" to="/professors">
            Professors
          </NavLink>
          <NavLink className="sidebar-link" to="/news">
            Public News
          </NavLink>
          <button onClick={handleLogout}>Logout</button>
        </div>
      </aside>

      <main className="panel-content">
        <Outlet />
      </main>
    </div>
  );
}
