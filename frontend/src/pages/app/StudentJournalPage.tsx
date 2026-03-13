import { useEffect, useState } from "react";
import { ApiError, fetchStudentJournal } from "../../lib/api";
import type { StudentJournalItem } from "../../types/student";

export default function StudentJournalPage() {
  const [items, setItems] = useState<StudentJournalItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentJournal();
        if (!cancelled) {
          setItems(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load journal");
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
        <h2>Student Journal</h2>
      </header>
      <section className="card">
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loading && !error && items.length === 0 ? <p className="muted">No grades yet.</p> : null}
        {!loading && !error && items.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Course</th>
                  <th>Name</th>
                  <th>Component</th>
                  <th>Value</th>
                  <th>Max</th>
                  <th>Comment</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.courseCode}</td>
                    <td>{item.courseName}</td>
                    <td>{item.component}</td>
                    <td>{item.value}</td>
                    <td>{item.max}</td>
                    <td>{item.comment || "-"}</td>
                    <td>{item.createdAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </div>
  );
}

