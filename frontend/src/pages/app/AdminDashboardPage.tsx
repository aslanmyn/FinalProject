import { useEffect, useMemo, useState } from "react";
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

  const topWorkflow = useMemo(() => dashboard?.workflowSummary[0] ?? null, [dashboard]);
  const topStudent = useMemo(() => dashboard?.criticalStudents[0] ?? null, [dashboard]);
  const topSection = useMemo(() => dashboard?.overloadedSections[0] ?? null, [dashboard]);
  const workflowTotal = useMemo(
    () => dashboard?.workflowSummary.reduce((sum, item) => sum + item.count, 0) ?? 0,
    [dashboard]
  );
  const overloadPeak = topSection?.utilizationPercent ?? 0;

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
          <section className="card analytics-hero-card analytics-hero-card-admin">
            <div className="analytics-hero analytics-hero-split">
              <div className="analytics-hero-main">
                <span className="assistant-eyebrow">Executive snapshot</span>
                <h3>What needs admin attention first</h3>
                <p className="muted">
                  Keep an eye on operational backlog, critical student risk, and overloaded teaching capacity before they turn into service issues.
                </p>
                <div className="analytics-pill-group">
                  <span className="badge badge-warning">{workflowTotal} workflow items</span>
                  <span className="badge badge-neutral">{dashboard.overloadedSections.length} overloaded sections</span>
                  <span className="badge badge-neutral">{dashboard.criticalStudents.length} critical students</span>
                </div>
                <div className="analytics-meter-list">
                  <div className="analytics-meter-card">
                    <div className="analytics-meter-head">
                      <span>Workflow pressure</span>
                      <strong>{workflowTotal}</strong>
                    </div>
                    <div className="analytics-meter">
                      <div
                        className="analytics-meter-fill analytics-meter-fill-danger"
                        style={{ width: `${Math.max(0, Math.min(100, workflowTotal * 10))}%` }}
                      />
                    </div>
                  </div>
                  <div className="analytics-meter-card">
                    <div className="analytics-meter-head">
                      <span>Overload peak</span>
                      <strong>{overloadPeak.toFixed(1)}%</strong>
                    </div>
                    <div className="analytics-meter">
                      <div
                        className="analytics-meter-fill analytics-meter-fill-warning"
                        style={{ width: `${Math.max(0, Math.min(100, overloadPeak))}%` }}
                      />
                    </div>
                  </div>
                  <div className="analytics-meter-card">
                    <div className="analytics-meter-head">
                      <span>Open windows</span>
                      <strong>{dashboard.metrics.openWindows}</strong>
                    </div>
                    <div className="analytics-meter">
                      <div
                        className="analytics-meter-fill analytics-meter-fill-accent"
                        style={{ width: `${Math.max(0, Math.min(100, dashboard.metrics.openWindows * 20))}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>
              <div className="analytics-hero-side-grid">
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
                    <strong>{dashboard.metrics.openWindows}</strong>
                    <span>Open windows</span>
                  </div>
                  <div className="stat-card">
                    <strong>{dashboard.metrics.teachers}</strong>
                    <span>Teachers</span>
                  </div>
                </div>
                <div className="analytics-spotlight-card">
                  <span className="assistant-summary-label">Priority spotlight</span>
                  <strong>{topWorkflow ? topWorkflow.workflowType : "No backlog"}</strong>
                  <p className="muted">
                    {topWorkflow
                      ? `${topWorkflow.count} items sit in the busiest workflow queue right now.`
                      : "Operational queues are clear."}
                  </p>
                </div>
              </div>
            </div>
          </section>

          <section className="analytics-split-grid">
            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Operational hotspot</h3>
                  <p className="muted">The queue or section most likely to need intervention next.</p>
                </div>
              </div>
              <div className="analytics-card-grid">
                <article className="analytics-focus-card">
                  <div className="analytics-focus-card-head">
                    <span className="badge badge-warning">Workflow</span>
                    <span className="badge badge-neutral">{topWorkflow ? topWorkflow.count : 0} items</span>
                  </div>
                  <h4>{topWorkflow ? topWorkflow.workflowType : "No backlog"}</h4>
                  <p className="muted">
                    {topWorkflow ? "This queue has the highest current operational load." : "All workflow queues are balanced."}
                  </p>
                </article>
                <article className="analytics-focus-card">
                  <div className="analytics-focus-card-head">
                    <span className="badge badge-warning">Section</span>
                    <span className="badge badge-warning">{overloadPeak.toFixed(1)}%</span>
                  </div>
                  <h4>{topSection ? topSection.courseCode : "No overloaded sections"}</h4>
                  <p className="muted">
                    {topSection
                      ? `${topSection.courseName} is the most capacity-constrained section right now.`
                      : "Section capacity is currently under control."}
                  </p>
                </article>
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
                {dashboard.criticalStudents.length === 0 ? <p className="muted">No critical students right now.</p> : null}
              </div>
              {topStudent ? (
                <div className="analytics-spotlight-card analytics-spotlight-inline">
                  <span className="assistant-summary-label">Highest risk student</span>
                  <strong>{topStudent.studentName}</strong>
                  <p className="muted">{topStudent.primaryReason}</p>
                </div>
              ) : null}
            </section>
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Top workflow backlog</h3>
                <p className="muted">The biggest queues that currently need operational attention.</p>
              </div>
            </div>
            <div className="analytics-mini-list">
              {dashboard.workflowSummary.map((item) => (
                <div key={item.workflowType} className="analytics-load-card">
                  <div className="analytics-mini-row">
                    <span>{item.workflowType}</span>
                    <strong>{item.count}</strong>
                  </div>
                  <div className="analytics-meter analytics-meter-compact">
                    <div
                      className="analytics-meter-fill analytics-meter-fill-danger"
                      style={{ width: `${Math.max(0, Math.min(100, item.count * 18))}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
