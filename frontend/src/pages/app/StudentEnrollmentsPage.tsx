import { useEffect, useMemo, useState } from "react";
import { ApiError, fetchStudentEnrollmentOptions, fetchStudentEnrollments } from "../../lib/api";
import type {
  StudentEnrollmentItem,
  StudentEnrollmentOptions,
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

function formatCreatedAt(value: string): string {
  return value ? value.slice(0, 10) : "-";
}

export default function StudentEnrollmentsPage() {
  const [items, setItems] = useState<StudentEnrollmentItem[]>([]);
  const [options, setOptions] = useState<StudentEnrollmentOptions | null>(null);
  const [selectedYear, setSelectedYear] = useState("");
  const [selectedSeason, setSelectedSeason] = useState("");
  const [loadingFilters, setLoadingFilters] = useState(true);
  const [loadingEnrollments, setLoadingEnrollments] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadOptions() {
      setLoadingFilters(true);
      setError(null);
      try {
        const payload = await fetchStudentEnrollmentOptions();
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
          setError(err instanceof ApiError ? err.message : "Failed to load enrollment filters");
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

    async function loadEnrollments() {
      if (!activeSemester) {
        setItems([]);
        return;
      }

      setLoadingEnrollments(true);
      setError(null);
      try {
        const payload = await fetchStudentEnrollments(activeSemester.id);
        if (!cancelled) {
          setItems(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load enrollments");
        }
      } finally {
        if (!cancelled) {
          setLoadingEnrollments(false);
        }
      }
    }

    void loadEnrollments();
    return () => {
      cancelled = true;
    };
  }, [activeSemester]);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Student Enrollments</h2>
          <p className="muted">Track registered sections by academic year and semester.</p>
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
        {loadingFilters || loadingEnrollments ? <p>Loading enrollments...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loadingFilters && !loadingEnrollments && !error && !activeSemester ? (
          <p className="muted">No enrollment semesters found for this student.</p>
        ) : null}

        {!loadingFilters && !loadingEnrollments && !error && activeSemester ? (
          <>
            {items.length === 0 ? (
              <p className="muted">No enrollments found for {activeSemester.name}.</p>
            ) : (
              <div className="table-wrap journal-table-wrap">
                <table className="table enrollment-table">
                  <thead>
                    <tr>
                      <th>Section</th>
                      <th>Course</th>
                      <th>Name</th>
                      <th>Teacher</th>
                      <th>Credits</th>
                      <th>Semester</th>
                      <th>Status</th>
                      <th>Created At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr key={item.id}>
                        <td>#{item.sectionId}</td>
                        <td>{item.subjectCode}</td>
                        <td>{item.subjectName}</td>
                        <td>{item.teacherName || "TBA"}</td>
                        <td>{item.credits ?? "-"}</td>
                        <td>{item.semesterName || `${item.academicYear || ""} ${item.season || ""}`.trim() || "-"}</td>
                        <td>
                          <span className="badge">{item.status}</span>
                        </td>
                        <td>{formatCreatedAt(item.createdAt)}</td>
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
