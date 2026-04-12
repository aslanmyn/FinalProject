import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, buildFileDownloadUrl, fetchStudentSectionDetail } from "../../lib/api";
import type {
  StudentSectionAnnouncement,
  StudentSectionAttendanceRecord,
  StudentSectionComponentGrade,
  StudentSectionDetail,
  StudentSectionMeetingSlot
} from "../../types/student";

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

function humanizeToken(value: string | null): string {
  if (!value) return "-";
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDay(dayOfWeek: string | null): string {
  if (!dayOfWeek) return "Day TBA";
  return DAY_LABELS[dayOfWeek] || humanizeToken(dayOfWeek);
}

function formatTime(value: string | null): string {
  if (!value) return "--:--";
  return value.length >= 5 ? value.slice(0, 5) : value;
}

function formatDate(value: string | null): string {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value.slice(0, 10);
  }
  return parsed.toLocaleDateString();
}

function formatDateTime(value: string | null): string {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value.replace("T", " ").slice(0, 16);
  }
  return parsed.toLocaleString();
}

function formatScore(value: number | null, max: number | null): string {
  if (value === null || value === undefined) return "-";
  if (max === null || max === undefined) return value.toFixed(2);
  return `${value.toFixed(2)} / ${max.toFixed(0)}`;
}

function formatNumber(value: number | null): string {
  if (value === null || value === undefined) return "-";
  return value.toFixed(2);
}

function compareMeetingSlots(left: StudentSectionMeetingSlot, right: StudentSectionMeetingSlot): number {
  const leftDay = left.dayOfWeek ? DAY_ORDER[left.dayOfWeek] || 99 : 99;
  const rightDay = right.dayOfWeek ? DAY_ORDER[right.dayOfWeek] || 99 : 99;
  if (leftDay !== rightDay) {
    return leftDay - rightDay;
  }
  return formatTime(left.startTime).localeCompare(formatTime(right.startTime));
}

function compareAttendanceRecords(left: StudentSectionAttendanceRecord, right: StudentSectionAttendanceRecord): number {
  return right.date.localeCompare(left.date);
}

function compareAnnouncements(left: StudentSectionAnnouncement, right: StudentSectionAnnouncement): number {
  if (left.pinned !== right.pinned) {
    return left.pinned ? -1 : 1;
  }
  return (right.publishedAt || "").localeCompare(left.publishedAt || "");
}

function getAttendanceBadgeClass(status: string): string {
  if (status === "PRESENT") return "badge";
  if (status === "LATE") return "badge badge-warning";
  if (status === "ABSENT") return "badge badge-danger";
  return "badge badge-neutral";
}

function getAccessBadgeClass(activeCourseAccess: boolean): string {
  return activeCourseAccess ? "badge" : "badge badge-warning";
}

export default function StudentSectionDetailPage() {
  const { sectionId } = useParams();
  const [detail, setDetail] = useState<StudentSectionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      const numericSectionId = Number(sectionId);
      if (!sectionId || Number.isNaN(numericSectionId)) {
        setError("Invalid section id");
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentSectionDetail(numericSectionId);
        if (!cancelled) {
          setDetail(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load course detail");
          setDetail(null);
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
    () => [...(detail?.meetingTimes || [])].sort(compareMeetingSlots),
    [detail]
  );

  const sortedAttendance = useMemo(
    () => [...(detail?.attendanceRecords || [])].sort(compareAttendanceRecords),
    [detail]
  );

  const sortedAnnouncements = useMemo(
    () => [...(detail?.announcements || [])].sort(compareAnnouncements),
    [detail]
  );

  const sortedComponents = useMemo(
    () =>
      [...(detail?.componentGrades || [])].sort((left: StudentSectionComponentGrade, right: StudentSectionComponentGrade) =>
        (left.componentName || "").localeCompare(right.componentName || "")
      ),
    [detail]
  );

  const sortedMaterials = useMemo(
    () =>
      [...(detail?.materials || [])].sort((left, right) => (right.createdAt || "").localeCompare(left.createdAt || "")),
    [detail]
  );

  return (
    <div className="screen app-screen student-course-detail-page">
      <header className="topbar">
        <div>
          <h2>{detail ? `${detail.subjectCode} section #${detail.sectionId}` : `Section #${sectionId}`}</h2>
          <p className="muted">Everything for this course in one place: scores, attendance, exam, announcements, and files.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/student/registration">
            Registration
          </Link>
          <Link className="link-btn" to="/app/student/journal">
            Journal
          </Link>
          <Link className="link-btn" to="/app/student/enrollments">
            Back to enrollments
          </Link>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading course detail...</p>
        </section>
      ) : null}

      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      {!loading && !error && detail ? (
        <>
          <section className="card student-course-hero-card">
            <div className="student-course-hero">
              <div className="student-course-hero-main">
                <div className="teacher-section-hero-heading">
                  <span className="student-section-kicker">Student Course Detail</span>
                  <h3>{detail.subjectName}</h3>
                  <p className="student-course-subtitle">
                    {detail.subjectCode}
                    {detail.semesterName ? ` | ${detail.semesterName}` : ""}
                    {!detail.semesterName && (detail.academicYear || detail.season)
                      ? ` | ${[detail.academicYear, detail.season].filter(Boolean).join(" ")}`
                      : ""}
                  </p>
                </div>

                <div className="student-course-chip-row">
                  <span className="badge">Section #{detail.sectionId}</span>
                  <span className="badge badge-neutral">{humanizeToken(detail.registrationStatus)}</span>
                  <span className={getAccessBadgeClass(detail.activeCourseAccess)}>
                    {detail.activeCourseAccess ? "Course access open" : "Limited course access"}
                  </span>
                  {detail.finalGrade?.letterValue ? <span className="badge">{detail.finalGrade.letterValue}</span> : null}
                </div>

                <div className="student-course-facts">
                  <div className="profile-fact">
                    <span className="profile-fact-label">Credits</span>
                    <span className="profile-fact-value">{detail.credits}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Teacher</span>
                    <span className="profile-fact-value">{detail.teacher?.teacherName || "TBA"}</span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Seats</span>
                    <span className="profile-fact-value">
                      {detail.occupiedSeats}/{detail.capacity}
                    </span>
                  </div>
                  <div className="profile-fact">
                    <span className="profile-fact-label">Attendance</span>
                    <span className="profile-fact-value">{detail.attendanceSummary.percentage.toFixed(1)}%</span>
                  </div>
                </div>

                {!detail.activeCourseAccess && detail.contentBlockedReason ? (
                  <div className="student-course-banner">
                    <div>
                      <strong>Course content is partially locked.</strong>
                      <p className="muted compact-text">{detail.contentBlockedReason}</p>
                    </div>
                    <span className="badge badge-warning">Read-only access</span>
                  </div>
                ) : null}
              </div>

              <aside className="student-course-hero-side">
                <div className="student-course-side-card">
                  <h4 className="teacher-section-side-title">Weekly Schedule</h4>
                  {sortedMeetings.length === 0 ? (
                    <p className="muted">No meeting times published yet.</p>
                  ) : (
                    <div className="teacher-meeting-list">
                      {sortedMeetings.map((slot, index) => (
                        <div key={`${detail.sectionId}-${index}`} className="teacher-meeting-item">
                          <div className="teacher-meeting-line">
                            <strong>
                              {formatDay(slot.dayOfWeek)} {formatTime(slot.startTime)}-{formatTime(slot.endTime)}
                            </strong>
                            {slot.lessonType ? (
                              <span className="badge teacher-meeting-badge">{humanizeToken(slot.lessonType)}</span>
                            ) : null}
                          </div>
                          <span className="teacher-meeting-room">{slot.room || "Room not assigned"}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="student-course-side-card">
                  <h4 className="teacher-section-side-title">Exam & Check-In</h4>
                  {detail.exam ? (
                    <div className="student-course-feed-item">
                      <strong>{formatDate(detail.exam.examDate)}</strong>
                      <span className="student-course-feed-meta">
                        {detail.exam.examTime} {detail.exam.room ? `| ${detail.exam.room}` : ""}
                      </span>
                      <span className="student-course-feed-meta">{detail.exam.format || "Format not specified"}</span>
                    </div>
                  ) : (
                    <p className="muted compact-text">No exam schedule published yet.</p>
                  )}

                  {detail.activeAttendanceSessions.length > 0 ? (
                    <div className="student-course-feed-list">
                      {detail.activeAttendanceSessions.map((session) => (
                        <div key={session.sessionId} className="student-course-feed-item">
                          <strong>Attendance session is open</strong>
                          <span className="student-course-feed-meta">
                            {formatDate(session.classDate)} | {humanizeToken(session.checkInMode)}
                          </span>
                          <span className="student-course-feed-meta">
                            Status: {session.currentStatus ? humanizeToken(session.currentStatus) : "Pending"}
                          </span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="muted compact-text">No live attendance sessions right now.</p>
                  )}
                </div>
              </aside>
            </div>
          </section>

          <section className="student-course-overview-grid">
            <article className="card student-course-summary-card">
              <div className="section-heading">
                <div>
                  <h3>Score Summary</h3>
                  <p className="muted">Published score snapshot for this section.</p>
                </div>
              </div>
              <div className="student-course-summary-stats">
                <div className="student-course-summary-stat">
                  <span>Attestation 1</span>
                  <strong>{formatScore(detail.scoreSummary.attestation1, detail.scoreSummary.attestation1Max)}</strong>
                </div>
                <div className="student-course-summary-stat">
                  <span>Attestation 2</span>
                  <strong>{formatScore(detail.scoreSummary.attestation2, detail.scoreSummary.attestation2Max)}</strong>
                </div>
                <div className="student-course-summary-stat">
                  <span>Final Exam</span>
                  <strong>{formatScore(detail.scoreSummary.finalExam, detail.scoreSummary.finalExamMax)}</strong>
                </div>
                <div className="student-course-summary-stat">
                  <span>Total / Letter</span>
                  <strong>
                    {formatNumber(detail.scoreSummary.totalScore)}
                    {detail.scoreSummary.letterValue ? ` | ${detail.scoreSummary.letterValue}` : ""}
                  </strong>
                </div>
              </div>
            </article>

            <article className="card student-course-summary-card">
              <div className="section-heading">
                <div>
                  <h3>Attendance Summary</h3>
                  <p className="muted">Attendance stats across all published class sessions.</p>
                </div>
              </div>
              <div className="student-course-summary-stats">
                <div className="student-course-summary-stat">
                  <span>Present</span>
                  <strong>{detail.attendanceSummary.present}</strong>
                </div>
                <div className="student-course-summary-stat">
                  <span>Late</span>
                  <strong>{detail.attendanceSummary.late}</strong>
                </div>
                <div className="student-course-summary-stat">
                  <span>Absent</span>
                  <strong>{detail.attendanceSummary.absent}</strong>
                </div>
                <div className="student-course-summary-stat">
                  <span>Rate</span>
                  <strong>{detail.attendanceSummary.percentage.toFixed(1)}%</strong>
                </div>
              </div>
            </article>
          </section>

          <section className="card student-course-data-card">
            <div className="section-heading">
              <div>
                <h3>Assessment Components</h3>
                <p className="muted">Every published component score tied to this section.</p>
              </div>
            </div>

            {sortedComponents.length === 0 ? (
              <p className="muted">No component grades published yet.</p>
            ) : (
              <div className="table-wrap">
                <table className="table student-course-detail-table">
                  <thead>
                    <tr>
                      <th>Component</th>
                      <th>Type</th>
                      <th>Weight</th>
                      <th>Grade</th>
                      <th>Status</th>
                      <th>Comment</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedComponents.map((component, index) => (
                      <tr key={`${component.componentId ?? "component"}-${index}`}>
                        <td>
                          <strong>{component.componentName || "Unnamed component"}</strong>
                        </td>
                        <td>{humanizeToken(component.componentType)}</td>
                        <td>{component.weightPercent !== null ? `${component.weightPercent}%` : "-"}</td>
                        <td>{formatScore(component.gradeValue, component.maxGradeValue)}</td>
                        <td>
                          <div className="student-course-table-stack">
                            <span className={component.componentPublished ? "badge" : "badge badge-neutral"}>
                              {component.componentPublished ? "Published" : "Draft"}
                            </span>
                            <span className={component.componentLocked ? "badge badge-warning" : "badge badge-neutral"}>
                              {component.componentLocked ? "Locked" : "Editable"}
                            </span>
                          </div>
                        </td>
                        <td>{component.comment || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card student-course-data-card">
            <div className="section-heading">
              <div>
                <h3>Attendance Records</h3>
                <p className="muted">Detailed attendance history for this section.</p>
              </div>
            </div>

            {sortedAttendance.length === 0 ? (
              <p className="muted">No attendance records found yet.</p>
            ) : (
              <div className="table-wrap">
                <table className="table student-course-detail-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Status</th>
                      <th>Teacher Confirmed</th>
                      <th>Marked By</th>
                      <th>Marked At</th>
                      <th>Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedAttendance.map((record) => (
                      <tr key={record.attendanceId}>
                        <td>{formatDate(record.sessionDate || record.date)}</td>
                        <td>
                          <span className={getAttendanceBadgeClass(record.status)}>{humanizeToken(record.status)}</span>
                        </td>
                        <td>{record.teacherConfirmed ? "Yes" : "No"}</td>
                        <td>{record.markedBy || "-"}</td>
                        <td>{formatDateTime(record.markedAt || record.updatedAt)}</td>
                        <td>{record.reason || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="student-course-feed-grid">
            <article className="card student-course-feed-card">
              <div className="section-heading">
                <div>
                  <h3>Announcements</h3>
                  <p className="muted">Latest teacher messages for this section.</p>
                </div>
              </div>
              {sortedAnnouncements.length === 0 ? (
                <p className="muted">No announcements published yet.</p>
              ) : (
                <div className="student-course-feed-list">
                  {sortedAnnouncements.map((announcement) => (
                    <article key={announcement.id} className="student-course-feed-item">
                      <div className="student-course-feed-head">
                        <strong>{announcement.title}</strong>
                        {announcement.pinned ? <span className="badge badge-warning">Pinned</span> : null}
                      </div>
                      <p>{announcement.content}</p>
                      <span className="student-course-feed-meta">
                        {announcement.teacherName || "Teacher"} | {formatDateTime(announcement.publishedAt)}
                      </span>
                    </article>
                  ))}
                </div>
              )}
            </article>

            <article className="card student-course-feed-card">
              <div className="section-heading">
                <div>
                  <h3>Materials</h3>
                  <p className="muted">Shared course files available to you.</p>
                </div>
              </div>
              {sortedMaterials.length === 0 ? (
                <p className="muted">No materials uploaded yet.</p>
              ) : (
                <div className="student-course-feed-list">
                  {sortedMaterials.map((material) => (
                    <article key={material.id} className="student-course-feed-item">
                      <strong>{material.title}</strong>
                      {material.description ? <p>{material.description}</p> : null}
                      <span className="student-course-feed-meta">
                        {material.originalFileName} | {formatDate(material.createdAt)}
                      </span>
                      <div className="student-course-material-actions">
                        <span className="student-course-feed-meta">{material.sizeBytes} bytes</span>
                        <a
                          className="student-course-inline-link"
                          href={buildFileDownloadUrl(material.downloadUrl)}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Download
                        </a>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </article>

            <article className="card student-course-feed-card">
              <div className="section-heading">
                <div>
                  <h3>Final Grade</h3>
                  <p className="muted">Published final standing for the course.</p>
                </div>
              </div>
              {detail.finalGrade ? (
                <div className="student-course-feed-item">
                  <strong>
                    {detail.finalGrade.letterValue || "-"} | {detail.finalGrade.numericValue.toFixed(2)}
                  </strong>
                  <span className="student-course-feed-meta">
                    Status: {humanizeToken(detail.finalGrade.status)} | Points: {detail.finalGrade.points.toFixed(2)}
                  </span>
                  <span className="student-course-feed-meta">
                    Published: {formatDateTime(detail.finalGrade.publishedAt)}
                  </span>
                </div>
              ) : (
                <p className="muted">Final grade has not been published yet.</p>
              )}

              <div className="student-course-side-card student-course-side-card-compact">
                <h4 className="teacher-section-side-title">Quick actions</h4>
                <div className="student-course-feed-list">
                  <Link className="student-course-inline-link" to="/app/student/attendance">
                    Open attendance center
                  </Link>
                  <Link className="student-course-inline-link" to="/app/student/exams">
                    Open exam schedule
                  </Link>
                  <Link className="student-course-inline-link" to="/app/student/registration">
                    Back to registration center
                  </Link>
                </div>
              </div>
            </article>
          </section>
        </>
      ) : null}
    </div>
  );
}
