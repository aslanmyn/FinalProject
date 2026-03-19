import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  closeTeacherAttendanceSession,
  fetchTeacherActiveAttendance,
  fetchTeacherRoster,
  fetchTeacherSections,
  openTeacherAttendanceSession,
  overrideTeacherAttendance,
} from "../../lib/api";
import { connectStomp, subscribeTo } from "../../lib/ws";
import type {
  TeacherActiveAttendancePayload,
  TeacherAttendanceRecordItem,
  TeacherRosterItem,
  TeacherSectionItem,
} from "../../types/teacher";

const attendanceStatuses = ["PRESENT", "LATE", "ABSENT"] as const;

function buildDefaultCloseAt() {
  const date = new Date(Date.now() + 15 * 60 * 1000);
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60 * 1000);
  return local.toISOString().slice(0, 16);
}

function formatTimestamp(value: string | null) {
  if (!value) return "-";
  const date = new Date(value);
  return date.toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function TeacherAttendancePage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [sectionId, setSectionId] = useState<number | "">("");
  const [classDate, setClassDate] = useState<string>(new Date().toISOString().slice(0, 10));
  const [closeAt, setCloseAt] = useState<string>(buildDefaultCloseAt());
  const [checkInMode, setCheckInMode] = useState<"ONE_CLICK" | "CODE">("ONE_CLICK");
  const [checkInCode, setCheckInCode] = useState("");
  const [allowTeacherOverride, setAllowTeacherOverride] = useState(true);
  const [sessionPayload, setSessionPayload] = useState<TeacherActiveAttendancePayload>({ session: null, records: [] });
  const [roster, setRoster] = useState<TeacherRosterItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [reloading, setReloading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [closing, setClosing] = useState(false);
  const [savingStudentId, setSavingStudentId] = useState<number | null>(null);
  const [overrideStatuses, setOverrideStatuses] = useState<Record<number, (typeof attendanceStatuses)[number]>>({});
  const [overrideReasons, setOverrideReasons] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const selectedSection = useMemo(
    () => sections.find((item) => item.id === sectionId) ?? null,
    [sections, sectionId],
  );

  const loadSections = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchTeacherSections();
      setSections(data);
      setSectionId((current) => (current ? current : data[0]?.id ?? ""));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load sections");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadAttendanceView = useCallback(async (targetSectionId: number, targetDate: string) => {
    setReloading(true);
    setError(null);
    try {
      const [activeData, rosterData] = await Promise.all([
        fetchTeacherActiveAttendance(targetSectionId, targetDate),
        fetchTeacherRoster(targetSectionId),
      ]);
      setSessionPayload(activeData);
      setRoster(rosterData);
      setOverrideStatuses((current) => {
        const next = { ...current };
        activeData.records.forEach((record) => {
          if (record.status) {
            next[record.studentId] = record.status;
          }
        });
        return next;
      });
      setOverrideReasons((current) => {
        const next = { ...current };
        activeData.records.forEach((record) => {
          if (record.reason && !(record.studentId in next)) {
            next[record.studentId] = record.reason;
          }
        });
        return next;
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load attendance session");
      setSessionPayload({ session: null, records: [] });
      setRoster([]);
    } finally {
      setReloading(false);
    }
  }, []);

  useEffect(() => {
    void loadSections();
  }, [loadSections]);

  useEffect(() => {
    if (!sectionId) {
      setSessionPayload({ session: null, records: [] });
      setRoster([]);
      return;
    }
    void loadAttendanceView(Number(sectionId), classDate);
  }, [sectionId, classDate, loadAttendanceView]);

  useEffect(() => {
    if (!sectionId) return undefined;
    connectStomp();
    const unsubscribe = subscribeTo(`/topic/attendance/section/${sectionId}`, () => {
      void loadAttendanceView(Number(sectionId), classDate);
    });
    return unsubscribe;
  }, [sectionId, classDate, loadAttendanceView]);

  async function handleOpenSession(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId) return;
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const payload = await openTeacherAttendanceSession(Number(sectionId), {
        classDate,
        closeAt,
        checkInMode,
        checkInCode: checkInMode === "CODE" ? checkInCode : undefined,
        allowTeacherOverride,
      });
      setSessionPayload(payload);
      setSuccess("Attendance session opened.");
      setCheckInCode(payload.session?.checkInCode ?? "");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to open attendance session");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCloseSession() {
    if (!sessionPayload.session) return;
    setClosing(true);
    setError(null);
    setSuccess(null);
    try {
      const payload = await closeTeacherAttendanceSession(sessionPayload.session.id);
      setSessionPayload(payload);
      setSuccess("Attendance session closed.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to close attendance session");
    } finally {
      setClosing(false);
    }
  }

  async function handleOverride(record: TeacherAttendanceRecordItem | TeacherRosterItem) {
    const sessionId = sessionPayload.session?.id;
    if (!sessionId) return;
    const nextStatus = overrideStatuses[record.studentId] ?? "PRESENT";
    const nextReason = overrideReasons[record.studentId] ?? "";
    setSavingStudentId(record.studentId);
    setError(null);
    setSuccess(null);
    try {
      await overrideTeacherAttendance(sessionId, record.studentId, nextStatus, nextReason);
      setSuccess(`Attendance updated for ${record.studentName}.`);
      await loadAttendanceView(Number(sectionId), classDate);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update attendance");
    } finally {
      setSavingStudentId(null);
    }
  }

  const rosterView = useMemo(() => {
    if (sessionPayload.records.length > 0) {
      return sessionPayload.records;
    }
    return roster.map((item) => ({
      studentId: item.studentId,
      studentName: item.studentName,
      studentEmail: item.studentEmail,
      attendanceId: null,
      status: null,
      reason: null,
      markedBy: null,
      teacherConfirmed: false,
      markedAt: null,
      updatedAt: null,
      registrationStatus: item.status,
    }));
  }, [roster, sessionPayload.records]);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Attendance</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <>
          <section className="card attendance-live-card">
            <div className="attendance-live-header">
              <div>
                <h3>Attendance Control Center</h3>
                <p className="muted">Open a check-in window, watch live arrivals, and override records when needed.</p>
              </div>
              {selectedSection ? <span className="badge badge-neutral">{selectedSection.subjectCode}</span> : null}
            </div>

            <form className="attendance-open-form" onSubmit={handleOpenSession}>
              <label>
                <span>Section</span>
                <select
                  value={sectionId}
                  onChange={(event) => setSectionId(event.target.value ? Number(event.target.value) : "")}
                >
                  {sections.map((section) => (
                    <option key={section.id} value={section.id}>
                      {section.subjectCode} - {section.subjectName}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Class date</span>
                <input type="date" value={classDate} onChange={(event) => setClassDate(event.target.value)} required />
              </label>

              <label>
                <span>Check-in closes at</span>
                <input
                  type="datetime-local"
                  value={closeAt}
                  onChange={(event) => setCloseAt(event.target.value)}
                  required
                />
              </label>

              <label>
                <span>Mode</span>
                <select
                  value={checkInMode}
                  onChange={(event) => setCheckInMode(event.target.value as "ONE_CLICK" | "CODE")}
                >
                  <option value="ONE_CLICK">One click</option>
                  <option value="CODE">Code</option>
                </select>
              </label>

              {checkInMode === "CODE" ? (
                <label>
                  <span>Check-in code</span>
                  <input
                    value={checkInCode}
                    onChange={(event) => setCheckInCode(event.target.value.toUpperCase())}
                    placeholder="Leave blank to auto-generate"
                  />
                </label>
              ) : null}

              <label className="attendance-checkbox-field">
                <span>Teacher override</span>
                <input
                  type="checkbox"
                  checked={allowTeacherOverride}
                  onChange={(event) => setAllowTeacherOverride(event.target.checked)}
                />
              </label>

              <div className="actions">
                <button type="submit" disabled={!sectionId || submitting}>
                  {submitting ? "Opening..." : sessionPayload.session?.status === "OPEN" ? "Re-open attendance" : "Open attendance"}
                </button>
                {sessionPayload.session?.status === "OPEN" ? (
                  <button type="button" onClick={() => void handleCloseSession()} disabled={closing}>
                    {closing ? "Closing..." : "Close attendance"}
                  </button>
                ) : null}
              </div>
            </form>
          </section>

          <section className="card attendance-session-summary-card">
            <div className="attendance-live-header">
              <div>
                <h3>Session Status</h3>
                <p className="muted">{reloading ? "Refreshing live attendance..." : "This block updates when students check in."}</p>
              </div>
            </div>
            {sessionPayload.session ? (
              <div className="attendance-session-summary-grid">
                <div className="attendance-summary-chip">
                  <span>Status</span>
                  <strong>{sessionPayload.session.status}</strong>
                </div>
                <div className="attendance-summary-chip">
                  <span>Mode</span>
                  <strong>{sessionPayload.session.checkInMode === "CODE" ? "Code" : "One click"}</strong>
                </div>
                <div className="attendance-summary-chip">
                  <span>Closes</span>
                  <strong>{formatTimestamp(sessionPayload.session.attendanceCloseAt)}</strong>
                </div>
                <div className="attendance-summary-chip">
                  <span>Opened</span>
                  <strong>{formatTimestamp(sessionPayload.session.openedAt)}</strong>
                </div>
                <div className="attendance-summary-chip">
                  <span>Check-in code</span>
                  <strong>{sessionPayload.session.checkInCode || "Not required"}</strong>
                </div>
                <div className="attendance-summary-chip">
                  <span>Override</span>
                  <strong>{sessionPayload.session.allowTeacherOverride ? "Enabled" : "Locked"}</strong>
                </div>
              </div>
            ) : (
              <div className="attendance-live-empty muted">No attendance session is open for the selected date yet.</div>
            )}
          </section>

          <section className="card">
            <div className="section-heading">
              <div>
                <h3>Live roster</h3>
                <p className="muted">Students appear here as soon as they check in. You can override any record below.</p>
              </div>
              <span className="badge badge-neutral">{rosterView.length} students</span>
            </div>
            <div className="table-wrap">
              <table className="table attendance-table">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Status</th>
                    <th>Marked by</th>
                    <th>Updated</th>
                    <th>Reason</th>
                    <th>Override</th>
                  </tr>
                </thead>
                <tbody>
                  {rosterView.map((record) => {
                    const recordStatus = "status" in record ? record.status : null;
                    const recordReason = "reason" in record ? record.reason : null;
                    const markedBy = "markedBy" in record ? record.markedBy : null;
                    const updatedAt = "updatedAt" in record ? record.updatedAt : null;
                    return (
                      <tr key={record.studentId}>
                        <td>
                          <div className="teacher-roster-student">
                            <span className="teacher-roster-avatar">
                              {record.studentName
                                .split(" ")
                                .map((part) => part[0])
                                .join("")
                                .slice(0, 2)
                                .toUpperCase()}
                            </span>
                            <div>
                              <strong>{record.studentName}</strong>
                              <p className="muted">{record.studentEmail}</p>
                            </div>
                          </div>
                        </td>
                        <td>{recordStatus || "Waiting"}</td>
                        <td>{markedBy || "-"}</td>
                        <td>{formatTimestamp(updatedAt)}</td>
                        <td>{recordReason || "-"}</td>
                        <td>
                          <div className="attendance-override-grid">
                            <select
                              value={overrideStatuses[record.studentId] ?? (recordStatus || "PRESENT")}
                              onChange={(event) =>
                                setOverrideStatuses((current) => ({
                                  ...current,
                                  [record.studentId]: event.target.value as (typeof attendanceStatuses)[number],
                                }))
                              }
                              disabled={!sessionPayload.session || !sessionPayload.session.allowTeacherOverride}
                            >
                              {attendanceStatuses.map((item) => (
                                <option key={item} value={item}>
                                  {item}
                                </option>
                              ))}
                            </select>
                            <input
                              value={overrideReasons[record.studentId] ?? (recordReason || "")}
                              onChange={(event) =>
                                setOverrideReasons((current) => ({
                                  ...current,
                                  [record.studentId]: event.target.value,
                                }))
                              }
                              placeholder="Optional reason"
                              disabled={!sessionPayload.session || !sessionPayload.session.allowTeacherOverride}
                            />
                            <button
                              type="button"
                              disabled={!sessionPayload.session || !sessionPayload.session.allowTeacherOverride || savingStudentId === record.studentId}
                              onClick={() => void handleOverride(record)}
                            >
                              {savingStudentId === record.studentId ? "Saving..." : "Save"}
                            </button>
                          </div>
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
