import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchTeacherProfile, fetchTeacherSections } from "../../lib/api";
import type { TeacherProfile, TeacherSectionItem } from "../../types/teacher";

export default function TeacherDashboardPage() {
  const [profile, setProfile] = useState<TeacherProfile | null>(null);
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [p, s] = await Promise.all([fetchTeacherProfile(), fetchTeacherSections()]);
        if (!cancelled) {
          setProfile(p);
          setSections(s);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load teacher data");
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
        <h2>Teacher Overview</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error && profile ? (
        <>
          <section className="card">
            <h3>{profile.name}</h3>
            <p className="muted">{profile.email}</p>
            <div className="kv-grid">
              <div>Department: {profile.department || "-"}</div>
              <div>Position: {profile.position || "-"}</div>
              <div>Office: {profile.officeRoom || "-"}</div>
              <div>Office hours: {profile.officeHours || "-"}</div>
            </div>
          </section>

          <section className="card">
            <h3>My Sections</h3>
            {sections.length === 0 ? <p className="muted">No assigned sections.</p> : null}
            {sections.map((section) => (
              <div key={section.id} className="row">
                <strong>{section.subjectCode}</strong> {section.subjectName}
                <span className="muted"> | {section.semesterName} | {section.lessonType}</span>
                <p>
                  <Link to={`/app/teacher/sections/${section.id}`}>Open section details</Link>
                </p>
              </div>
            ))}
          </section>
        </>
      ) : null}
    </div>
  );
}

