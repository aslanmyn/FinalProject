import { useEffect, useState } from "react";
import { ApiError, fetchStudentProfile, fetchStudentSchedule } from "../../lib/api";
import type { StudentProfile, StudentScheduleItem } from "../../types/student";

export default function StudentDashboardPage() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [schedule, setSchedule] = useState<StudentScheduleItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [p, s] = await Promise.all([fetchStudentProfile(), fetchStudentSchedule()]);
        if (!cancelled) {
          setProfile(p);
          setSchedule(s);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load student data");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Student Overview</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error && profile ? (
        <>
          <section className="card">
            <h3>{profile.name}</h3>
            <p className="muted">{profile.email}</p>
            <div className="kv-grid">
              <div>Program: {profile.program || "-"}</div>
              <div>Faculty: {profile.faculty || "-"}</div>
              <div>Course: {profile.course}</div>
              <div>Group: {profile.groupName}</div>
              <div>Status: {profile.status}</div>
              <div>Credits: {profile.creditsEarned}</div>
            </div>
          </section>

          <section className="card">
            <h3>Current Schedule</h3>
            {schedule.length === 0 ? <p className="muted">No active schedule items.</p> : null}
            {schedule.map((item) => (
              <div key={`${item.sectionId}-${item.courseCode}`} className="row">
                <strong>{item.courseCode}</strong> {item.courseName}
                <span className="muted">
                  {" "}
                  | {item.dayOfWeek || "-"} {item.startTime || ""}-{item.endTime || ""} | {item.room || "-"} |{" "}
                  {item.teacherName || "-"}
                </span>
              </div>
            ))}
          </section>
        </>
      ) : null}
    </div>
  );
}

