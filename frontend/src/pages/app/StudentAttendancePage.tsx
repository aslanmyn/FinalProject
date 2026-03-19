import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  checkInStudentAttendance,
  fetchStudentAttendance,
} from "../../lib/api";
import { connectStomp, subscribeTo } from "../../lib/ws";
import type {
  StudentAttendanceActiveSessionItem,
  StudentAttendanceData,
  StudentAttendanceRecordItem,
} from "../../types/student";

function buildSummary(records: StudentAttendanceRecordItem[]) {
  const present = records.filter((item) => item.status === "PRESENT").length;
  const late = records.filter((item) => item.status === "LATE").length;
  const absent = records.filter((item) => item.status === "ABSENT").length;
  const total = records.length;
  const percentage = total === 0 ? 0 : ((present + late) * 100) / total;

  return { present, late, absent, total, percentage };
}

function formatDeadline(value: string | null) {
  if (!value) return "No deadline";
  const date = new Date(value);
  return date.toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getAttendanceActionLabel(session: StudentAttendanceActiveSessionItem) {
  if (session.currentStatus) {
    return session.teacherConfirmed ? "Marked" : "Submitted";
  }
  return "Mark attendance";
}

export default function StudentAttendancePage() {
  const [data, setData] = useState<StudentAttendanceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [selectedCourseCode, setSelectedCourseCode] = useState("ALL");
  const [checkInCodes, setCheckInCodes] = useState<Record<number, string>>({});
  const [submittingSessionId, setSubmittingSessionId] = useState<number | null>(null);

  const loadAttendance = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const payload = await fetchStudentAttendance();
      setData(payload);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load attendance");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAttendance();
  }, [loadAttendance]);

  useEffect(() => {
    connectStomp();
    const unsubscribe = subscribeTo("/user/queue/attendance", () => {
      void loadAttendance();
    });
    return unsubscribe;
  }, [loadAttendance]);

  const courseOptions = useMemo(() => {
    if (!data) return [];
    const map = new Map<string, string>();
    data.records.forEach((item) => {
      if (!map.has(item.subjectCode)) {
        map.set(item.subjectCode, item.subjectName);
      }
    });
    return Array.from(map.entries())
      .map(([code, name]) => ({ code, name }))
      .sort((left, right) => left.code.localeCompare(right.code));
  }, [data]);

  const filteredRecords = useMemo(() => {
    if (!data) return [];
    if (selectedCourseCode === "ALL") return data.records;
    return data.records.filter((item) => item.subjectCode === selectedCourseCode);
  }, [data, selectedCourseCode]);

  const filteredSummary = useMemo(() => buildSummary(filteredRecords), [filteredRecords]);

  async function handleCheckIn(session: StudentAttendanceActiveSessionItem) {
    setSubmittingSessionId(session.sessionId);
    setError(null);
    setSuccess(null);
    try {
      const code = checkInCodes[session.sessionId]?.trim();
      await checkInStudentAttendance(session.sessionId, code || undefined);
      setSuccess(`${session.subjectCode} attendance marked successfully.`);
      setCheckInCodes((current) => ({ ...current, [session.sessionId]: "" }));
      await loadAttendance();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to mark attendance");
    } finally {
      setSubmittingSessionId(null);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Attendance</h2>
      </header>
      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}
      {!loading && !error && data ? (
        <>
          <section className="card attendance-live-card">
            <div className="attendance-live-header">
              <div>
                <h3>Live Check-In</h3>
                <p className="muted">When a professor opens attendance, it appears here in real time.</p>
              </div>
              <span className="badge badge-neutral">{data.activeSessions.length} active</span>
            </div>

            {data.activeSessions.length === 0 ? (
              <div className="attendance-live-empty muted">No active attendance sessions right now.</div>
            ) : (
              <div className="attendance-live-grid">
                {data.activeSessions.map((session) => {
                  const isMarked = Boolean(session.currentStatus);
                  return (
                    <article key={session.sessionId} className="attendance-session-card">
                      <div className="attendance-session-top">
                        <div>
                          <strong>
                            {session.subjectCode} {session.subjectName}
                          </strong>
                          <p className="muted">{session.teacherName || "Professor"}</p>
                        </div>
                        <span className={`badge ${session.checkInMode === "CODE" ? "badge-warning" : "badge-neutral"}`}>
                          {session.checkInMode === "CODE" ? "Code required" : "One click"}
                        </span>
                      </div>

                      <div className="attendance-session-meta">
                        <span>Class date: {session.classDate || "-"}</span>
                        <span>Closes: {formatDeadline(session.attendanceCloseAt)}</span>
                        <span>
                          Status: {session.currentStatus || "Waiting for check-in"}
                          {session.currentStatus && session.teacherConfirmed ? " · confirmed" : ""}
                        </span>
                      </div>

                      {session.checkInMode === "CODE" ? (
                        <label className="attendance-code-field">
                          <span>Check-in code</span>
                          <input
                            value={checkInCodes[session.sessionId] ?? ""}
                            onChange={(event) =>
                              setCheckInCodes((current) => ({
                                ...current,
                                [session.sessionId]: event.target.value,
                              }))
                            }
                            placeholder="Enter code"
                            disabled={isMarked || submittingSessionId === session.sessionId}
                          />
                        </label>
                      ) : null}

                      <div className="actions">
                        <button
                          type="button"
                          disabled={isMarked || submittingSessionId === session.sessionId}
                          onClick={() => void handleCheckIn(session)}
                        >
                          {submittingSessionId === session.sessionId ? "Submitting..." : getAttendanceActionLabel(session)}
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>

          <section className="card schedule-filter-card">
            <div className="schedule-filter-bar">
              <label className="schedule-filter-group">
                <span>Subject</span>
                <select
                  className="schedule-filter-select"
                  value={selectedCourseCode}
                  onChange={(event) => setSelectedCourseCode(event.target.value)}
                >
                  <option value="ALL">All subjects</option>
                  {courseOptions.map((course) => (
                    <option key={course.code} value={course.code}>
                      {course.code} - {course.name}
                    </option>
                  ))}
                </select>
              </label>

              <div className="schedule-filter-summary">
                <span className="schedule-filter-summary-label">Showing</span>
                <strong>
                  {selectedCourseCode === "ALL" ? "All subjects" : `${selectedCourseCode} attendance`}
                </strong>
              </div>
            </div>
          </section>

          <section className="card">
            <div className="stats-grid">
              <div className="stat-card">
                <strong>{filteredSummary.present}</strong>
                <span>Present</span>
              </div>
              <div className="stat-card">
                <strong>{filteredSummary.late}</strong>
                <span>Late</span>
              </div>
              <div className="stat-card">
                <strong>{filteredSummary.absent}</strong>
                <span>Absent</span>
              </div>
              <div className="stat-card">
                <strong>{filteredSummary.total}</strong>
                <span>Total</span>
              </div>
              <div className="stat-card">
                <strong>{filteredSummary.percentage.toFixed(1)}%</strong>
                <span>Attendance Rate</span>
              </div>
            </div>
          </section>

          <section className="card">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Course</th>
                    <th>Name</th>
                    <th>Status</th>
                    <th>Reason</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredRecords.map((item, idx) => (
                    <tr key={`${item.date}-${item.subjectCode}-${idx}`}>
                      <td>{item.date}</td>
                      <td>{item.subjectCode}</td>
                      <td>{item.subjectName}</td>
                      <td>{item.status}</td>
                      <td>{item.reason || "-"}</td>
                    </tr>
                  ))}
                  {filteredRecords.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="muted">
                        No attendance records for the selected subject.
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
