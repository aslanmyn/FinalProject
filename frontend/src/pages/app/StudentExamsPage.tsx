import { useEffect, useState } from "react";
import { ApiError, fetchStudentExamSchedule } from "../../lib/api";
import type { StudentExamScheduleItem } from "../../types/student";

export default function StudentExamsPage() {
  const [items, setItems] = useState<StudentExamScheduleItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentExamSchedule();
        if (!cancelled) {
          setItems(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load exam schedule");
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
        <h2>Exam Schedule</h2>
      </header>
      <section className="card">
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loading && !error && items.length === 0 ? <p className="muted">No exams scheduled.</p> : null}
        {!loading && !error && items.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Course</th>
                  <th>Name</th>
                  <th>Date</th>
                  <th>Time</th>
                  <th>Room</th>
                  <th>Format</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.subjectCode}</td>
                    <td>{item.subjectName}</td>
                    <td>{item.examDate}</td>
                    <td>{item.examTime}</td>
                    <td>{item.room || "-"}</td>
                    <td>{item.format || "-"}</td>
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

