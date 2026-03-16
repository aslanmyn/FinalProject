import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchTeacherSections } from "../../lib/api";
import type { TeacherSectionItem } from "../../types/teacher";

function compareSections(left: TeacherSectionItem, right: TeacherSectionItem): number {
  if (left.semesterName !== right.semesterName) {
    return right.semesterName.localeCompare(left.semesterName);
  }
  return left.subjectCode.localeCompare(right.subjectCode);
}

function formatLessonType(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

function extractAcademicYear(semesterName: string): string {
  const [year] = semesterName.split(" ");
  return year || semesterName;
}

export default function TeacherSectionsPage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchTeacherSections();
        if (!cancelled) {
          setSections(payload.sort(compareSections));
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load sections");
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

  const latestAcademicYear = useMemo(
    () => (sections[0] ? extractAcademicYear(sections[0].semesterName) : ""),
    [sections]
  );
  const currentCount = useMemo(
    () =>
      latestAcademicYear
        ? sections.filter((section) => extractAcademicYear(section.semesterName) === latestAcademicYear).length
        : 0,
    [latestAcademicYear, sections]
  );

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Teacher Sections</h2>
          <p className="muted">A structured list of all assigned teaching sections.</p>
        </div>
      </header>

      {loading ? <p>Loading sections...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error ? (
        <>
          <section className="card">
            <div className="stats-grid">
              <div className="stat-card">
                <strong>{sections.length}</strong>
                <span>Total Sections</span>
              </div>
              <div className="stat-card">
                <strong>{currentCount}</strong>
                <span>{latestAcademicYear || "Latest"} Snapshot</span>
              </div>
            </div>
          </section>

          <section className="card">
            {sections.length === 0 ? (
              <p className="muted">No sections assigned yet.</p>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Course</th>
                      <th>Name</th>
                      <th>Semester</th>
                      <th>Type</th>
                      <th>Capacity</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sections.map((section) => (
                      <tr key={section.id}>
                        <td>{section.subjectCode}</td>
                        <td>{section.subjectName}</td>
                        <td>{section.semesterName}</td>
                        <td>{formatLessonType(section.lessonType)}</td>
                        <td>{section.capacity}</td>
                        <td>
                          <Link to={`/app/teacher/sections/${section.id}`}>Open details</Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}
