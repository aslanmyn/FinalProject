import { type FormEvent, useEffect, useMemo, useState } from "react";
import { ApiError, fetchTeacherRoster, fetchTeacherSections, markTeacherAttendance } from "../../lib/api";
import type { TeacherRosterItem, TeacherSectionItem } from "../../types/teacher";

const attendanceStatuses = ["PRESENT", "LATE", "ABSENT"] as const;

export default function TeacherAttendancePage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [sectionId, setSectionId] = useState<number | "">("");
  const [roster, setRoster] = useState<TeacherRosterItem[]>([]);
  const [studentId, setStudentId] = useState<number | "">("");
  const [classDate, setClassDate] = useState<string>(new Date().toISOString().slice(0, 10));
  const [status, setStatus] = useState<(typeof attendanceStatuses)[number]>("PRESENT");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function loadSections() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchTeacherSections();
        if (!cancelled) {
          setSections(data);
          const firstId = data[0]?.id ?? "";
          setSectionId(firstId);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load sections");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    void loadSections();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function loadRoster() {
      if (!sectionId) {
        setRoster([]);
        setStudentId("");
        return;
      }
      setError(null);
      try {
        const data = await fetchTeacherRoster(Number(sectionId));
        if (!cancelled) {
          setRoster(data);
          setStudentId(data[0]?.studentId ?? "");
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load roster");
        }
      }
    }
    void loadRoster();
    return () => {
      cancelled = true;
    };
  }, [sectionId]);

  const sectionLabel = useMemo(
    () => sections.find((item) => item.id === sectionId)?.subjectCode || "-",
    [sections, sectionId]
  );

  async function handleMarkSingle(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId || !studentId) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await markTeacherAttendance(Number(sectionId), classDate, [{ studentId: Number(studentId), status, reason }]);
      setSuccess(`Attendance saved for ${sectionLabel}`);
      setReason("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save attendance");
    } finally {
      setSaving(false);
    }
  }

  async function handleMarkAllPresent() {
    if (!sectionId || roster.length === 0) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await markTeacherAttendance(
        Number(sectionId),
        classDate,
        roster.map((student) => ({ studentId: student.studentId, status: "PRESENT" as const, reason: "" }))
      );
      setSuccess(`All students marked PRESENT for ${sectionLabel}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to mark all present");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Attendance</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <section className="card">
          <form className="form" onSubmit={handleMarkSingle}>
            <label>
              Section
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
              Class Date
              <input type="date" value={classDate} onChange={(event) => setClassDate(event.target.value)} required />
            </label>

            <label>
              Student
              <select
                value={studentId}
                onChange={(event) => setStudentId(event.target.value ? Number(event.target.value) : "")}
              >
                {roster.map((student) => (
                  <option key={student.registrationId} value={student.studentId}>
                    {student.studentName} ({student.studentEmail})
                  </option>
                ))}
              </select>
            </label>

            <label>
              Status
              <select value={status} onChange={(event) => setStatus(event.target.value as (typeof attendanceStatuses)[number])}>
                {attendanceStatuses.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Reason
              <input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Optional comment" />
            </label>

            <div className="actions">
              <button type="submit" disabled={saving || !sectionId || !studentId}>
                {saving ? "Saving..." : "Save Attendance"}
              </button>
              <button type="button" disabled={saving || !sectionId || roster.length === 0} onClick={handleMarkAllPresent}>
                Mark All Present
              </button>
            </div>
          </form>
        </section>
      ) : null}
    </div>
  );
}
