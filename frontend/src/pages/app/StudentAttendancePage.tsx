import { useEffect, useState } from "react";
import { ApiError, fetchStudentAttendance } from "../../lib/api";
import type { StudentAttendanceData } from "../../types/student";

export default function StudentAttendancePage() {
  const [data, setData] = useState<StudentAttendanceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentAttendance();
        if (!cancelled) {
          setData(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load attendance");
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
        <h2>Attendance</h2>
      </header>
      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {!loading && !error && data ? (
        <>
          <section className="card">
            <div className="stats-grid">
              <div className="stat-card">
                <strong>{data.summary.present}</strong>
                <span>Present</span>
              </div>
              <div className="stat-card">
                <strong>{data.summary.late}</strong>
                <span>Late</span>
              </div>
              <div className="stat-card">
                <strong>{data.summary.absent}</strong>
                <span>Absent</span>
              </div>
              <div className="stat-card">
                <strong>{data.summary.total}</strong>
                <span>Total</span>
              </div>
              <div className="stat-card">
                <strong>{data.summary.percentage.toFixed(1)}%</strong>
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
                  {data.records.map((item, idx) => (
                    <tr key={`${item.date}-${item.subjectCode}-${idx}`}>
                      <td>{item.date}</td>
                      <td>{item.subjectCode}</td>
                      <td>{item.subjectName}</td>
                      <td>{item.status}</td>
                      <td>{item.reason || "-"}</td>
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

