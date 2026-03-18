import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchAdminStats } from "../../lib/api";
import { getUserPermissions } from "../../lib/auth";
import type { AdminStats } from "../../types/admin";

function hasPermission(permissions: string[], permission: string): boolean {
  return permissions.includes("SUPER") || permissions.includes(permission);
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const permissions = getUserPermissions();

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchAdminStats();
        if (!cancelled) {
          setStats(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load admin overview");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  const availableActions = useMemo(() => {
    const actions: Array<{ to: string; label: string; description: string }> = [
      {
        to: "/app/admin/notifications",
        label: "Notifications",
        description: "Track unread system alerts and approval updates."
      }
    ];

    if (hasPermission(permissions, "REGISTRAR")) {
      actions.push(
        {
          to: "/app/admin/registration",
          label: "Registration Ops",
          description: "Manage windows, FX queue, and registration blockers."
        },
        {
          to: "/app/admin/academic",
          label: "Academic Setup",
          description: "Terms, sections, meeting times, and faculty scheduling."
        }
      );
    }
    if (hasPermission(permissions, "FINANCE")) {
      actions.push({
        to: "/app/admin/finance",
        label: "Finance",
        description: "Invoices, payments, and active financial holds."
      });
    }
    if (hasPermission(permissions, "SUPPORT")) {
      actions.push({
        to: "/app/admin/requests",
        label: "Requests",
        description: "Review student tickets and operational backlog."
      });
    }
    if (hasPermission(permissions, "CONTENT")) {
      actions.push({
        to: "/app/admin/moderation",
        label: "Moderation & News",
        description: "Publish news and handle grade change review workflows."
      });
    }
    if (hasPermission(permissions, "SUPER")) {
      actions.push(
        {
          to: "/app/admin/analytics",
          label: "Analytics",
          description: "Institution-wide risk, overload, and service pressure."
        },
        {
          to: "/app/admin/workflows",
          label: "Workflows",
          description: "Cross-office workflow timeline and queue control."
        },
        {
          to: "/app/admin/assistant",
          label: "AI Assistant",
          description: "Ask for executive summaries powered by live portal data."
        },
        {
          to: "/app/admin/users",
          label: "Users",
          description: "Manage admin permissions and user access."
        }
      );
    }

    return actions;
  }, [permissions]);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Admin Overview</h2>
          <p className="muted">Your role-aware control room for the parts of the portal you manage.</p>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading admin overview...</p>
        </section>
      ) : null}

      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      {!loading && stats ? (
        <>
          <section className="card analytics-hero-card analytics-hero-card-admin">
            <div className="analytics-hero analytics-hero-split">
              <div className="analytics-hero-main">
                <span className="assistant-eyebrow">Operations snapshot</span>
                <h3>Portal health at a glance</h3>
                <p className="muted">
                  These live counts stay available for every admin account, even when advanced analytics are limited to super-admin access.
                </p>
                <div className="analytics-pill-group">
                  <span className="badge badge-neutral">{stats.students} students</span>
                  <span className="badge badge-neutral">{stats.teachers} teachers</span>
                  <span className="badge badge-warning">{stats.requests} requests</span>
                </div>
              </div>
              <div className="stats-grid">
                <div className="stat-card">
                  <strong>{stats.students}</strong>
                  <span>Students</span>
                </div>
                <div className="stat-card">
                  <strong>{stats.teachers}</strong>
                  <span>Teachers</span>
                </div>
                <div className="stat-card">
                  <strong>{stats.sections}</strong>
                  <span>Sections</span>
                </div>
                <div className="stat-card">
                  <strong>{stats.requests}</strong>
                  <span>Requests</span>
                </div>
                <div className="stat-card">
                  <strong>{stats.activeHolds}</strong>
                  <span>Active holds</span>
                </div>
                <div className="stat-card">
                  <strong>#{stats.adminId}</strong>
                  <span>Admin ID</span>
                </div>
              </div>
            </div>
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Available workspaces</h3>
                <p className="muted">Only the sections allowed by your admin permissions are shown here.</p>
              </div>
            </div>
            <div className="analytics-card-grid">
              {availableActions.map((action) => (
                <article key={action.to} className="analytics-focus-card">
                  <div className="analytics-focus-card-head">
                    <span className="badge badge-neutral">Workspace</span>
                  </div>
                  <h4>{action.label}</h4>
                  <p className="muted">{action.description}</p>
                  <Link className="link-btn" to={action.to}>
                    Open
                  </Link>
                </article>
              ))}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
