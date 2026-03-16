import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, buildFileDownloadUrl, fetchPublicProfessors } from "../lib/api";
import type { PublicProfessorListItem } from "../types/public";

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function resolvePhotoUrl(photoUrl: string): string | null {
  if (!photoUrl) return null;
  return buildFileDownloadUrl(photoUrl);
}

export default function ProfessorsPage() {
  const [items, setItems] = useState<PublicProfessorListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchPublicProfessors();
        if (!cancelled) {
          setItems(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load professors");
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
        <div>
          <h2>Professors</h2>
          <p className="muted">Public directory of faculty members, office hours and current teaching information.</p>
        </div>
        <Link className="link-btn" to="/">
          Back
        </Link>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error ? (
        <section className="public-professors-grid">
          {items.map((item) => (
            <article key={item.id} className="card professor-directory-card">
              <div className="professor-directory-top">
                <div className="public-professor-avatar">
                  {resolvePhotoUrl(item.photoUrl) ? (
                    <img
                      src={resolvePhotoUrl(item.photoUrl) || undefined}
                      alt={item.name}
                      className="public-professor-avatar-image"
                    />
                  ) : (
                    <span className="public-professor-avatar-fallback">{getInitials(item.name)}</span>
                  )}
                </div>
                <div className="professor-directory-heading">
                  <span className="student-section-kicker">Faculty Directory</span>
                  <h3>{item.name}</h3>
                  <p className="muted">{item.positionTitle || item.role}</p>
                </div>
              </div>

              <div className="professor-directory-facts">
                <div className="profile-fact">
                  <span className="profile-fact-label">Faculty</span>
                  <span className="profile-fact-value">{item.faculty || "-"}</span>
                </div>
                <div className="profile-fact">
                  <span className="profile-fact-label">Department</span>
                  <span className="profile-fact-value">{item.department || "-"}</span>
                </div>
                <div className="profile-fact">
                  <span className="profile-fact-label">Office</span>
                  <span className="profile-fact-value">{item.officeRoom || "-"}</span>
                </div>
                <div className="profile-fact">
                  <span className="profile-fact-label">Office Hours</span>
                  <span className="profile-fact-value">{item.officeHours || "By appointment"}</span>
                </div>
              </div>

              <div className="professor-directory-footer">
                <div className="professor-directory-contact">
                  <span className="profile-fact-label">Contact</span>
                  <strong>{item.publicEmail || "-"}</strong>
                </div>
                <Link className="link-btn" to={`/professors/${item.id}`}>
                  Open profile
                </Link>
              </div>
            </article>
          ))}
        </section>
      ) : null}
    </div>
  );
}
