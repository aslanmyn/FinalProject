import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchTeacherSections } from "../../lib/api";
import type { TeacherSectionItem, TeacherSectionMeetingTimeItem } from "../../types/teacher";

const SEASON_ORDER: Record<string, number> = {
  Fall: 1,
  Spring: 2
};

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

function compareSections(left: TeacherSectionItem, right: TeacherSectionItem): number {
  if (left.semesterName !== right.semesterName) {
    return right.semesterName.localeCompare(left.semesterName);
  }
  if (left.subjectCode !== right.subjectCode) {
    return left.subjectCode.localeCompare(right.subjectCode);
  }
  return left.id - right.id;
}

function compareMeetingTimes(left: TeacherSectionMeetingTimeItem, right: TeacherSectionMeetingTimeItem): number {
  if (left.dayOfWeek !== right.dayOfWeek) {
    return (DAY_ORDER[left.dayOfWeek] || 99) - (DAY_ORDER[right.dayOfWeek] || 99);
  }
  return left.startTime.localeCompare(right.startTime);
}

function formatLessonType(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

function extractAcademicYear(semesterName: string): string {
  const [year] = semesterName.split(" ");
  return year || semesterName;
}

function extractSeason(semesterName: string): string {
  const parts = semesterName.split(" ");
  return parts[1] || semesterName;
}

function formatDay(dayOfWeek: string): string {
  return DAY_LABELS[dayOfWeek] || dayOfWeek;
}

function formatTime(value: string): string {
  return value.length >= 5 ? value.slice(0, 5) : value;
}

function formatMeeting(meetingTime: TeacherSectionMeetingTimeItem): string {
  const room = meetingTime.room ? ` • ${meetingTime.room}` : "";
  return `${formatDay(meetingTime.dayOfWeek)} ${formatTime(meetingTime.startTime)}-${formatTime(meetingTime.endTime)}${room}`;
}

export default function TeacherSectionsPage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedYear, setSelectedYear] = useState("");
  const [selectedSeason, setSelectedSeason] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchTeacherSections();
        if (cancelled) return;

        const sorted = [...payload].sort(compareSections);
        setSections(sorted);

        const first = sorted[0];
        if (first) {
          setSelectedYear(extractAcademicYear(first.semesterName));
          setSelectedSeason(extractSeason(first.semesterName));
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

  const availableYears = useMemo(
    () =>
      [...new Set(sections.map((section) => extractAcademicYear(section.semesterName)))].sort((left, right) =>
        right.localeCompare(left)
      ),
    [sections]
  );

  const availableSemesters = useMemo(() => {
    if (!selectedYear) return [];
    return [
      ...new Set(
        sections
          .filter((section) => extractAcademicYear(section.semesterName) === selectedYear)
          .map((section) => extractSeason(section.semesterName))
      )
    ].sort((left, right) => (SEASON_ORDER[left] || 99) - (SEASON_ORDER[right] || 99));
  }, [sections, selectedYear]);

  useEffect(() => {
    if (!availableSemesters.length) return;
    if (!availableSemesters.includes(selectedSeason)) {
      setSelectedSeason(availableSemesters[0]);
    }
  }, [availableSemesters, selectedSeason]);

  const filteredSections = useMemo(
    () =>
      sections.filter((section) => {
        const year = extractAcademicYear(section.semesterName);
        const season = extractSeason(section.semesterName);
        return (!selectedYear || year === selectedYear) && (!selectedSeason || season === selectedSeason);
      }),
    [sections, selectedSeason, selectedYear]
  );

  const showingLabel = useMemo(() => {
    if (selectedYear && selectedSeason) return `${selectedYear} ${selectedSeason}`;
    if (selectedYear) return selectedYear;
    return "All sections";
  }, [selectedSeason, selectedYear]);

  const summary = useMemo(() => {
    const totalCapacity = filteredSections.reduce((sum, section) => sum + section.capacity, 0);
    const totalEnrolled = filteredSections.reduce((sum, section) => sum + section.enrolledCount, 0);
    const totalMeetings = filteredSections.reduce((sum, section) => sum + section.meetingTimes.length, 0);
    return { totalCapacity, totalEnrolled, totalMeetings };
  }, [filteredSections]);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Teacher Sections</h2>
          <p className="muted">A complete teaching register with schedule, capacity, credits and semester history.</p>
        </div>
      </header>

      {loading ? <p>Loading sections...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error ? (
        <>
          <section className="card schedule-filter-card">
            <div className="schedule-filter-bar">
              <label className="schedule-filter-group">
                <span>Academic Year</span>
                <select
                  className="schedule-filter-select"
                  value={selectedYear}
                  onChange={(event) => setSelectedYear(event.target.value)}
                  disabled={availableYears.length === 0}
                >
                  {availableYears.length === 0 ? <option value="">No years</option> : null}
                  {availableYears.map((year) => (
                    <option key={year} value={year}>
                      {year}
                    </option>
                  ))}
                </select>
              </label>

              <label className="schedule-filter-group">
                <span>Semester</span>
                <select
                  className="schedule-filter-select"
                  value={selectedSeason}
                  onChange={(event) => setSelectedSeason(event.target.value)}
                  disabled={availableSemesters.length === 0}
                >
                  {availableSemesters.length === 0 ? <option value="">No semesters</option> : null}
                  {availableSemesters.map((semester) => (
                    <option key={semester} value={semester}>
                      {semester}
                    </option>
                  ))}
                </select>
              </label>

              <div className="schedule-filter-summary">
                <span className="schedule-filter-summary-label">Showing</span>
                <strong>{showingLabel}</strong>
              </div>
            </div>
          </section>

          <section className="card teacher-sections-card">
            <div className="teacher-sections-stats">
              <div className="teacher-sections-stat">
                <span>Sections</span>
                <strong>{filteredSections.length}</strong>
              </div>
              <div className="teacher-sections-stat">
                <span>Meetings</span>
                <strong>{summary.totalMeetings}</strong>
              </div>
              <div className="teacher-sections-stat">
                <span>Enrolled</span>
                <strong>{summary.totalEnrolled}</strong>
              </div>
              <div className="teacher-sections-stat">
                <span>Total Capacity</span>
                <strong>{summary.totalCapacity}</strong>
              </div>
            </div>

            {filteredSections.length === 0 ? (
              <p className="muted">No sections found for the selected period.</p>
            ) : (
              <div className="table-wrap teacher-sections-table-wrap">
                <table className="table teacher-sections-table">
                  <thead>
                    <tr>
                      <th>Section</th>
                      <th>Course</th>
                      <th>Subject</th>
                      <th>Program</th>
                      <th>Semester</th>
                      <th>Schedule</th>
                      <th>Enrollment</th>
                      <th>Credits</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredSections.map((section) => {
                      const sortedMeetings = [...section.meetingTimes].sort(compareMeetingTimes);
                      return (
                        <tr key={section.id}>
                          <td>
                            <div className="teacher-section-id-cell">
                              <strong>#{section.id}</strong>
                              <span className="badge teacher-section-kind">{formatLessonType(section.lessonType)}</span>
                            </div>
                          </td>
                          <td>
                            <div className="teacher-section-course-cell">
                              <strong>{section.subjectCode}</strong>
                            </div>
                          </td>
                          <td>
                            <div className="teacher-section-subject-cell">
                              <strong>{section.subjectName}</strong>
                              {section.facultyName ? <span>{section.facultyName}</span> : null}
                            </div>
                          </td>
                          <td>
                            <div className="teacher-section-program-cell">
                              <strong>{section.programName || "General curriculum"}</strong>
                              <span>{section.meetingTimes.length} meetings</span>
                            </div>
                          </td>
                          <td>
                            <div className="teacher-section-semester-cell">
                              <strong>{section.semesterName}</strong>
                              <span>{extractAcademicYear(section.semesterName)}</span>
                            </div>
                          </td>
                          <td>
                            {sortedMeetings.length === 0 ? (
                              <span className="muted">Schedule not assigned</span>
                            ) : (
                              <div className="teacher-meeting-list">
                                {sortedMeetings.map((meetingTime) => (
                                  <div key={meetingTime.id} className="teacher-meeting-item">
                                    <div className="teacher-meeting-line">
                                      <strong>{formatMeeting(meetingTime)}</strong>
                                      <span className="badge teacher-meeting-badge">
                                        {formatLessonType(meetingTime.lessonType)}
                                      </span>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </td>
                          <td>
                            <div className="teacher-section-enrollment-cell">
                              <strong>
                                {section.enrolledCount} / {section.capacity}
                              </strong>
                              <span>
                                {section.capacity > 0
                                  ? `${Math.round((section.enrolledCount / section.capacity) * 100)}% filled`
                                  : "No capacity"}
                              </span>
                            </div>
                          </td>
                          <td>
                            <span className="teacher-section-credit">{section.credits}</span>
                          </td>
                          <td>
                            <span className={`badge teacher-section-status ${section.currentSemester ? "current" : "archive"}`}>
                              {section.currentSemester ? "Current" : "Archive"}
                            </span>
                          </td>
                          <td>
                            <Link className="link-btn" to={`/app/teacher/sections/${section.id}`}>
                              Open details
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
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
