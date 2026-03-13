import { useEffect, useState } from "react";
import { ApiError, fetchStudentSchedule } from "../../lib/api";
import type { StudentScheduleItem } from "../../types/student";

export default function StudentSchedulePage() {
  const [items, setItems] = useState<StudentScheduleItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentSchedule();
        if (!cancelled) {
          setItems(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load schedule");
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
        <h2>Student Schedule</h2>
      </header>
      <section className="card">
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loading && !error && items.length === 0 ? <p className="muted">No schedule items.</p> : null}
        {!loading && !error && items.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Course</th>
                  <th>Name</th>
                  <th>Day</th>
                  <th>Time</th>
                  <th>Room</th>
                  <th>Teacher</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={`${item.sectionId}-${item.courseCode}`}>
                    <td>{item.courseCode}</td>
                    <td>{item.courseName}</td>
                    <td>{item.dayOfWeek || "-"}</td>
                    <td>
                      {item.startTime || "-"} - {item.endTime || "-"}
                    </td>
                    <td>{item.room || "-"}</td>
                    <td>{item.teacherName || "-"}</td>
                    <td>{item.status}</td>
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

