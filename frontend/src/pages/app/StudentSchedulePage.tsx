import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchStudentSchedule, fetchStudentScheduleOptions } from "../../lib/api";
import type {
  StudentScheduleItem,
  StudentScheduleOptions,
  StudentScheduleSemesterOption
} from "../../types/student";

const DAYS = [
  { key: "MONDAY", label: "Mon" },
  { key: "TUESDAY", label: "Tue" },
  { key: "WEDNESDAY", label: "Wed" },
  { key: "THURSDAY", label: "Thu" },
  { key: "FRIDAY", label: "Fri" },
  { key: "SATURDAY", label: "Sat" }
] as const;

const SEASON_ORDER: Record<string, number> = {
  Fall: 1,
  Spring: 2
};

function parseHour(value: string | null): number | null {
  if (!value) return null;
  const numeric = Number.parseInt(value.slice(0, 2), 10);
  return Number.isNaN(numeric) ? null : numeric;
}

function formatHour(hour: number): string {
  return `${String(hour).padStart(2, "0")}:00`;
}

function buildVisibleHours(items: StudentScheduleItem[]): number[] {
  const parsedStarts = items.map((item) => parseHour(item.startTime)).filter((value): value is number => value !== null);
  const parsedEnds = items.map((item) => parseHour(item.endTime)).filter((value): value is number => value !== null);

  const minHour = parsedStarts.length ? Math.max(8, Math.min(...parsedStarts)) : 8;
  const maxHour = parsedEnds.length ? Math.min(22, Math.max(...parsedEnds)) : 18;
  const safeEnd = Math.max(minHour + 1, maxHour);

  return Array.from({ length: safeEnd - minHour }, (_, index) => minHour + index);
}

function formatTimeRange(item: StudentScheduleItem): string {
  const start = item.startTime ? item.startTime.slice(0, 5) : "--:--";
  const end = item.endTime ? item.endTime.slice(0, 5) : "--:--";
  return `${start} - ${end}`;
}

function formatLessonType(value: string | null): string {
  if (!value) return "";
  return value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function buildGridMap(items: StudentScheduleItem[]): {
  starts: Map<string, { item: StudentScheduleItem; span: number }>;
  occupied: Set<string>;
} {
  const starts = new Map<string, { item: StudentScheduleItem; span: number }>();
  const occupied = new Set<string>();

  items.forEach((item) => {
    if (!item.dayOfWeek) return;

    const startHour = parseHour(item.startTime);
    const endHour = parseHour(item.endTime);
    if (startHour === null || endHour === null) return;

    const span = Math.max(1, endHour - startHour);
    const startKey = `${item.dayOfWeek}-${startHour}`;
    starts.set(startKey, { item, span });

    for (let hour = startHour + 1; hour < startHour + span; hour += 1) {
      occupied.add(`${item.dayOfWeek}-${hour}`);
    }
  });

  return { starts, occupied };
}

function compareSemesterOption(left: StudentScheduleSemesterOption, right: StudentScheduleSemesterOption): number {
  if (left.academicYear !== right.academicYear) {
    return right.academicYear.localeCompare(left.academicYear);
  }
  return (SEASON_ORDER[left.season] || 99) - (SEASON_ORDER[right.season] || 99);
}

export default function StudentSchedulePage() {
  const [items, setItems] = useState<StudentScheduleItem[]>([]);
  const [options, setOptions] = useState<StudentScheduleOptions | null>(null);
  const [selectedYear, setSelectedYear] = useState("");
  const [selectedSeason, setSelectedSeason] = useState("");
  const [loadingFilters, setLoadingFilters] = useState(true);
  const [loadingSchedule, setLoadingSchedule] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadOptions() {
      setLoadingFilters(true);
      setError(null);
      try {
        const payload = await fetchStudentScheduleOptions();
        if (cancelled) return;

        const semesters = [...payload.semesters].sort(compareSemesterOption);
        const normalized = { ...payload, semesters };
        setOptions(normalized);

        const currentSemester =
          semesters.find((semester) => semester.id === payload.currentSemesterId) || semesters[0] || null;

        if (currentSemester) {
          setSelectedYear(currentSemester.academicYear);
          setSelectedSeason(currentSemester.season);
        } else {
          setSelectedYear("");
          setSelectedSeason("");
          setItems([]);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load schedule filters");
        }
      } finally {
        if (!cancelled) {
          setLoadingFilters(false);
        }
      }
    }

    void loadOptions();
    return () => {
      cancelled = true;
    };
  }, []);

  const availableYears = useMemo(() => {
    const values = options?.semesters.map((semester) => semester.academicYear).filter(Boolean) || [];
    return [...new Set(values)].sort((left, right) => right.localeCompare(left));
  }, [options]);

  const availableSemesters = useMemo(() => {
    if (!options || !selectedYear) return [];
    return options.semesters
      .filter((semester) => semester.academicYear === selectedYear)
      .sort(compareSemesterOption);
  }, [options, selectedYear]);

  const activeSemester = useMemo(() => {
    if (availableSemesters.length === 0) return null;
    return availableSemesters.find((semester) => semester.season === selectedSeason) || availableSemesters[0];
  }, [availableSemesters, selectedSeason]);

  useEffect(() => {
    if (!availableSemesters.length) return;
    const seasonExists = availableSemesters.some((semester) => semester.season === selectedSeason);
    if (!seasonExists) {
      setSelectedSeason(availableSemesters[0].season);
    }
  }, [availableSemesters, selectedSeason]);

  useEffect(() => {
    let cancelled = false;

    async function loadSchedule() {
      if (!activeSemester) {
        setItems([]);
        return;
      }

      setLoadingSchedule(true);
      setError(null);
      try {
        const payload = await fetchStudentSchedule(activeSemester.id);
        if (!cancelled) {
          setItems(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load schedule");
        }
      } finally {
        if (!cancelled) {
          setLoadingSchedule(false);
        }
      }
    }

    void loadSchedule();
    return () => {
      cancelled = true;
    };
  }, [activeSemester]);

  const grid = useMemo(() => buildGridMap(items), [items]);
  const visibleHours = useMemo(() => buildVisibleHours(items), [items]);

  return (
    <div className="screen app-screen schedule-page">
      <header className="topbar">
        <div>
          <h2>Student Schedule</h2>
          <p className="muted">Choose an academic year and semester to see the weekly timetable.</p>
        </div>
      </header>

      <section className="card schedule-filter-card">
        <div className="schedule-filter-bar">
          <label className="schedule-filter-group">
            <span>Academic Year</span>
            <select
              className="schedule-filter-select"
              value={selectedYear}
              onChange={(event) => setSelectedYear(event.target.value)}
              disabled={loadingFilters || availableYears.length === 0}
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
              disabled={loadingFilters || availableSemesters.length === 0}
            >
              {availableSemesters.length === 0 ? <option value="">No semesters</option> : null}
              {availableSemesters.map((semester) => (
                <option key={semester.id} value={semester.season}>
                  {semester.season}
                </option>
              ))}
            </select>
          </label>

          <div className="schedule-filter-summary">
            <span className="schedule-filter-summary-label">Showing</span>
            <strong>{activeSemester?.name || "No semester selected"}</strong>
          </div>
        </div>
      </section>

      <section className="card">
        {loadingFilters || loadingSchedule ? <p>Loading schedule...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loadingFilters && !loadingSchedule && !error && !activeSemester ? (
          <p className="muted">No semesters found for this student.</p>
        ) : null}

        {!loadingFilters && !loadingSchedule && !error && activeSemester ? (
          <>
            {items.length === 0 ? (
              <p className="muted">No schedule items for {activeSemester.name}.</p>
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
                          if (grid.occupied.has(key)) {
                            return null;
                          }

                          const entry = grid.starts.get(key);
                          if (!entry) {
                            return (
                              <td key={key} className="schedule-slot-cell">
                                <div className="schedule-cell-empty" />
                              </td>
                            );
                          }

                          const { item, span } = entry;
                          return (
                            <td key={key} className="schedule-slot-cell" rowSpan={span}>
                              <article className="schedule-slot-card">
                                <div className="schedule-slot-top">
                                  <Link className="schedule-slot-link" to={`/app/student/sections/${item.sectionId}`}>
                                    <strong className="schedule-slot-course">{item.courseCode}</strong>
                                  </Link>
                                  {item.lessonType ? (
                                    <span className="schedule-slot-type">{formatLessonType(item.lessonType)}</span>
                                  ) : null}
                                </div>
                                <Link className="schedule-slot-name-link" to={`/app/student/sections/${item.sectionId}`}>
                                  <div className="schedule-slot-name">{item.courseName}</div>
                                </Link>
                                <div className="schedule-slot-meta">{formatTimeRange(item)}</div>
                                <div className="schedule-slot-meta">{item.room || "Room TBA"}</div>
                                {item.teacherName ? (
                                  <div className="schedule-slot-meta">{item.teacherName}</div>
                                ) : null}
                              </article>
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        ) : null}
      </section>
    </div>
  );
}
