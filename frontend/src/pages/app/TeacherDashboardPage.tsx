import { ChangeEvent, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  buildFileDownloadUrl,
  fetchTeacherProfile,
  fetchTeacherSections,
  uploadTeacherProfilePhoto
} from "../../lib/api";
import type { TeacherProfile, TeacherSectionItem } from "../../types/teacher";

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean).slice(0, 2);
  if (parts.length === 0) return "PR";
  return parts.map((part) => part[0]?.toUpperCase() || "").join("");
}

function formatRole(role: string): string {
  return role === "TA" ? "Teaching Assistant" : "Professor";
}

function formatLessonType(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

function compareSections(left: TeacherSectionItem, right: TeacherSectionItem): number {
  if (left.semesterName !== right.semesterName) {
    return right.semesterName.localeCompare(left.semesterName);
  }
  return left.subjectCode.localeCompare(right.subjectCode);
}

function extractAcademicYear(semesterName: string): string {
  const [year] = semesterName.split(" ");
  return year || semesterName;
}

export default function TeacherDashboardPage() {
  const [profile, setProfile] = useState<TeacherProfile | null>(null);
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploadSuccess, setUploadSuccess] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [profilePayload, sectionsPayload] = await Promise.all([fetchTeacherProfile(), fetchTeacherSections()]);
        if (!cancelled) {
          setProfile(profilePayload);
          setSections(sectionsPayload);
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

  const sortedSections = useMemo(() => [...sections].sort(compareSections), [sections]);
  const latestAcademicYear = useMemo(
    () => (sortedSections[0] ? extractAcademicYear(sortedSections[0].semesterName) : ""),
    [sortedSections]
  );
  const currentSections = useMemo(
    () =>
      (latestAcademicYear
        ? sortedSections.filter((section) => extractAcademicYear(section.semesterName) === latestAcademicYear)
        : sortedSections
      ).slice(0, 4),
    [latestAcademicYear, sortedSections]
  );

  async function handleProfilePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setUploadSuccess(null);
      setUploadError("Please select an image file.");
      return;
    }

    setUploading(true);
    setUploadError(null);
    setUploadSuccess(null);
    try {
      const updatedProfile = await uploadTeacherProfilePhoto(file);
      setProfile(updatedProfile);
      setUploadSuccess("Profile photo updated.");
    } catch (err) {
      setUploadError(err instanceof ApiError ? err.message : "Failed to upload profile photo");
    } finally {
      setUploading(false);
    }
  }

  const profileFacts = profile
    ? [
        { label: "Department", value: profile.department || "Not assigned" },
        { label: "Faculty", value: profile.faculty || "Not assigned" },
        { label: "Position", value: profile.position || formatRole(profile.teacherRole) },
        { label: "Office", value: profile.officeRoom || "Office pending" },
        { label: "Office hours", value: profile.officeHours || "Not published" }
      ]
    : [];

  const profilePhotoUrl = profile?.profilePhotoUrl ? buildFileDownloadUrl(profile.profilePhotoUrl) : null;

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Overview</h2>
        <div className="actions">
          <Link className="link-btn" to="/app/teacher/sections">
            Open sections table
          </Link>
          <Link className="link-btn" to="/app/teacher/gradebook">
            Open gradebook
          </Link>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading dashboard...</p>
        </section>
      ) : null}
      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      {!loading && !error && profile ? (
        <>
          <section className="card teacher-hero-card">
            <div className="teacher-hero">
              <div className="teacher-avatar-panel">
                <div className="teacher-avatar">
                  {profilePhotoUrl ? (
                    <img className="teacher-avatar-image" src={profilePhotoUrl} alt={profile.name} />
                  ) : (
                    <span className="teacher-avatar-fallback">{getInitials(profile.name)}</span>
                  )}
                </div>

                <div className="profile-upload-control">
                  <input
                    ref={fileInputRef}
                    className="profile-upload-input"
                    type="file"
                    accept="image/*"
                    onChange={handleProfilePhotoChange}
                  />
                  <button
                    type="button"
                    className="profile-upload-button teacher-upload-button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                  >
                    {uploading ? "Uploading..." : profilePhotoUrl ? "Change photo" : "Add profile photo"}
                  </button>
                  <p className="profile-upload-hint">Upload a professional profile image for your teaching cabinet.</p>
                  {uploadError ? <p className="error">{uploadError}</p> : null}
                  {uploadSuccess ? <p className="success">{uploadSuccess}</p> : null}
                </div>
              </div>

              <div className="teacher-hero-content">
                <div className="teacher-status-row">
                  <div className="student-status-line">
                    <p className="student-section-kicker">Teaching profile</p>
                    <h2>{profile.name}</h2>
                    <p className="student-contact-line">{profile.email}</p>
                  </div>
                  <span className="teacher-role-pill">{formatRole(profile.teacherRole)}</span>
                </div>

                <div className="teacher-profile-grid">
                  {profileFacts.map((fact) => (
                    <div className="profile-fact" key={fact.label}>
                      <span className="profile-fact-label">{fact.label}</span>
                      <strong className="profile-fact-value">{fact.value}</strong>
                    </div>
                  ))}
                </div>

                <div className="teacher-quick-stats">
                  <div className="teacher-mini-stat">
                    <span className="profile-fact-label">All sections</span>
                    <strong>{sections.length}</strong>
                  </div>
                  <div className="teacher-mini-stat">
                    <span className="profile-fact-label">{latestAcademicYear || "Latest"} snapshot</span>
                    <strong>{currentSections.length}</strong>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section className="card schedule-summary-card">
            <div className="schedule-summary-header">
              <div>
                <h3>Section Snapshot</h3>
                <p className="muted">Quick access to your latest teaching sections.</p>
              </div>
              <Link className="link-btn" to="/app/teacher/sections">
                View all sections
              </Link>
            </div>

            {sections.length === 0 ? (
              <p className="muted">No assigned sections.</p>
            ) : (
              <div className="schedule-summary-grid">
                {(currentSections.length ? currentSections : sortedSections.slice(0, 4)).map((section) => (
                  <article className="schedule-summary-item" key={section.id}>
                    <span className="badge schedule-summary-code">{section.subjectCode}</span>
                    <h3>{section.subjectName}</h3>
                    <p className="schedule-summary-meta">{section.semesterName}</p>
                    <p className="schedule-summary-meta">
                      {formatLessonType(section.lessonType)} • Capacity {section.capacity}
                    </p>
                    <Link className="link-btn" to={`/app/teacher/sections/${section.id}`}>
                      Open details
                    </Link>
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
