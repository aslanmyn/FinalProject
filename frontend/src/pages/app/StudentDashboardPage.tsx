import { ChangeEvent, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  buildFileDownloadUrl,
  fetchStudentPlanner,
  fetchStudentProfile,
  fetchStudentRiskDashboard,
  fetchStudentSchedule,
  fetchStudentWorkflows,
  uploadStudentProfilePhoto
} from "../../lib/api";
import type { WorkflowOverview } from "../../types/common";
import type { StudentPlannerDashboard, StudentProfile, StudentRiskDashboard, StudentScheduleItem } from "../../types/student";

const DAY_ORDER: Record<string, number> = {
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
  SUNDAY: 7
};

const DAY_LABELS: Record<string, string> = {
  MONDAY: "Monday",
  TUESDAY: "Tuesday",
  WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday",
  FRIDAY: "Friday",
  SATURDAY: "Saturday",
  SUNDAY: "Sunday"
};

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean).slice(0, 2);
  if (parts.length === 0) return "ST";
  return parts.map((part) => part[0]?.toUpperCase() || "").join("");
}

function formatDay(dayOfWeek: string | null): string {
  if (!dayOfWeek) return "Day TBA";
  return DAY_LABELS[dayOfWeek] || dayOfWeek;
}

function formatTime(value: string | null): string {
  if (!value) return "--:--";
  return value.slice(0, 5);
}

function formatStatus(status: string): string {
  return status
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function formatRiskLevel(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function sortSchedule(items: StudentScheduleItem[]): StudentScheduleItem[] {
  return [...items].sort((left, right) => {
    const dayDiff = (DAY_ORDER[left.dayOfWeek || ""] || 99) - (DAY_ORDER[right.dayOfWeek || ""] || 99);
    if (dayDiff !== 0) return dayDiff;
    return (left.startTime || "").localeCompare(right.startTime || "");
  });
}

export default function StudentDashboardPage() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [schedule, setSchedule] = useState<StudentScheduleItem[]>([]);
  const [riskDashboard, setRiskDashboard] = useState<StudentRiskDashboard | null>(null);
  const [plannerDashboard, setPlannerDashboard] = useState<StudentPlannerDashboard | null>(null);
  const [workflowOverview, setWorkflowOverview] = useState<WorkflowOverview | null>(null);
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
        const [profilePayload, schedulePayload, riskPayload, plannerPayload, workflowPayload] = await Promise.all([
          fetchStudentProfile(),
          fetchStudentSchedule(),
          fetchStudentRiskDashboard(),
          fetchStudentPlanner(),
          fetchStudentWorkflows()
        ]);
        if (!cancelled) {
          setProfile(profilePayload);
          setSchedule(sortSchedule(schedulePayload));
          setRiskDashboard(riskPayload);
          setPlannerDashboard(plannerPayload);
          setWorkflowOverview(workflowPayload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load student dashboard");
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

  const schedulePreview = useMemo(() => schedule.slice(0, 4), [schedule]);

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
      const updatedProfile = await uploadStudentProfilePhoto(file);
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
        { label: "Program", value: profile.program || "Not assigned" },
        { label: "Faculty", value: profile.faculty || "Not assigned" },
        { label: "Course year", value: `${profile.course}` },
        { label: "Credits earned", value: `${profile.creditsEarned}` },
        ...(profile.phone ? [{ label: "Phone", value: profile.phone }] : []),
        ...(profile.groupName?.trim() ? [{ label: "Group", value: profile.groupName }] : [])
      ]
    : [];

  const photoUrl = profile?.profilePhotoUrl ? buildFileDownloadUrl(profile.profilePhotoUrl) : null;
  const statusClass = profile?.status?.toLowerCase() || "active";

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Student Overview</h2>
        <div className="actions">
          <Link className="link-btn" to="/app/student/planner">
            Open planner
          </Link>
          <Link className="link-btn" to="/app/student/workflows">
            View workflows
          </Link>
          <Link className="link-btn" to="/app/student/schedule">
            View full schedule
          </Link>
          <Link className="link-btn" to="/app/student/files">
            Open files
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
          <section className="card student-hero-card">
            <div className="student-hero">
              <div className="student-avatar-panel">
                <div className="student-avatar">
                  {photoUrl ? (
                    <img className="student-avatar-image" src={photoUrl} alt={profile.name} />
                  ) : (
                    <span className="student-avatar-fallback">{getInitials(profile.name)}</span>
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
                    className="profile-upload-button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                  >
                    {uploading ? "Uploading..." : photoUrl ? "Change photo" : "Add profile photo"}
                  </button>
                  <p className="profile-upload-hint">Use a clear JPG or PNG portrait for your account.</p>
                  {uploadError ? <p className="error">{uploadError}</p> : null}
                  {uploadSuccess ? <p className="success">{uploadSuccess}</p> : null}
                </div>
              </div>

              <div className="student-hero-content">
                <div className="student-status-row">
                  <div className="student-status-line">
                    <p className="student-section-kicker">Student profile</p>
                    <h2>{profile.name}</h2>
                    <p className="student-contact-line">{profile.email}</p>
                  </div>
                  <span className={`student-status-pill ${statusClass}`}>{formatStatus(profile.status)}</span>
                </div>

                <div className="student-profile-grid">
                  {profileFacts.map((fact) => (
                    <div className="profile-fact" key={fact.label}>
                      <span className="profile-fact-label">{fact.label}</span>
                      <strong className="profile-fact-value">{fact.value}</strong>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </section>

          {riskDashboard && plannerDashboard ? (
            <section className="card">
              <div className="stats-grid">
                <div className="stat-card">
                  <strong>{formatRiskLevel(riskDashboard.level)}</strong>
                  <span>Current risk level</span>
                </div>
                <div className="stat-card">
                  <strong>{riskDashboard.publishedGpa.toFixed(2)}</strong>
                  <span>Published GPA</span>
                </div>
                <div className="stat-card">
                  <strong>{riskDashboard.attendanceRate.toFixed(1)}%</strong>
                  <span>Attendance rate</span>
                </div>
                <div className="stat-card">
                  <strong>{workflowOverview?.items.length ?? 0}</strong>
                  <span>Open workflows</span>
                </div>
                <div className="stat-card">
                  <strong>{plannerDashboard.courses.length}</strong>
                  <span>Planner courses</span>
                </div>
              </div>
            </section>
          ) : null}

          <section className="card schedule-summary-card">
            <div className="schedule-summary-header">
              <div>
                <h3>Current Schedule</h3>
                <p className="muted">Only the nearest active classes are shown here.</p>
              </div>
              <Link className="link-btn" to="/app/student/schedule">
                Open full timetable
              </Link>
            </div>

            {schedulePreview.length === 0 ? (
              <p className="muted">No current semester classes found.</p>
            ) : (
              <div className="schedule-summary-grid">
                {schedulePreview.map((item) => (
                  <article className="schedule-summary-item" key={`${item.sectionId}-${item.dayOfWeek}-${item.startTime}`}>
                    <span className="badge schedule-summary-code">{item.courseCode}</span>
                    <h3>{item.courseName}</h3>
                    <p className="schedule-summary-meta">
                      {formatDay(item.dayOfWeek)} | {formatTime(item.startTime)} - {formatTime(item.endTime)}
                    </p>
                    <p className="schedule-summary-meta">{item.room || "Room to be assigned"}</p>
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

