import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchTeacherRiskDashboard } from "../../lib/api";
import type { TeacherRiskDashboard, TeacherRiskMeetingTimeItem } from "../../types/teacher";

function formatRiskLevel(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function riskBadgeClass(level: string): string {
  if (level === "AT_RISK") return "badge badge-danger";
  if (level === "MEDIUM") return "badge badge-warning";
  return "badge";
}

function formatLessonType(value: string | null): string {
  if (!value) return "";
  return value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function formatMeetingTime(meetingTime: TeacherRiskMeetingTimeItem): string {
  const day = meetingTime.dayOfWeek.slice(0, 3);
  const start = meetingTime.startTime ? meetingTime.startTime.slice(0, 5) : "--:--";
  const end = meetingTime.endTime ? meetingTime.endTime.slice(0, 5) : "--:--";
  const lessonType = formatLessonType(meetingTime.lessonType);
  return `${day} ${start}-${end}${meetingTime.room ? ` | ${meetingTime.room}` : ""}${lessonType ? ` | ${lessonType}` : ""}`;
}

export default function TeacherRiskPage() {
  const [dashboard, setDashboard] = useState<TeacherRiskDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchTeacherRiskDashboard();
        if (!cancelled) {
          setDashboard(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load teaching risk dashboard");
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

  const highAttentionSections = useMemo(
    () => dashboard?.sections.filter((section) => section.level !== "STABLE").length ?? 0,
    [dashboard]
  );

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Teaching Risk Dashboard</h2>
          <p className="muted">See weak attendance, unpublished finals, pending grade changes, and students at risk.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/teacher/assistant">
            Ask AI assistant
          </Link>
          <Link className="link-btn" to="/app/teacher/sections">
            Open sections
          </Link>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading risk dashboard...</p>
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
                <strong>{dashboard.currentSections}</strong>
                <span>Current sections</span>
              </div>
              <div className="stat-card">
                <strong>{dashboard.atRiskStudents}</strong>
                <span>Students at risk</span>
              </div>
              <div className="stat-card">
                <strong>{highAttentionSections}</strong>
                <span>Sections needing attention</span>
              </div>
              <div className="stat-card">
                <strong>{dashboard.pendingGradeChanges}</strong>
                <span>Pending grade changes</span>
              </div>
              <div className="stat-card">
                <strong>{dashboard.unpublishedFinals}</strong>
                <span>Unpublished finals</span>
              </div>
            </div>
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Section risk view</h3>
                <p className="muted">Current section health across attendance, publication backlog, and risk signals.</p>
              </div>
            </div>
            <div className="table-wrap">
              <table className="table analytics-table">
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Semester</th>
                    <th>Schedule</th>
                    <th>Enrollment</th>
                    <th>Attendance</th>
                    <th>Issues</th>
                    <th>Risk</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.sections.map((section) => (
                    <tr key={section.sectionId}>
                      <td>
                        <strong>{section.courseCode}</strong>
                        <div>{section.courseName}</div>
                      </td>
                      <td>{section.semesterName}</td>
                      <td>
                        <div className="schedule-chip-list">
                          {section.meetingTimes.map((meetingTime, index) => (
                            <span key={`${section.sectionId}-${index}`} className="schedule-chip">
                              {formatMeetingTime(meetingTime)}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        {section.enrolledCount} / {section.capacity}
                      </td>
                      <td>{section.attendanceRate.toFixed(1)}%</td>
                      <td>
                        <div className="analytics-needed-stack">
                          <span>At-risk students: {section.atRiskStudents}</span>
                          <span>Pending grade changes: {section.pendingGradeChanges}</span>
                          <span>Unpublished finals: {section.unpublishedFinals}</span>
                        </div>
                        {section.reasons[0] ? <p className="muted compact-text">{section.reasons[0]}</p> : null}
                      </td>
                      <td>
                        <span className={riskBadgeClass(section.level)}>
                          {formatRiskLevel(section.level)} | {section.riskScore.toFixed(1)}
                        </span>
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
                <h3>Students at risk</h3>
                <p className="muted">Students that need intervention first based on live academic signals.</p>
              </div>
            </div>
            <div className="table-wrap">
              <table className="table analytics-table">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Course</th>
                    <th>Attendance</th>
                    <th>Attestation subtotal</th>
                    <th>Final total</th>
                    <th>Reasons</th>
                    <th>Risk</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.students.map((student) => (
                    <tr key={`${student.sectionId}-${student.studentId}`}>
                      <td>
                        <strong>{student.studentName}</strong>
                        <div className="muted">{student.studentEmail}</div>
                      </td>
                      <td>
                        <strong>{student.courseCode}</strong>
                        <div>{student.courseName}</div>
                      </td>
                      <td>{student.attendanceRate.toFixed(1)}%</td>
                      <td>{student.attestationSubtotal.toFixed(1)}</td>
                      <td>{student.finalTotal !== null ? student.finalTotal.toFixed(1) : "Not published"}</td>
                      <td>{student.reasons.join(", ")}</td>
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
