import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  fetchStudentPlanner,
  fetchStudentRiskDashboard,
  fetchStudentWorkflows,
  simulateStudentPlanner
} from "../../lib/api";
import type {
  StudentPlannerCourse,
  StudentPlannerDashboard,
  StudentPlannerSimulation,
  StudentRiskDashboard
} from "../../types/student";
import type { WorkflowOverview } from "../../types/common";

function formatRiskLevel(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function riskBadgeClass(level: string): string {
  if (level === "AT_RISK") return "badge badge-danger";
  if (level === "MEDIUM") return "badge badge-warning";
  return "badge";
}

function courseDefaults(course: StudentPlannerCourse): string {
  const fallback = course.publishedFinal ?? course.neededForB ?? course.neededForPass ?? 20;
  return Number.isFinite(fallback) ? String(fallback) : "";
}

function parseProjectedValue(value: string): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return 0;
  }
  return Math.max(0, Math.min(40, parsed));
}

function clampPercent(value: number): number {
  return Math.max(0, Math.min(100, value));
}

function signedDelta(value: number): string {
  if (value > 0) return `+${value.toFixed(2)}`;
  if (value < 0) return value.toFixed(2);
  return "0.00";
}

export default function StudentPlannerPage() {
  const [risk, setRisk] = useState<StudentRiskDashboard | null>(null);
  const [planner, setPlanner] = useState<StudentPlannerDashboard | null>(null);
  const [workflows, setWorkflows] = useState<WorkflowOverview | null>(null);
  const [simulation, setSimulation] = useState<StudentPlannerSimulation | null>(null);
  const [projectedFinals, setProjectedFinals] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [simulating, setSimulating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [riskPayload, plannerPayload, workflowsPayload] = await Promise.all([
          fetchStudentRiskDashboard(),
          fetchStudentPlanner(),
          fetchStudentWorkflows()
        ]);
        if (!cancelled) {
          setRisk(riskPayload);
          setPlanner(plannerPayload);
          setWorkflows(workflowsPayload);
          setProjectedFinals(
            plannerPayload.courses.reduce<Record<number, string>>((acc, course) => {
              acc[course.sectionId] = courseDefaults(course);
              return acc;
            }, {})
          );
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load academic planner");
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

  const simulationPayload = useMemo(() => {
    if (!planner) {
      return [];
    }
    return planner.courses.map((course) => ({
      sectionId: course.sectionId,
      projectedFinalScore: parseProjectedValue(projectedFinals[course.sectionId] ?? courseDefaults(course))
    }));
  }, [planner, projectedFinals]);

  useEffect(() => {
    if (!planner || simulationPayload.length === 0) {
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(async () => {
      setSimulating(true);
      try {
        const payload = await simulateStudentPlanner(simulationPayload);
        if (!cancelled) {
          setSimulation(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to simulate planner");
        }
      } finally {
        if (!cancelled) {
          setSimulating(false);
        }
      }
    }, 280);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [planner, simulationPayload]);

  const simulationBySection = useMemo(
    () => new Map((simulation?.courses ?? []).map((course) => [course.sectionId, course])),
    [simulation]
  );

  const topRiskCourses = useMemo(() => risk?.courses.slice(0, 4) ?? [], [risk]);
  const overdueWorkflowCount = useMemo(
    () => workflows?.items.filter((item) => item.overdue).length ?? 0,
    [workflows]
  );
  const atRiskCoursesCount = useMemo(
    () => risk?.courses.filter((course) => course.level === "AT_RISK").length ?? 0,
    [risk]
  );
  const projectionDelta = useMemo(() => {
    const projected = simulation?.projectedOverallGpa ?? planner?.currentPublishedGpa ?? 0;
    const current = planner?.currentPublishedGpa ?? 0;
    return projected - current;
  }, [planner, simulation]);
  const strongestProjection = useMemo(() => {
    const courses = simulation?.courses ?? [];
    if (courses.length === 0) return null;
    return [...courses].sort((left, right) => right.projectedTotal - left.projectedTotal)[0];
  }, [simulation]);

  function handleProjectedFinalChange(sectionId: number, value: string) {
    setProjectedFinals((prev) => ({
      ...prev,
      [sectionId]: value
    }));
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Academic Planner</h2>
          <p className="muted">Risk scoring, final score modelling, and GPA planning in one place.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/student/assistant">
            Ask AI assistant
          </Link>
          <Link className="link-btn" to="/app/student/workflows">
            Open workflows
          </Link>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading planner...</p>
        </section>
      ) : null}

      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      {!loading && risk && planner ? (
        <>
          <section className="card analytics-hero-card analytics-hero-card-student">
            <div className="analytics-hero analytics-hero-split">
              <div className="analytics-hero-main">
                <span className="assistant-eyebrow">Current semester</span>
                <h3>{planner.semesterName || "Planner snapshot"}</h3>
                <p className="muted">
                  We calculate risk from attendance, attestation results, final grades, and active holds.
                </p>
                <div className="analytics-pill-group">
                  <span className={riskBadgeClass(risk.level)}>{formatRiskLevel(risk.level)}</span>
                  <span className="badge badge-neutral">{planner.courses.length} planned courses</span>
                  <span className="badge badge-neutral">{workflows?.items.length ?? 0} workflow items</span>
                </div>
                <div className="analytics-meter-list">
                  <div className="analytics-meter-card">
                    <div className="analytics-meter-head">
                      <span>Risk intensity</span>
                      <strong>{risk.riskScore.toFixed(1)} / 100</strong>
                    </div>
                    <div className="analytics-meter">
                      <div
                        className="analytics-meter-fill analytics-meter-fill-danger"
                        style={{ width: `${clampPercent(risk.riskScore)}%` }}
                      />
                    </div>
                  </div>
                  <div className="analytics-meter-card">
                    <div className="analytics-meter-head">
                      <span>Attendance resilience</span>
                      <strong>{risk.attendanceRate.toFixed(1)}%</strong>
                    </div>
                    <div className="analytics-meter">
                      <div
                        className="analytics-meter-fill analytics-meter-fill-success"
                        style={{ width: `${clampPercent(risk.attendanceRate)}%` }}
                      />
                    </div>
                  </div>
                  <div className="analytics-meter-card">
                    <div className="analytics-meter-head">
                      <span>Planner uplift</span>
                      <strong>{signedDelta(projectionDelta)}</strong>
                    </div>
                    <div className="analytics-meter">
                      <div
                        className="analytics-meter-fill analytics-meter-fill-accent"
                        style={{ width: `${clampPercent(Math.abs(projectionDelta) * 30)}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>
              <div className="analytics-hero-side-grid">
                <div className="analytics-stat-grid">
                  <article className="analytics-stat">
                    <span>Overall risk</span>
                    <strong>{formatRiskLevel(risk.level)}</strong>
                    <small>{risk.riskScore.toFixed(1)} / 100</small>
                  </article>
                  <article className="analytics-stat">
                    <span>Published GPA</span>
                    <strong>{risk.publishedGpa.toFixed(2)}</strong>
                    <small>Current official GPA</small>
                  </article>
                  <article className="analytics-stat">
                    <span>Projected GPA</span>
                    <strong>{(simulation?.projectedOverallGpa ?? planner.currentPublishedGpa).toFixed(2)}</strong>
                    <small>{simulating ? "Updating projection..." : "Live planner result"}</small>
                  </article>
                  <article className="analytics-stat">
                    <span>Attendance</span>
                    <strong>{risk.attendanceRate.toFixed(1)}%</strong>
                    <small>{workflows?.items.length ?? 0} active workflows</small>
                  </article>
                </div>
                <div className="analytics-spotlight-card">
                  <span className="assistant-summary-label">Planner spotlight</span>
                  <strong>{strongestProjection ? strongestProjection.courseCode : "No projection yet"}</strong>
                  <p className="muted">
                    {strongestProjection
                      ? `${strongestProjection.courseName} projects to ${strongestProjection.projectedTotal.toFixed(1)} with ${strongestProjection.projectedLetter}.`
                      : "Start adjusting projected finals to see a stronger scenario."}
                  </p>
                </div>
              </div>
            </div>
          </section>

          <section className="analytics-split-grid">
            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Why attention is needed</h3>
                  <p className="muted">Top signals affecting your current academic risk.</p>
                </div>
                <span className={riskBadgeClass(risk.level)}>{formatRiskLevel(risk.level)}</span>
              </div>
              {risk.reasons.length > 0 ? (
                <div className="analytics-reason-list">
                  {risk.reasons.map((reason) => (
                    <article key={reason} className="analytics-reason-card">
                      <strong>{reason}</strong>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="muted">No major warning signs detected right now.</p>
              )}
            </section>

            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Workflow pressure</h3>
                  <p className="muted">Registration and service tasks that may still need action.</p>
                </div>
                <Link className="link-btn" to="/app/student/workflows">
                  View all
                </Link>
              </div>
              <div className="analytics-mini-list">
                <div className="analytics-mini-row">
                  <span>Open workflows</span>
                  <strong>{workflows?.items.length ?? 0}</strong>
                </div>
                <div className="analytics-mini-row">
                  <span>Overdue items</span>
                  <strong>{overdueWorkflowCount}</strong>
                </div>
                <div className="analytics-mini-row">
                  <span>Active holds</span>
                  <strong>{risk.activeHolds}</strong>
                </div>
                <div className="analytics-mini-row">
                  <span>Open requests</span>
                  <strong>{risk.openRequests}</strong>
                </div>
                <div className="analytics-mini-row">
                  <span>Courses at risk</span>
                  <strong>{atRiskCoursesCount}</strong>
                </div>
              </div>
            </section>
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Courses to focus on first</h3>
                <p className="muted">Highest-risk courses based on attendance and current grade signals.</p>
              </div>
            </div>
            <div className="analytics-card-grid">
              {topRiskCourses.map((course) => (
                <article key={course.sectionId} className="analytics-focus-card">
                  <div className="analytics-focus-card-head">
                    <span className="badge">{course.courseCode}</span>
                    <span className={riskBadgeClass(course.level)}>{formatRiskLevel(course.level)}</span>
                  </div>
                  <h4>{course.courseName}</h4>
                  <div className="analytics-focus-meta">
                    <span>Attendance {course.attendanceRate.toFixed(1)}%</span>
                    <span>Attestations {(course.attestation1 + course.attestation2).toFixed(1)} / 60</span>
                  </div>
                  <div className="analytics-meter analytics-meter-compact">
                    <div
                      className="analytics-meter-fill analytics-meter-fill-danger"
                      style={{ width: `${clampPercent(course.riskScore)}%` }}
                    />
                  </div>
                  <p className="muted">{course.reasons[0] ?? "Performance is stable."}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="card analytics-panel">
            <div className="section-heading">
              <div>
                <h3>Final score planner</h3>
                <p className="muted">Adjust the projected final for each course and watch GPA and totals update.</p>
              </div>
              <div className="actions">
                <span className="badge badge-neutral">
                  Term GPA {simulation ? simulation.projectedTermGpa.toFixed(2) : planner.currentPublishedGpa.toFixed(2)}
                </span>
                <span className="badge">
                  Overall GPA {(simulation?.projectedOverallGpa ?? planner.currentPublishedGpa).toFixed(2)}
                </span>
              </div>
            </div>

            <div className="table-wrap">
              <table className="table analytics-table">
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Teacher</th>
                    <th>Att 1</th>
                    <th>Att 2</th>
                    <th>Published final</th>
                    <th>Projected final</th>
                    <th>Projected total</th>
                    <th>Letter</th>
                    <th>Needed</th>
                    <th>Attendance</th>
                    <th>Risk</th>
                  </tr>
                </thead>
                <tbody>
                  {planner.courses.map((course) => {
                    const riskCourse = risk.courses.find((item) => item.sectionId === course.sectionId);
                    const simulated = simulationBySection.get(course.sectionId);
                    return (
                      <tr key={course.sectionId}>
                        <td>
                          <strong>{course.courseCode}</strong>
                          <div>{course.courseName}</div>
                        </td>
                        <td>{course.teacherName || "TBA"}</td>
                        <td>{course.attestation1.toFixed(1)}</td>
                        <td>{course.attestation2.toFixed(1)}</td>
                        <td>{course.publishedFinal !== null ? course.publishedFinal.toFixed(1) : "Not published"}</td>
                        <td>
                          <input
                            type="number"
                            min={0}
                            max={40}
                            step={0.5}
                            value={projectedFinals[course.sectionId] ?? ""}
                            onChange={(event) => handleProjectedFinalChange(course.sectionId, event.target.value)}
                            className="planner-input"
                          />
                        </td>
                        <td>{simulated ? simulated.projectedTotal.toFixed(1) : "-"}</td>
                        <td>{simulated?.projectedLetter ?? course.publishedLetter ?? "-"}</td>
                        <td>
                          <div className="analytics-needed-stack">
                            <span>Pass: {course.neededForPass !== null ? course.neededForPass.toFixed(1) : "N/A"}</span>
                            <span>B: {course.neededForB !== null ? course.neededForB.toFixed(1) : "N/A"}</span>
                            <span>A: {course.neededForA !== null ? course.neededForA.toFixed(1) : "N/A"}</span>
                          </div>
                        </td>
                        <td>{riskCourse ? `${riskCourse.attendanceRate.toFixed(1)}%` : "-"}</td>
                        <td>
                          {riskCourse ? (
                            <span className={riskBadgeClass(riskCourse.level)}>{formatRiskLevel(riskCourse.level)}</span>
                          ) : (
                            <span className="badge badge-neutral">No data</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
