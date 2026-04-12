import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchStudentJournal, fetchStudentJournalOptions } from "../../lib/api";
import type {
  StudentJournalItem,
  StudentJournalOptions,
  StudentScheduleSemesterOption
} from "../../types/student";

const SEASON_ORDER: Record<string, number> = {
  Fall: 1,
  Spring: 2
};

function compareSemesterOption(left: StudentScheduleSemesterOption, right: StudentScheduleSemesterOption): number {
  if (left.academicYear !== right.academicYear) {
    return right.academicYear.localeCompare(left.academicYear);
  }
  return (SEASON_ORDER[left.season] || 99) - (SEASON_ORDER[right.season] || 99);
}

function formatScore(value: number | null, max: number | null): string {
  if (value === null || value === undefined) return "-";
  if (max === null || max === undefined) return value.toFixed(2);
  return `${value.toFixed(2)} / ${max.toFixed(0)}`;
}

export default function StudentJournalPage() {
  const [items, setItems] = useState<StudentJournalItem[]>([]);
  const [options, setOptions] = useState<StudentJournalOptions | null>(null);
  const [selectedYear, setSelectedYear] = useState("");
  const [selectedSeason, setSelectedSeason] = useState("");
  const [loadingFilters, setLoadingFilters] = useState(true);
  const [loadingJournal, setLoadingJournal] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadOptions() {
      setLoadingFilters(true);
      setError(null);
      try {
        const payload = await fetchStudentJournalOptions();
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
          setError(err instanceof ApiError ? err.message : "Failed to load journal filters");
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

    async function loadJournal() {
      if (!activeSemester) {
        setItems([]);
        return;
      }

      setLoadingJournal(true);
      setError(null);
      try {
        const payload = await fetchStudentJournal(activeSemester.id);
        if (!cancelled) {
          setItems(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load journal");
        }
      } finally {
        if (!cancelled) {
          setLoadingJournal(false);
        }
      }
    }

    void loadJournal();
    return () => {
      cancelled = true;
    };
  }, [activeSemester]);

  return (
    <div className="screen app-screen journal-page">
      <header className="topbar">
        <div>
          <h2>Student Journal</h2>
          <p className="muted">Review attestation and final points for each semester.</p>
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
        {loadingFilters || loadingJournal ? <p>Loading journal...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loadingFilters && !loadingJournal && !error && !activeSemester ? (
          <p className="muted">No journal semesters found for this student.</p>
        ) : null}

        {!loadingFilters && !loadingJournal && !error && activeSemester ? (
          <>
            {items.length === 0 ? (
              <p className="muted">No published journal entries for {activeSemester.name}.</p>
            ) : (
              <div className="table-wrap journal-table-wrap">
                <table className="table journal-table">
                  <thead>
                    <tr>
                      <th>Course</th>
                      <th>Name</th>
                      <th>Attestation 1</th>
                      <th>Attestation 2</th>
                      <th>Final</th>
                      <th>Total</th>
                      <th>Letter</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr key={`${item.sectionId}-${item.courseCode}`}>
                        <td>
                          <Link className="student-course-table-link" to={`/app/student/sections/${item.sectionId}`}>
                            {item.courseCode}
                          </Link>
                        </td>
                        <td>
                          <Link className="student-course-table-link student-course-table-link-name" to={`/app/student/sections/${item.sectionId}`}>
                            {item.courseName}
                          </Link>
                        </td>
                        <td>{formatScore(item.attestation1, item.attestation1Max)}</td>
                        <td>{formatScore(item.attestation2, item.attestation2Max)}</td>
                        <td>{formatScore(item.finalExam, item.finalExamMax)}</td>
                        <td>{item.totalScore !== null && item.totalScore !== undefined ? item.totalScore.toFixed(2) : "-"}</td>
                        <td>{item.letterValue || "-"}</td>
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
