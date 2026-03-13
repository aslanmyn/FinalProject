import { useEffect, useState } from "react";
import { ApiError, fetchStudentTranscript } from "../../lib/api";
import type { StudentTranscriptData } from "../../types/student";

export default function StudentTranscriptPage() {
  const [data, setData] = useState<StudentTranscriptData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentTranscript();
        if (!cancelled) {
          setData(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load transcript");
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
        <h2>Transcript</h2>
      </header>
      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {!loading && !error && data ? (
        <>
          <section className="card">
            <div className="kv-grid">
              <div>
                <strong>Student:</strong> {data.studentName}
              </div>
              <div>
                <strong>GPA:</strong> {data.gpa.toFixed(2)}
              </div>
              <div>
                <strong>Total Credits:</strong> {data.totalCredits}
              </div>
              <div>
                <strong>Published Final Grades:</strong> {data.finalGrades.length}
              </div>
            </div>
          </section>
          <section className="card">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Name</th>
                    <th>Credits</th>
                    <th>Numeric</th>
                    <th>Letter</th>
                    <th>Points</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {data.finalGrades.map((item) => (
                    <tr key={item.id}>
                      <td>{item.courseCode}</td>
                      <td>{item.courseName}</td>
                      <td>{item.credits}</td>
                      <td>{item.numericValue}</td>
                      <td>{item.letterValue}</td>
                      <td>{item.points}</td>
                      <td>{item.status}</td>
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

