import { type FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  createTeacherGradeChangeRequest,
  fetchTeacherGradeChangeRequests,
  fetchTeacherSections
} from "../../lib/api";
import type { TeacherGradeChangeRequestItem, TeacherSectionItem } from "../../types/teacher";

export default function TeacherGradeChangesPage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [sectionId, setSectionId] = useState<number | "">("");
  const [items, setItems] = useState<TeacherGradeChangeRequestItem[]>([]);
  const [gradeId, setGradeId] = useState<number | "">("");
  const [newValue, setNewValue] = useState<number>(0);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function loadInit() {
      setLoading(true);
      setError(null);
      try {
        const [sectionsData, requestsData] = await Promise.all([fetchTeacherSections(), fetchTeacherGradeChangeRequests()]);
        if (!cancelled) {
          setSections(sectionsData);
          setSectionId(sectionsData[0]?.id ?? "");
          setItems(requestsData);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load grade change data");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void loadInit();
    return () => {
      cancelled = true;
    };
  }, []);

  async function refreshRequests() {
    try {
      const data = await fetchTeacherGradeChangeRequests();
      setItems(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to refresh requests");
    }
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId || !gradeId) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await createTeacherGradeChangeRequest(Number(sectionId), Number(gradeId), Number(newValue), reason.trim());
      setReason("");
      setGradeId("");
      setNewValue(0);
      setSuccess("Grade change request submitted");
      await refreshRequests();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create request");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Grade Change Requests</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <>
          <section className="card">
            <h3>Create Request</h3>
            <form className="inline-form" onSubmit={handleCreate}>
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
                Grade ID
                <input
                  type="number"
                  value={gradeId}
                  onChange={(event) => setGradeId(event.target.value ? Number(event.target.value) : "")}
                  required
                />
              </label>
              <label>
                New Value
                <input type="number" step={0.01} value={newValue} onChange={(event) => setNewValue(Number(event.target.value))} required />
              </label>
              <label>
                Reason
                <input value={reason} onChange={(event) => setReason(event.target.value)} required />
              </label>
              <button type="submit" disabled={saving || !sectionId || !gradeId}>
                {saving ? "Saving..." : "Submit"}
              </button>
            </form>
          </section>

          <section className="card">
            <h3>My Grade Change Requests</h3>
            {items.length === 0 ? <p className="muted">No requests.</p> : null}
            {items.length > 0 ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Status</th>
                      <th>Old</th>
                      <th>New</th>
                      <th>Reason</th>
                      <th>Created</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr key={item.id}>
                        <td>{item.id}</td>
                        <td>{item.status}</td>
                        <td>{item.oldValue ?? "-"}</td>
                        <td>{item.newValue ?? "-"}</td>
                        <td>{item.reason}</td>
                        <td>{item.createdAt}</td>
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

