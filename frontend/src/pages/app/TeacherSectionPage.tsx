import { type FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ApiError,
  buildFileDownloadUrl,
  downloadTeacherSectionGrades,
  fetchTeacherComponents,
  fetchTeacherRoster,
  fetchTeacherSections,
  uploadTeacherStudentFile
} from "../../lib/api";
import type {
  TeacherComponentItem,
  TeacherRosterItem,
  TeacherSectionItem,
  TeacherSectionMeetingTimeItem,
  TeacherStudentFileItem
} from "../../types/teacher";

const DAY_LABELS: Record<string, string> = {
  MONDAY: "Mon",
  TUESDAY: "Tue",
  WEDNESDAY: "Wed",
  THURSDAY: "Thu",
  FRIDAY: "Fri",
  SATURDAY: "Sat",
  SUNDAY: "Sun"
};

const DAY_ORDER: Record<string, number> = {
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
  SUNDAY: 7
};

function formatLessonType(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

function formatDay(dayOfWeek: string): string {
  return DAY_LABELS[dayOfWeek] || dayOfWeek;
}

function formatTime(value: string): string {
  return value.length >= 5 ? value.slice(0, 5) : value;
}

function compareMeetingTimes(left: TeacherSectionMeetingTimeItem, right: TeacherSectionMeetingTimeItem): number {
  if (left.dayOfWeek !== right.dayOfWeek) {
    return (DAY_ORDER[left.dayOfWeek] || 99) - (DAY_ORDER[right.dayOfWeek] || 99);
  }
  return left.startTime.localeCompare(right.startTime);
}

function compareRoster(left: TeacherRosterItem, right: TeacherRosterItem): number {
  return left.studentName.localeCompare(right.studentName);
}

export default function TeacherSectionPage() {
  const { sectionId } = useParams();
  const [section, setSection] = useState<TeacherSectionItem | null>(null);
  const [roster, setRoster] = useState<TeacherRosterItem[]>([]);
  const [components, setComponents] = useState<TeacherComponentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedStudentId, setSelectedStudentId] = useState<number | "">("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [lastUploadedFile, setLastUploadedFile] = useState<TeacherStudentFileItem | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      if (!sectionId) {
        setError("Invalid section id");
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const sid = Number(sectionId);
        const [sectionsPayload, rosterPayload, componentsPayload] = await Promise.all([
          fetchTeacherSections(),
          fetchTeacherRoster(sid),
          fetchTeacherComponents(sid)
        ]);

        if (cancelled) return;

        const currentSection = sectionsPayload.find((item) => item.id === sid) || null;
        setSection(currentSection);

        const sortedRoster = [...rosterPayload].sort(compareRoster);
        setRoster(sortedRoster);
        setComponents(componentsPayload);
        setSelectedStudentId(sortedRoster[0]?.studentId ?? "");
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load section");
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
  }, [sectionId]);

  const sortedMeetings = useMemo(
    () => [...(section?.meetingTimes || [])].sort(compareMeetingTimes),
    [section]
  );

  const fillRate = useMemo(() => {
    if (!section || section.capacity <= 0) return 0;
    return Math.round((section.enrolledCount / section.capacity) * 100);
  }, [section]);

  const publishedComponents = useMemo(
    () => components.filter((component) => component.published).length,
    [components]
  );

  const lockedComponents = useMemo(() => components.filter((component) => component.locked).length, [components]);

  async function handleExport() {
    if (!sectionId) return;
    setExporting(true);
    setError(null);
    try {
      const { blob, fileName } = await downloadTeacherSectionGrades(Number(sectionId));
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Export failed");
    } finally {
      setExporting(false);
    }
  }

  async function handleUploadStudentFile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId) return;
    if (!selectedStudentId || !selectedFile) {
      setError("Select student and file before upload");
      return;
    }

    setUploading(true);
    setError(null);
    try {
      const uploaded = await uploadTeacherStudentFile(Number(sectionId), Number(selectedStudentId), selectedFile);
      setLastUploadedFile(uploaded);
      setSelectedFile(null);
      const fileInput = document.getElementById("teacher-student-file-input") as HTMLInputElement | null;
      if (fileInput) {
        fileInput.value = "";
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to upload file");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>{section ? `${section.subjectCode} Section #${section.id}` : `Section #${sectionId}`}</h2>
          <p className="muted">Detailed section view with roster, schedule, components and student file tools.</p>
        </div>
        <div className="actions">
          <button onClick={handleExport} disabled={exporting}>
            {exporting ? "Exporting..." : "Export grades (XLSX)"}
          </button>
          <Link className="link-btn" to="/app/teacher/sections">
            Back to sections
          </Link>
        </div>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error ? (
        <>
          <section className="card teacher-section-hero-card">
            <div className="teacher-section-hero">
              <div className="teacher-section-hero-main">
                <div className="teacher-section-hero-heading">
                  <span className="student-section-kicker">Teacher Section Details</span>
                  <h3>{section?.subjectName || "Section"}</h3>
                  <p className="teacher-section-subtitle">
                    {section?.programName || "General curriculum"}{section?.facultyName ? ` · ${section.facultyName}` : ""}
                  </p>
                </div>

                <div className="teacher-section-chip-row">
                  {section ? (
                    <>
                      <span className="badge">{section.semesterName}</span>
                      <span className="badge">{formatLessonType(section.lessonType)}</span>
                      <span className={`badge teacher-section-status ${section.currentSemester ? "current" : "archive"}`}>
                        {section.currentSemester ? "Current semester" : "Archive"}
                      </span>
                    </>
                  ) : null}
                </div>

                <div className="teacher-section-facts">
                  <div className="profile-fact">
                    <span className="profile-fact-label">Credits</span>
                    <span className="profile-fact-value">{section?.credits ?? "-"}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Capacity</span>
                    <span className="profile-fact-value">{section ? section.capacity : "-"}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Enrolled</span>
                    <span className="profile-fact-value">{section ? section.enrolledCount : roster.length}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Fill rate</span>
                    <span className="profile-fact-value">{section ? `${fillRate}%` : "-"}</span>
                  </div>
                </div>
              </div>

              <aside className="teacher-section-hero-side">
                <h4 className="teacher-section-side-title">Weekly Schedule</h4>
                {sortedMeetings.length === 0 ? (
                  <p className="muted">No meeting times assigned for this section.</p>
                ) : (
                  <div className="teacher-meeting-list">
                    {sortedMeetings.map((meetingTime) => (
                      <div key={meetingTime.id} className="teacher-meeting-item">
                        <div className="teacher-meeting-line">
                          <strong>
                            {formatDay(meetingTime.dayOfWeek)} {formatTime(meetingTime.startTime)}-{formatTime(meetingTime.endTime)}
                          </strong>
                          <span className="badge teacher-meeting-badge">{formatLessonType(meetingTime.lessonType)}</span>
                        </div>
                        <span className="teacher-meeting-room">{meetingTime.room || "Room not assigned"}</span>
                      </div>
                    ))}
                  </div>
                )}
              </aside>
            </div>
          </section>

          <section className="teacher-section-overview-grid">
            <article className="card teacher-section-summary-card">
              <div className="teacher-sections-stats">
                <div className="teacher-sections-stat">
                  <span>Students</span>
                  <strong>{roster.length}</strong>
                </div>
                <div className="teacher-sections-stat">
                  <span>Components</span>
                  <strong>{components.length}</strong>
                </div>
                <div className="teacher-sections-stat">
                  <span>Published</span>
                  <strong>{publishedComponents}</strong>
                </div>
                <div className="teacher-sections-stat">
                  <span>Locked</span>
                  <strong>{lockedComponents}</strong>
                </div>
              </div>
            </article>

            <article className="card teacher-section-upload-card">
              <div className="teacher-section-card-header">
                <div>
                  <h3>Upload File To Student</h3>
                  <p className="muted">Add a file directly to student files from this section.</p>
                </div>
              </div>

              {roster.length === 0 ? <p className="muted">No students in this section.</p> : null}

              {roster.length > 0 ? (
                <form className="teacher-section-upload-form" onSubmit={handleUploadStudentFile}>
                  <label>
                    Student
                    <select
                      value={selectedStudentId}
                      onChange={(event) => setSelectedStudentId(Number(event.target.value))}
                    >
                      {roster.map((item) => (
                        <option key={item.registrationId} value={item.studentId}>
                          {item.studentName} ({item.studentEmail})
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    File
                    <input
                      id="teacher-student-file-input"
                      type="file"
                      onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
                    />
                  </label>
                  <button type="submit" disabled={uploading}>
                    {uploading ? "Uploading..." : "Upload file"}
                  </button>
                </form>
              ) : null}

              {lastUploadedFile ? (
                <div className="teacher-last-upload">
                  <span className="profile-fact-label">Last uploaded file</span>
                  <a href={buildFileDownloadUrl(lastUploadedFile.downloadUrl)} target="_blank" rel="noreferrer">
                    {lastUploadedFile.fileName}
                  </a>
                </div>
              ) : null}
            </article>
          </section>

          <section className="card teacher-section-block">
            <div className="teacher-section-card-header">
              <div>
                <h3>Roster</h3>
                <p className="muted">Students currently linked to this section.</p>
              </div>
            </div>

            {roster.length === 0 ? (
              <p className="muted">No students in roster.</p>
            ) : (
              <div className="table-wrap">
                <table className="table teacher-detail-table">
                  <thead>
                    <tr>
                      <th>Student</th>
                      <th>Email</th>
                      <th>Status</th>
                      <th>Student ID</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roster.map((item) => (
                      <tr key={item.registrationId}>
                        <td>
                          <div className="teacher-roster-student">
                            <div className="teacher-roster-avatar">{item.studentName.slice(0, 1)}</div>
                            <strong>{item.studentName}</strong>
                          </div>
                        </td>
                        <td>{item.studentEmail}</td>
                        <td>
                          <span className={`badge teacher-roster-status ${String(item.status).toLowerCase()}`}>
                            {item.status}
                          </span>
                        </td>
                        <td>{item.studentId}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card teacher-section-block">
            <div className="teacher-section-card-header">
              <div>
                <h3>Assessment Components</h3>
                <p className="muted">Weight, publication state and lock state for this section.</p>
              </div>
            </div>

            {components.length === 0 ? (
              <p className="muted">No components.</p>
            ) : (
              <div className="table-wrap">
                <table className="table teacher-detail-table">
                  <thead>
                    <tr>
                      <th>Component</th>
                      <th>Type</th>
                      <th>Weight</th>
                      <th>Status</th>
                      <th>Published</th>
                      <th>Locked</th>
                    </tr>
                  </thead>
                  <tbody>
                    {components.map((item) => (
                      <tr key={item.id}>
                        <td>
                          <strong>{item.name}</strong>
                        </td>
                        <td>{formatLessonType(item.type)}</td>
                        <td>{item.weightPercent}%</td>
                        <td>{item.status}</td>
                        <td>
                          <span className={`badge ${item.published ? "teacher-badge-yes" : "teacher-badge-no"}`}>
                            {item.published ? "Published" : "Draft"}
                          </span>
                        </td>
                        <td>
                          <span className={`badge ${item.locked ? "teacher-badge-no" : "teacher-badge-yes"}`}>
                            {item.locked ? "Locked" : "Open"}
                          </span>
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
