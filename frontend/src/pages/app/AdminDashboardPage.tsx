import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchAdminAnalytics } from "../../lib/api";
import type { AdminAnalyticsDashboard } from "../../types/admin";

function formatRiskLevel(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function riskBadgeClass(level: string): string {
  if (level === "AT_RISK") return "badge badge-danger";
  if (level === "MEDIUM") return "badge badge-warning";
  return "badge";
}

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<AdminAnalyticsDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchAdminAnalytics();
        if (!cancelled) {
          setDashboard(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load admin dashboard");
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

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Admin Overview</h2>
          <p className="muted">A live executive snapshot of academic risk, workflow pressure, and overloaded sections.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/admin/analytics">
            Full analytics
          </Link>
          <Link className="link-btn" to="/app/admin/workflows">
            Workflow engine
          </Link>
          <Link className="link-btn" to="/app/admin/assistant">
            AI assistant
          </Link>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading admin dashboard...</p>
        </section>
      ) : null}

      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      {!loading && dashboard ? (
        <>
          <section className="card">
            <div className="stats-grid">
              <div className="stat-card">
                <strong>{dashboard.metrics.students}</strong>
                <span>Students</span>
              </div>
              <div className="stat-card">
                <strong>{dashboard.metrics.currentSections}</strong>
                <span>Current sections</span>
              </div>
              <div className="stat-card">
                <strong>{dashboard.metrics.requests}</strong>
                <span>Requests</span>
              </div>
              <div className="stat-card">
                <strong>{dashboard.metrics.activeHolds}</strong>
                <span>Active holds</span>
              </div>
              <div className="stat-card">
                <strong>{dashboard.overloadedSections.length}</strong>
                <span>Overloaded sections</span>
              </div>
            </div>
          </section>

          <section className="analytics-split-grid">
            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Top workflow backlog</h3>
                  <p className="muted">The biggest queues that currently need operational attention.</p>
                </div>
              </div>
              <div className="analytics-mini-list">
                {dashboard.workflowSummary.map((item) => (
                  <div key={item.workflowType} className="analytics-mini-row">
                    <span>{item.workflowType}</span>
                    <strong>{item.count}</strong>
                  </div>
                ))}
              </div>
            </section>

            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Critical student watchlist</h3>
                  <p className="muted">Students with the strongest current risk signals.</p>
                </div>
              </div>
              <div className="analytics-mini-list">
                {dashboard.criticalStudents.slice(0, 5).map((student) => (
                  <div key={student.studentId} className="analytics-watch-row">
                    <div>
                      <strong>{student.studentName}</strong>
                      <p className="muted">{student.primaryReason}</p>
                    </div>
                    <span className={riskBadgeClass(student.level)}>
                      {formatRiskLevel(student.level)} | {student.riskScore.toFixed(1)}
                    </span>
                  </div>
                ))}
              </div>
            </section>
          </section>
        </>
      ) : null}
    </div>
  );
}
