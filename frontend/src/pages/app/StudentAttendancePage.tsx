import { useEffect, useMemo, useState } from "react";
import { ApiError, fetchStudentAttendance } from "../../lib/api";
import type { StudentAttendanceData, StudentAttendanceRecordItem } from "../../types/student";

function buildSummary(records: StudentAttendanceRecordItem[]) {
  const present = records.filter((item) => item.status === "PRESENT").length;
  const late = records.filter((item) => item.status === "LATE").length;
  const absent = records.filter((item) => item.status === "ABSENT").length;
  const total = records.length;
  const percentage = total === 0 ? 0 : ((present + late) * 100) / total;

  return { present, late, absent, total, percentage };
}

export default function StudentAttendancePage() {
  const [data, setData] = useState<StudentAttendanceData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCourseCode, setSelectedCourseCode] = useState("ALL");

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

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Attendance</h2>
      </header>
      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {!loading && !error && data ? (
        <>
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
                  {selectedCourseCode === "ALL"
                    ? "All subjects"
                    : `${selectedCourseCode} attendance`}
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
