import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  clearStudentNextSemesterSection,
  fetchStudentNextSemesterOverview,
  removeStudentNextSemesterSubject,
  saveStudentNextSemesterSection,
  saveStudentNextSemesterSubject
} from "../../lib/api";
import type {
  StudentNextSemesterOverview,
  StudentNextSemesterSectionOption,
  StudentNextSemesterSubjectOption,
  StudentRegistrationMeetingSlot
} from "../../types/student";

const DAYS = [
  { key: "MONDAY", label: "Mon" },
  { key: "TUESDAY", label: "Tue" },
  { key: "WEDNESDAY", label: "Wed" },
  { key: "THURSDAY", label: "Thu" },
  { key: "FRIDAY", label: "Fri" },
  { key: "SATURDAY", label: "Sat" }
] as const;

type SubjectScheduleEntry = {
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  sectionId: number;
  teacherName: string | null;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string | null;
  lessonType: string | null;
  selected: boolean;
};

function formatLessonType(value: string | null): string {
  if (!value) return "";
  return value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function formatMeetingSlot(slot: StudentRegistrationMeetingSlot): string {
  const day = slot.dayOfWeek.slice(0, 3);
  const lessonType = formatLessonType(slot.lessonType);
  return `${day} ${slot.startTime.slice(0, 5)}-${slot.endTime.slice(0, 5)}${slot.room ? ` | ${slot.room}` : ""}${lessonType ? ` | ${lessonType}` : ""}`;
}

function parseHour(value: string | null): number | null {
  if (!value) return null;
  const numeric = Number.parseInt(value.slice(0, 2), 10);
  return Number.isNaN(numeric) ? null : numeric;
}

function formatHour(hour: number): string {
  return `${String(hour).padStart(2, "0")}:00`;
}

function buildVisibleHours(entries: SubjectScheduleEntry[]): number[] {
  const parsedStarts = entries.map((entry) => parseHour(entry.startTime)).filter((value): value is number => value !== null);
  const parsedEnds = entries.map((entry) => parseHour(entry.endTime)).filter((value): value is number => value !== null);

  const minHour = parsedStarts.length ? Math.max(8, Math.min(...parsedStarts)) : 8;
  const maxHour = parsedEnds.length ? Math.min(22, Math.max(...parsedEnds)) : 18;
  const safeEnd = Math.max(minHour + 1, maxHour);

  return Array.from({ length: safeEnd - minHour }, (_, index) => minHour + index);
}

function joinReasons(reasons: string[]): string {
  return reasons.join(" | ");
}

function compareSectionOptions(left: StudentNextSemesterSectionOption, right: StudentNextSemesterSectionOption): number {
  if (left.selected !== right.selected) {
    return left.selected ? -1 : 1;
  }
  return (left.teacherName || "").localeCompare(right.teacherName || "");
}

function compareMeetingSlots(left: StudentRegistrationMeetingSlot, right: StudentRegistrationMeetingSlot): number {
  const leftStart = `${left.dayOfWeek}-${left.startTime}`;
  const rightStart = `${right.dayOfWeek}-${right.startTime}`;
  return leftStart.localeCompare(rightStart);
}

function buildSearchTarget(subject: StudentNextSemesterSubjectOption): string {
  return [
    subject.subjectCode,
    subject.subjectName,
    ...subject.sections.map((section) => section.teacherName || ""),
    ...subject.sections.flatMap((section) => section.meetingTimes.map((slot) => slot.room || ""))
  ]
    .join(" ")
    .toLowerCase();
}

function buildScheduleEntries(subject: StudentNextSemesterSubjectOption | null): SubjectScheduleEntry[] {
  if (!subject) {
    return [];
  }

  return subject.sections.flatMap((section) =>
    [...section.meetingTimes]
      .sort(compareMeetingSlots)
      .map((slot) => ({
        subjectId: subject.subjectId,
        subjectCode: subject.subjectCode,
        subjectName: subject.subjectName,
        sectionId: section.sectionId,
        teacherName: section.teacherName,
        dayOfWeek: slot.dayOfWeek,
        startTime: slot.startTime,
        endTime: slot.endTime,
        room: slot.room,
        lessonType: slot.lessonType,
        selected: section.selected
      }))
  );
}

function buildGridMap(entries: SubjectScheduleEntry[]): Map<string, SubjectScheduleEntry[]> {
  const grid = new Map<string, SubjectScheduleEntry[]>();

  entries.forEach((entry) => {
    const startHour = parseHour(entry.startTime);
    if (startHour === null) return;

    const key = `${entry.dayOfWeek}-${startHour}`;
    const bucket = grid.get(key) || [];
    bucket.push(entry);
    bucket.sort((left, right) => {
      if (left.startTime !== right.startTime) {
        return left.startTime.localeCompare(right.startTime);
      }
      return left.sectionId - right.sectionId;
    });
    grid.set(key, bucket);
  });

  return grid;
}

export default function StudentSubjectsPage() {
  const [overview, setOverview] = useState<StudentNextSemesterOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(null);
  const [busySubjectId, setBusySubjectId] = useState<number | null>(null);
  const [busySectionId, setBusySectionId] = useState<number | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const payload = await fetchStudentNextSemesterOverview();
      setOverview(payload);
      setSelectedSubjectId((current) => {
        if (current !== null && payload.subjects.some((subject) => subject.subjectId === current)) {
          return current;
        }
        return payload.subjects[0]?.subjectId ?? null;
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load subject explorer");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  const normalizedQuery = searchQuery.trim().toLowerCase();

  const filteredSubjects = useMemo(() => {
    if (!overview) {
      return [];
    }
    if (!normalizedQuery) {
      return overview.subjects;
    }
    return overview.subjects.filter((subject) => buildSearchTarget(subject).includes(normalizedQuery));
  }, [overview, normalizedQuery]);

  useEffect(() => {
    if (filteredSubjects.length === 0) {
      setSelectedSubjectId(null);
      return;
    }

    if (!filteredSubjects.some((subject) => subject.subjectId === selectedSubjectId)) {
      setSelectedSubjectId(filteredSubjects[0].subjectId);
    }
  }, [filteredSubjects, selectedSubjectId]);

  const selectedSubject = useMemo(
    () => filteredSubjects.find((subject) => subject.subjectId === selectedSubjectId) || filteredSubjects[0] || null,
    [filteredSubjects, selectedSubjectId]
  );

  const scheduleEntries = useMemo(() => buildScheduleEntries(selectedSubject), [selectedSubject]);
  const scheduleGrid = useMemo(() => buildGridMap(scheduleEntries), [scheduleEntries]);
  const visibleHours = useMemo(() => buildVisibleHours(scheduleEntries), [scheduleEntries]);

  const publishedSectionsCount = useMemo(
    () => overview?.subjects.reduce((total, subject) => total + subject.sections.length, 0) || 0,
    [overview]
  );

  const selectedTimeCount = useMemo(
    () => overview?.savedSubjects.filter((subject) => subject.sectionSelected).length || 0,
    [overview]
  );

  async function handleSubjectAction(subjectId: number, action: "save" | "remove") {
    setBusySubjectId(subjectId);
    setError(null);
    setSuccess(null);
    try {
      const result =
        action === "save"
          ? await saveStudentNextSemesterSubject(subjectId)
          : await removeStudentNextSemesterSubject(subjectId);
      setSuccess(result.message);
      await loadData();
      setSelectedSubjectId(subjectId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Subject action failed");
    } finally {
      setBusySubjectId(null);
    }
  }

  async function handleSectionAction(subjectId: number, sectionId: number, action: "select" | "clear") {
    setBusySectionId(sectionId);
    setError(null);
    setSuccess(null);
    try {
      const result =
        action === "select"
          ? await saveStudentNextSemesterSection(subjectId, sectionId)
          : await clearStudentNextSemesterSection(subjectId);
      setSuccess(result.message);
      await loadData();
      setSelectedSubjectId(subjectId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Section action failed");
    } finally {
      setBusySectionId(null);
    }
  }

  return (
    <div className="screen app-screen subject-browser-page">
      <header className="topbar">
        <div>
          <h2>Subject Explorer</h2>
          <p className="muted">
            Search all published next-semester subjects, click one, and preview every section time in a weekly timetable.
          </p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/student/registration">
            Registration
          </Link>
          <Link className="link-btn" to="/app/student/schedule">
            Current schedule
          </Link>
        </div>
      </header>

      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      {success ? (
        <section className="card">
          <p className="success">{success}</p>
        </section>
      ) : null}

      {loading ? (
        <section className="card">
          <p>Loading subject explorer...</p>
        </section>
      ) : null}

      {!loading && overview ? (
        <>
          <section className="card subject-browser-hero-card">
            <div className="subject-browser-hero">
              <div className="subject-browser-hero-copy">
                <span className="assistant-eyebrow">Next Semester</span>
                <h3>{overview.semesterName || "Published subjects"}</h3>
                <p className="muted">
                  This page is for comparison only until you press an action button. Clicking a subject just opens its full timetable preview.
                </p>
                <div className="subject-browser-hero-note">
                  <strong>{overview.message}</strong>
                  <span className="muted compact-text">
                    Save the subject first, then choose a preferred section only if its time works for you.
                  </span>
                </div>
              </div>

              <div className="subject-browser-stat-grid">
                <div className="registration-mini-stat">
                  <strong>{overview.subjects.length}</strong>
                  <span>Subjects</span>
                </div>
                <div className="registration-mini-stat">
                  <strong>{publishedSectionsCount}</strong>
                  <span>Published sections</span>
                </div>
                <div className="registration-mini-stat">
                  <strong>{overview.selectedCount}</strong>
                  <span>Saved subjects</span>
                </div>
                <div className="registration-mini-stat">
                  <strong>{selectedTimeCount}</strong>
                  <span>Times chosen</span>
                </div>
              </div>
            </div>
          </section>

          <section className="card subject-browser-search-card">
            <div className="subject-browser-search-row">
              <label className="subject-browser-search-group">
                <span>Search subject</span>
                <input
                  type="search"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="Type code, name, teacher, or room"
                />
              </label>

              <div className="subject-browser-search-summary">
                <span className={`badge ${overview.selectionEnabled ? "" : "badge-neutral"}`}>
                  {overview.selectionEnabled ? `${overview.selectedCount}/${overview.maxSelections} saved` : "Selection closed"}
                </span>
                <span className="muted">
                  Showing {filteredSubjects.length} of {overview.subjects.length} subjects
                </span>
              </div>
            </div>
          </section>

          <section className="subject-browser-layout">
            <aside className="card subject-browser-sidebar">
              <div className="section-heading">
                <div>
                  <h3>Subjects</h3>
                  <p className="muted">Click any subject to see every available time.</p>
                </div>
              </div>

              {filteredSubjects.length === 0 ? (
                <p className="muted">No subjects matched this search.</p>
              ) : (
                <div className="subject-browser-subject-list">
                  {filteredSubjects.map((subject) => (
                    <button
                      key={subject.subjectId}
                      type="button"
                      className={`subject-browser-list-item${selectedSubject?.subjectId === subject.subjectId ? " active" : ""}`}
                      onClick={() => setSelectedSubjectId(subject.subjectId)}
                    >
                      <div className="subject-browser-list-head">
                        <strong>{subject.subjectCode}</strong>
                        <div className="subject-browser-badge-row">
                          {subject.saved ? <span className="badge badge-warning">Saved</span> : null}
                          {subject.selectedSectionId ? <span className="badge">Time chosen</span> : null}
                        </div>
                      </div>
                      <div className="subject-browser-list-name">{subject.subjectName}</div>
                      <div className="muted">
                        {subject.credits} credits | {subject.sections.length} section{subject.sections.length === 1 ? "" : "s"}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </aside>

            <div className="subject-browser-main">
              {selectedSubject ? (
                <>
                  <section className="card subject-browser-selected-card">
                    <div className="subject-browser-selected-head">
                      <div>
                        <span className="student-section-kicker">Selected Subject</span>
                        <h3>{selectedSubject.subjectCode} {selectedSubject.subjectName}</h3>
                        <p className="muted">
                          {selectedSubject.credits} credits | {selectedSubject.sections.length} published section{selectedSubject.sections.length === 1 ? "" : "s"}
                        </p>
                      </div>

                      <div className="actions">
                        {selectedSubject.saved ? (
                          <button
                            type="button"
                            className="ghost-danger-btn"
                            disabled={busySubjectId === selectedSubject.subjectId}
                            onClick={() => handleSubjectAction(selectedSubject.subjectId, "remove")}
                          >
                            {busySubjectId === selectedSubject.subjectId ? "Saving..." : "Remove subject"}
                          </button>
                        ) : (
                          <button
                            type="button"
                            disabled={busySubjectId === selectedSubject.subjectId || !overview.selectionEnabled}
                            onClick={() => handleSubjectAction(selectedSubject.subjectId, "save")}
                          >
                            {busySubjectId === selectedSubject.subjectId ? "Saving..." : "Save subject"}
                          </button>
                        )}
                      </div>
                    </div>

                    <div className="subject-browser-selected-summary">
                      <div className="profile-fact">
                        <span className="profile-fact-label">Saved in draft</span>
                        <span className="profile-fact-value">{selectedSubject.saved ? "Yes" : "No"}</span>
                      </div>
                      <div className="profile-fact">
                        <span className="profile-fact-label">Preferred time</span>
                        <span className="profile-fact-value">
                          {selectedSubject.selectedSectionId ? `Section #${selectedSubject.selectedSectionId}` : "Not selected"}
                        </span>
                      </div>
                      <div className="profile-fact">
                        <span className="profile-fact-label">Required</span>
                        <span className="profile-fact-value">{selectedSubject.required ? "Required" : "Elective"}</span>
                      </div>
                    </div>
                  </section>

                  <section className="card">
                    <div className="section-heading">
                      <div>
                        <h3>Weekly Timetable Preview</h3>
                        <p className="muted">
                          Every published section of {selectedSubject.subjectCode} is shown in the same weekly table.
                        </p>
                      </div>
                    </div>

                    {scheduleEntries.length === 0 ? (
                      <p className="muted">No meeting times published yet for this subject.</p>
                    ) : (
                      <div className="schedule-week-wrap">
                        <table className="schedule-week-table">
                          <thead>
                            <tr>
                              <th className="schedule-time-header">Time</th>
                              {DAYS.map((day) => (
                                <th key={day.key} className="schedule-day-header">
                                  {day.label}
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {visibleHours.map((hour) => (
                              <tr key={hour} className="schedule-hour-row">
                                <td className="schedule-time-cell">{formatHour(hour)}</td>
                                {DAYS.map((day) => {
                                  const key = `${day.key}-${hour}`;
                                  const entries = scheduleGrid.get(key) || [];

                                  return (
                                    <td key={key} className="subject-browser-schedule-cell">
                                      {entries.length === 0 ? (
                                        <div className="schedule-cell-empty" />
                                      ) : (
                                        <div className="subject-browser-schedule-stack">
                                          {entries.map((entry) => (
                                            <article
                                              key={`${entry.sectionId}-${entry.dayOfWeek}-${entry.startTime}`}
                                              className={`schedule-slot-card subject-browser-schedule-card${entry.selected ? " subject-browser-schedule-card-selected" : ""}`}
                                            >
                                              <div className="schedule-slot-top">
                                                <Link className="schedule-slot-link" to={`/app/student/sections/${entry.sectionId}`}>
                                                  <strong className="schedule-slot-course">Section #{entry.sectionId}</strong>
                                                </Link>
                                                {entry.lessonType ? (
                                                  <span className="schedule-slot-type">{formatLessonType(entry.lessonType)}</span>
                                                ) : null}
                                              </div>
                                              <div className="schedule-slot-name">{entry.teacherName || "TBA"}</div>
                                              <div className="schedule-slot-meta">
                                                {entry.startTime.slice(0, 5)} - {entry.endTime.slice(0, 5)}
                                              </div>
                                              <div className="schedule-slot-meta">{entry.room || "Room TBA"}</div>
                                              {entry.selected ? (
                                                <div className="schedule-slot-meta">Currently preferred</div>
                                              ) : null}
                                            </article>
                                          ))}
                                        </div>
                                      )}
                                    </td>
                                  );
                                })}
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </section>

                  <section className="card subject-browser-sections-card">
                    <div className="section-heading">
                      <div>
                        <h3>All Sections</h3>
                        <p className="muted">
                          Full section list for {selectedSubject.subjectCode} with actions and conflict messages.
                        </p>
                      </div>
                    </div>

                    <div className="subject-browser-section-grid">
                      {[...selectedSubject.sections].sort(compareSectionOptions).map((section) => (
                        <article
                          key={section.sectionId}
                          className={`subject-browser-section-card${section.selected ? " subject-browser-section-card-selected" : ""}`}
                        >
                          <div className="subject-browser-section-head">
                            <div>
                              <strong>{section.teacherName || "TBA"}</strong>
                              <div className="muted">
                                Section #{section.sectionId} | {section.occupiedSeats}/{section.capacity}
                              </div>
                            </div>
                            <div className="subject-browser-badge-row">
                              {section.selected ? (
                                <span className="badge">Preferred</span>
                              ) : selectedSubject.saved && section.canSelect ? (
                                <span className="badge badge-warning">Ready</span>
                              ) : (
                                <span className="badge badge-neutral">Preview</span>
                              )}
                            </div>
                          </div>

                          <div className="schedule-chip-list">
                            {[...section.meetingTimes].sort(compareMeetingSlots).map((slot, index) => (
                              <span key={`${section.sectionId}-${index}`} className="schedule-chip">
                                {formatMeetingSlot(slot)}
                              </span>
                            ))}
                          </div>

                          <div className="subject-browser-section-status">
                            {section.selected ? (
                              <p className="muted compact-text">
                                This is your current preferred section for {selectedSubject.subjectCode}.
                              </p>
                            ) : !selectedSubject.saved ? (
                              <p className="muted compact-text">
                                Save the subject first if you want to choose one of these times.
                              </p>
                            ) : section.blockedReasons.length > 0 ? (
                              <p className="muted compact-text">{joinReasons(section.blockedReasons)}</p>
                            ) : (
                              <p className="muted compact-text">This section currently fits your draft plan.</p>
                            )}
                          </div>

                          <div className="actions">
                            <Link className="student-course-inline-link" to={`/app/student/sections/${section.sectionId}`}>
                              Details
                            </Link>

                            {section.selected ? (
                              <button
                                type="button"
                                disabled={busySectionId === section.sectionId}
                                onClick={() => handleSectionAction(selectedSubject.subjectId, section.sectionId, "clear")}
                              >
                                {busySectionId === section.sectionId ? "Saving..." : "Clear time"}
                              </button>
                            ) : (
                              <button
                                type="button"
                                disabled={
                                  busySectionId === section.sectionId ||
                                  !selectedSubject.saved ||
                                  !section.canSelect ||
                                  !overview.selectionEnabled
                                }
                                onClick={() => handleSectionAction(selectedSubject.subjectId, section.sectionId, "select")}
                              >
                                {busySectionId === section.sectionId
                                  ? "Saving..."
                                  : selectedSubject.saved
                                    ? "Choose this time"
                                    : "Save subject first"}
                              </button>
                            )}
                          </div>
                        </article>
                      ))}
                    </div>
                  </section>
                </>
              ) : (
                <section className="card">
                  <p className="muted">Choose a subject from the left to see its full timetable.</p>
                </section>
              )}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
