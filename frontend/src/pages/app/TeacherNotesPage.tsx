import { type FormEvent, useEffect, useState } from "react";
import { ApiError, fetchTeacherNotes, fetchTeacherRoster, fetchTeacherSections, upsertTeacherNote } from "../../lib/api";
import type { TeacherNoteItem, TeacherRosterItem, TeacherSectionItem } from "../../types/teacher";

const riskFlags = ["NONE", "LOW_ATTENDANCE", "LOW_GRADES", "COMBINED_RISK"] as const;

export default function TeacherNotesPage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [sectionId, setSectionId] = useState<number | "">("");
  const [roster, setRoster] = useState<TeacherRosterItem[]>([]);
  const [items, setItems] = useState<TeacherNoteItem[]>([]);
  const [studentId, setStudentId] = useState<number | "">("");
  const [note, setNote] = useState("");
  const [riskFlag, setRiskFlag] = useState<(typeof riskFlags)[number]>("NONE");
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
          setSectionId(data[0]?.id ?? "");
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof ApiError ? err.message : "Failed to load sections");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void loadSections();
    return () => {
      cancelled = true;
    };
  }, []);

  async function loadData(targetSectionId: number | "") {
    if (!targetSectionId) {
      setRoster([]);
      setItems([]);
      setStudentId("");
      return;
    }
    try {
      const [r, n] = await Promise.all([fetchTeacherRoster(Number(targetSectionId)), fetchTeacherNotes(Number(targetSectionId))]);
      setRoster(r);
      setItems(n);
      setStudentId(r[0]?.studentId ?? "");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load notes");
    }
  }

  useEffect(() => {
    let cancelled = false;

    async function refreshNotes() {
      if (!sectionId) {
        setRoster([]);
        setItems([]);
        setStudentId("");
        return;
      }
      try {
        const [r, n] = await Promise.all([
          fetchTeacherRoster(Number(sectionId)),
          fetchTeacherNotes(Number(sectionId))
        ]);
        if (!cancelled) {
          setRoster(r);
          setItems(n);
          setStudentId(r[0]?.studentId ?? "");
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load notes");
        }
      }
    }

    void refreshNotes();
    return () => {
      cancelled = true;
    };
  }, [sectionId]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId || !studentId) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await upsertTeacherNote(Number(sectionId), Number(studentId), note.trim(), riskFlag);
      setNote("");
      setRiskFlag("NONE");
      setSuccess("Note saved");
      await loadData(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save note");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Student Notes</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <>
          <section className="card">
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
          </section>

          <section className="card">
            <h3>Add / Update Note</h3>
            <form className="inline-form" onSubmit={handleSave}>
              <label>
                Student
                <select
                  value={studentId}
                  onChange={(event) => setStudentId(event.target.value ? Number(event.target.value) : "")}
                >
                  {roster.map((student) => (
                    <option key={student.registrationId} value={student.studentId}>
                      {student.studentName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Risk Flag
                <select value={riskFlag} onChange={(event) => setRiskFlag(event.target.value as (typeof riskFlags)[number])}>
                  {riskFlags.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Note
                <input value={note} onChange={(event) => setNote(event.target.value)} required />
              </label>
              <button type="submit" disabled={saving || !sectionId || !studentId}>
                {saving ? "Saving..." : "Save Note"}
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Current Notes</h3>
            {items.length === 0 ? <p className="muted">No notes.</p> : null}
            {items.length > 0 ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Student</th>
                      <th>Risk</th>
                      <th>Note</th>
                      <th>Updated</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr key={item.id}>
                        <td>{item.id}</td>
                        <td>{item.student?.name || item.student?.id || "-"}</td>
                        <td>{item.riskFlag}</td>
                        <td>{item.note}</td>
                        <td>{item.updatedAt || item.createdAt}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </section>
        </>
      ) : null}
    </div>
  );
}
