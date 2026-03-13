import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, fetchPublicProfessorById } from "../lib/api";
import type { PublicProfessorProfile } from "../types/public";

export default function ProfessorProfilePage() {
  const { id } = useParams();
  const [data, setData] = useState<PublicProfessorProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      if (!id) {
        setError("Invalid professor id");
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const payload = await fetchPublicProfessorById(Number(id));
        if (!cancelled) {
          setData(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load profile");
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
  }, [id]);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Professor Profile</h2>
        <Link to="/professors">Back</Link>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error && data ? (
        <>
          <section className="card">
            <h3>{data.name}</h3>
            <p className="muted">{data.positionTitle || data.role}</p>
            <p>Department: {data.department || "-"}</p>
            <p>Faculty: {data.faculty || "-"}</p>
            <p>Email: {data.publicEmail || "-"}</p>
            <p>Office: {data.officeRoom || "-"}</p>
            <p>Office hours: {data.officeHours || "-"}</p>
            <p>{data.bio || "No bio provided."}</p>
          </section>

          <section className="card">
            <h3>Current Sections</h3>
            {data.currentSections.length === 0 ? <p className="muted">No current sections.</p> : null}
            {data.currentSections.map((section) => (
              <div key={section.id} className="row">
                <strong>{section.subjectCode}</strong> {section.subjectName}
                <span className="muted">
                  {" "}
                  | {section.lessonType} | {section.dayOfWeek || "-"} {section.startTime || ""}-{section.endTime || ""} | {section.room || "-"}
                </span>
              </div>
            ))}
          </section>

          <section className="card">
            <h3>Public Announcements</h3>
            {data.announcements.length === 0 ? <p className="muted">No announcements.</p> : null}
            {data.announcements.map((a) => (
              <div key={a.id} className="row">
                <strong>{a.title}</strong>
                <span className="muted">
                  {" "}
                  {a.sectionCode ? `(${a.sectionCode})` : ""} |{" "}
                  {a.publishedAt ? new Date(a.publishedAt).toLocaleString() : "-"}
                </span>
                <p>{a.content}</p>
              </div>
            ))}
          </section>
        </>
      ) : null}
    </div>
  );
}

