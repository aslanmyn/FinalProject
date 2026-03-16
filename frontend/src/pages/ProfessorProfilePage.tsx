import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, buildFileDownloadUrl, fetchPublicProfessorById } from "../lib/api";
import type { PublicProfessorProfile } from "../types/public";

const DAY_LABELS: Record<string, string> = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun"
};

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

function formatDay(value: string | null): string {
  if (!value) return "TBA";
  return DAY_LABELS[value] || value;
}

function formatTime(value: string | null): string {
  if (!value) return "--:--";
  return value.length >= 5 ? value.slice(0, 5) : value;
}

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
        <div>
          <h2>Professor Profile</h2>
          <p className="muted">Public teaching profile with current sections, office hours and announcements.</p>
        </div>
        <Link className="link-btn" to="/professors">
          Back
        </Link>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error && data ? (
        <>
          <section className="card public-professor-hero-card">
            <div className="public-professor-hero">
              <aside className="public-professor-hero-aside">
                <div className="public-professor-avatar public-professor-avatar-large">
                  {resolvePhotoUrl(data.photoUrl) ? (
                    <img
                      src={resolvePhotoUrl(data.photoUrl) || undefined}
                      alt={data.name}
                      className="public-professor-avatar-image"
                    />
                  ) : (
                    <span className="public-professor-avatar-fallback">{getInitials(data.name)}</span>
                  )}
                </div>
                <div className="public-professor-role-block">
                  <span className="student-section-kicker">Public Faculty Profile</span>
                  <strong>{data.positionTitle || data.role}</strong>
                  <span>{data.faculty || "-"}</span>
                </div>
              </aside>

              <div className="public-professor-hero-content">
                <div className="teacher-section-hero-heading">
                  <h3>{data.name}</h3>
                  <p className="teacher-section-subtitle">{data.department || "Department not specified"}</p>
                </div>

                <div className="teacher-section-chip-row">
                  <span className="badge">{data.positionTitle || data.role}</span>
                  {data.faculty ? <span className="badge">{data.faculty}</span> : null}
                </div>

                <div className="teacher-section-facts">
                  <div className="profile-fact">
                    <span className="profile-fact-label">Email</span>
                    <span className="profile-fact-value">{data.publicEmail || "-"}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Office</span>
                    <span className="profile-fact-value">{data.officeRoom || "-"}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Office Hours</span>
                    <span className="profile-fact-value">{data.officeHours || "By appointment"}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Current Sections</span>
                    <span className="profile-fact-value">{data.currentSections.length}</span>
                  </div>
                </div>

                <div className="public-professor-bio-card">
                  <span className="profile-fact-label">About</span>
                  <p>{data.bio || "No public bio provided yet."}</p>
                </div>
              </div>
            </div>
          </section>

          <section className="card public-professor-section-card">
            <div className="teacher-section-card-header">
              <div>
                <h3>Current Sections</h3>
                <p className="muted">Current teaching load with semester, lesson type and exact time.</p>
              </div>
            </div>

            {data.currentSections.length === 0 ? (
              <p className="muted">No current sections.</p>
            ) : (
              <div className="public-professor-sections-grid">
                {data.currentSections.map((section) => (
                  <article key={section.id} className="public-professor-section-item">
                    <div className="public-professor-section-top">
                      <span className="badge">{section.subjectCode}</span>
                      <span className="badge">{section.semesterName}</span>
                    </div>
                    <h4>{section.subjectName}</h4>
                    <div className="public-professor-section-meta">
                      <span>{formatDay(section.dayOfWeek)}</span>
                      <span>
                        {formatTime(section.startTime)}-{formatTime(section.endTime)}
                      </span>
                      <span>{section.room || "Room TBA"}</span>
                    </div>
                    <span className="public-professor-section-type">{section.lessonType}</span>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="card public-professor-announcements-card">
            <div className="teacher-section-card-header">
              <div>
                <h3>Public Announcements</h3>
                <p className="muted">Recent public notices shared by this professor.</p>
              </div>
            </div>

            {data.announcements.length === 0 ? (
              <p className="muted">No announcements.</p>
            ) : (
              <div className="public-professor-announcements-list">
                {data.announcements.map((announcement) => (
                  <article key={announcement.id} className="public-professor-announcement">
                    <div className="public-professor-announcement-top">
                      <div>
                        <h4>{announcement.title}</h4>
                        <p className="muted">
                          {announcement.sectionCode ? `${announcement.sectionCode} · ` : ""}
                          {announcement.publishedAt
                            ? new Date(announcement.publishedAt).toLocaleString()
                            : "Not published yet"}
                        </p>
                      </div>
                      {announcement.pinned ? <span className="badge">Pinned</span> : null}
                    </div>
                    <p>{announcement.content}</p>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}
