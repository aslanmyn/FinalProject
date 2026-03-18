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

export default function AdminAnalyticsPage() {
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
          setError(err instanceof ApiError ? err.message : "Failed to load admin analytics");
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
          <h2>Admin Analytics</h2>
          <p className="muted">Institution-wide academic risk, operational backlog, and overloaded section visibility.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/admin/workflows">
            Open workflows
          </Link>
          <Link className="link-btn" to="/app/admin/assistant">
            Ask AI assistant
          </Link>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading analytics...</p>
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
                <strong>{dashboard.metrics.teachers}</strong>
                <span>Teachers</span>
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
            </div>
          </section>

          <section className="analytics-split-grid">
            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Request load</h3>
                  <p className="muted">Demand by service category.</p>
                </div>
              </div>
              <div className="analytics-mini-list">
                {dashboard.requestLoads.map((item) => (
                  <div key={item.category} className="analytics-mini-row">
                    <span>{item.category}</span>
                    <strong>{item.count}</strong>
                  </div>
                ))}
              </div>
            </section>

            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Workflow backlog</h3>
                  <p className="muted">How much unfinished work is currently in the queue.</p>
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
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Faculty risk distribution</h3>
                <p className="muted">Average academic risk and attendance by faculty.</p>
              </div>
            </div>
            <div className="table-wrap">
              <table className="table analytics-table">
                <thead>
                  <tr>
                    <th>Faculty</th>
                    <th>Students</th>
                    <th>At risk</th>
                    <th>Medium risk</th>
                    <th>Average risk</th>
                    <th>Average attendance</th>
                    <th>Financial holds</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.facultyRisks.map((faculty) => (
                    <tr key={faculty.facultyName}>
                      <td>
                        <strong>{faculty.facultyName}</strong>
                      </td>
                      <td>{faculty.studentCount}</td>
                      <td>{faculty.atRiskStudents}</td>
                      <td>{faculty.mediumRiskStudents}</td>
                      <td>{faculty.averageRisk.toFixed(1)}</td>
                      <td>{faculty.averageAttendance.toFixed(1)}%</td>
                      <td>{faculty.studentsWithFinancialHolds}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Overloaded sections</h3>
                <p className="muted">Sections operating close to or above practical capacity.</p>
              </div>
            </div>
            <div className="table-wrap">
              <table className="table analytics-table">
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Teacher</th>
                    <th>Faculty</th>
                    <th>Semester</th>
                    <th>Seats</th>
                    <th>Utilization</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.overloadedSections.map((section) => (
                    <tr key={section.sectionId}>
                      <td>
                        <strong>{section.courseCode}</strong>
                        <div>{section.courseName}</div>
                      </td>
                      <td>{section.teacherName}</td>
                      <td>{section.facultyName}</td>
                      <td>{section.semesterName}</td>
                      <td>
                        {section.enrolledCount} / {section.capacity}
                      </td>
                      <td>
                        <span className="badge badge-warning">{section.utilizationPercent.toFixed(1)}%</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Critical students</h3>
                <p className="muted">Students with the highest current risk scores.</p>
              </div>
            </div>
            <div className="table-wrap">
              <table className="table analytics-table">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Faculty</th>
                    <th>Primary reason</th>
                    <th>Risk</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.criticalStudents.map((student) => (
                    <tr key={student.studentId}>
                      <td>
                        <strong>{student.studentName}</strong>
                      </td>
                      <td>{student.facultyName || "Unassigned"}</td>
                      <td>{student.primaryReason}</td>
                      <td>
                        <span className={riskBadgeClass(student.level)}>
                          {formatRiskLevel(student.level)} | {student.riskScore.toFixed(1)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
