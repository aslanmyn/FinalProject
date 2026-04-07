import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  addStudentCourse,
  ApiError,
  createStudentFxRegistration,
  dropStudentCourse,
  fetchStudentFxOverview,
  fetchStudentNextSemesterOverview,
  fetchStudentRegistrationCatalog,
  fetchStudentRegistrationOverview,
  removeStudentNextSemesterSection,
  saveStudentNextSemesterSection,
  submitStudentRegistration
} from "../../lib/api";
import type {
  StudentCourseCatalogItem,
  StudentFxOverview,
  StudentNextSemesterOverview,
  StudentRegistrationBoardItem,
  StudentRegistrationMeetingSlot,
  StudentRegistrationOverview,
  StudentWindowStatusItem
} from "../../types/student";

type RegistrationTab = "nextSemester" | "catalog" | "available" | "addDrop" | "fx";

const tabs: Array<{ id: RegistrationTab; label: string }> = [
  { id: "nextSemester", label: "Next Semester" },
  { id: "catalog", label: "Course Catalog" },
  { id: "available", label: "Available Sections" },
  { id: "addDrop", label: "Add / Drop" },
  { id: "fx", label: "FX Registration" }
];

function formatWindowType(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char: string) => char.toUpperCase());
}

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

function formatWindow(window: StudentWindowStatusItem): string {
  return `${window.startDate} -> ${window.endDate}`;
}

function joinReasons(reasons: string[]): string {
  return reasons.join(" | ");
}

export default function StudentRegistrationPage() {
  const [overview, setOverview] = useState<StudentRegistrationOverview | null>(null);
  const [catalog, setCatalog] = useState<StudentCourseCatalogItem[]>([]);
  const [fxOverview, setFxOverview] = useState<StudentFxOverview | null>(null);
  const [nextSemesterOverview, setNextSemesterOverview] = useState<StudentNextSemesterOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [tab, setTab] = useState<RegistrationTab>("nextSemester");
  const [busySectionId, setBusySectionId] = useState<number | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [overviewData, catalogData, fxData, nextSemesterData] = await Promise.all([
        fetchStudentRegistrationOverview(),
        fetchStudentRegistrationCatalog(),
        fetchStudentFxOverview(),
        fetchStudentNextSemesterOverview()
      ]);
      setOverview(overviewData);
      setCatalog(catalogData);
      setFxOverview(fxData);
      setNextSemesterOverview(nextSemesterData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load registration center");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  const availableRegistrationItems = useMemo(
    () => catalog.filter((item) => !item.registrationStatus && (item.canRegister || item.canAddDrop)),
    [catalog]
  );
  const addDropAdditions = useMemo(
    () => catalog.filter((item) => !item.registrationStatus && item.canAddDrop),
    [catalog]
  );

  async function handleAction(
    sectionId: number,
    action: "register" | "add" | "drop" | "fx"
  ) {
    setBusySectionId(sectionId);
    setError(null);
    setSuccess(null);
    try {
      if (action === "register") {
        const result = await submitStudentRegistration(sectionId);
        setSuccess(result.message);
      } else if (action === "add") {
        const result = await addStudentCourse(sectionId);
        setSuccess(result.message);
      } else if (action === "drop") {
        const result = await dropStudentCourse(sectionId);
        setSuccess(result.message);
      } else {
        await createStudentFxRegistration(sectionId);
        setSuccess("FX request submitted");
      }
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Action failed");
    } finally {
      setBusySectionId(null);
    }
  }

  async function handleNextSemesterAction(sectionId: number, action: "save" | "remove") {
    setBusySectionId(sectionId);
    setError(null);
    setSuccess(null);
    try {
      const result =
        action === "save"
          ? await saveStudentNextSemesterSection(sectionId)
          : await removeStudentNextSemesterSection(sectionId);
      setSuccess(result.message);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Next semester action failed");
    } finally {
      setBusySectionId(null);
    }
  }

  function renderCatalogRows(items: StudentCourseCatalogItem[]) {
    return (
      <div className="registration-table-wrap">
        <table className="table registration-table">
          <thead>
            <tr>
              <th>Course</th>
              <th>Teacher</th>
              <th>Schedule</th>
              <th>Seats</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.sectionId}>
                <td>
                  <strong>{item.subjectCode}</strong>
                  <div>{item.subjectName}</div>
                  <div className="muted">
                    {item.credits} credits | {item.semesterName}
                  </div>
                </td>
                <td>{item.teacherName || "TBA"}</td>
                <td>
                  <div className="schedule-chip-list">
                    {item.meetingTimes.map((slot, index) => (
                      <span key={`${item.sectionId}-${index}`} className="schedule-chip">
                        {formatMeetingSlot(slot)}
                      </span>
                    ))}
                  </div>
                </td>
                <td>
                  {item.occupiedSeats}/{item.capacity}
                </td>
                <td>
                  {item.registrationStatus ? (
                    <span className="badge badge-neutral">{item.registrationStatus}</span>
                  ) : item.canRegister ? (
                    <span className="badge">Ready for registration</span>
                  ) : item.canAddDrop ? (
                    <span className="badge badge-warning">Open in add/drop</span>
                  ) : (
                    <span className="badge badge-neutral">Blocked</span>
                  )}
                  {!item.canRegister && item.registrationBlockedReasons.length > 0 ? (
                    <p className="muted compact-text">{joinReasons(item.registrationBlockedReasons)}</p>
                  ) : null}
                </td>
                <td>
                  <div className="actions">
                    {!item.registrationStatus ? (
                      <button
                        type="button"
                        onClick={() => handleAction(item.sectionId, item.canRegister ? "register" : "add")}
                        disabled={busySectionId === item.sectionId || (!item.canRegister && !item.canAddDrop)}
                      >
                        {busySectionId === item.sectionId
                          ? "Saving..."
                          : item.canRegister
                            ? "Register"
                            : "Add"}
                      </button>
                    ) : null}
                    {item.canDrop ? (
                      <button
                        type="button"
                        className="ghost-danger-btn"
                        onClick={() => handleAction(item.sectionId, "drop")}
                        disabled={busySectionId === item.sectionId}
                      >
                        Drop
                      </button>
                    ) : null}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  function renderCurrentRegistrationRows(items: StudentRegistrationBoardItem[]) {
    return (
      <div className="registration-table-wrap">
        <table className="table registration-table">
          <thead>
            <tr>
              <th>Course</th>
              <th>Teacher</th>
              <th>Schedule</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.registrationId}>
                <td>
                  <strong>{item.subjectCode}</strong>
                  <div>{item.subjectName}</div>
                  <div className="muted">{item.credits} credits</div>
                </td>
                <td>{item.teacherName || "TBA"}</td>
                <td>
                  <div className="schedule-chip-list">
                    {item.meetingTimes.map((slot, index) => (
                      <span key={`${item.sectionId}-${index}`} className="schedule-chip">
                        {formatMeetingSlot(slot)}
                      </span>
                    ))}
                  </div>
                </td>
                <td>
                  <span className="badge badge-neutral">{item.status}</span>
                  {!item.canDrop && item.dropBlockedReasons.length > 0 ? (
                    <p className="muted compact-text">{joinReasons(item.dropBlockedReasons)}</p>
                  ) : null}
                </td>
                <td>
                  <button
                    type="button"
                    className="ghost-danger-btn"
                    onClick={() => handleAction(item.sectionId, "drop")}
                    disabled={!item.canDrop || busySectionId === item.sectionId}
                  >
                    {busySectionId === item.sectionId ? "Saving..." : "Drop course"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  function renderNextSemesterBoard() {
    if (!nextSemesterOverview) {
      return <p className="muted">Next semester planning is not available right now.</p>;
    }

    return (
      <section className="card stack">
        <div className="section-heading">
          <div>
            <h3>Next Semester Registration</h3>
            <p className="muted">{nextSemesterOverview.message}</p>
          </div>
          <span className={`badge ${nextSemesterOverview.selectionEnabled ? "" : "badge-neutral"}`}>
            {nextSemesterOverview.selectionEnabled
              ? `${nextSemesterOverview.selectedCount}/${nextSemesterOverview.maxSelections} saved`
              : "Unavailable"}
          </span>
        </div>

        {nextSemesterOverview.semesterName ? (
          <div className="registration-window-grid">
            <article className="window-card window-card-open">
              <div className="window-card-head">
                <h3>{nextSemesterOverview.semesterName}</h3>
                <span className="badge">Year {nextSemesterOverview.academicYear}, semester {nextSemesterOverview.semesterNumber}</span>
              </div>
              <p className="muted">Recommended curriculum subjects for your next registration cycle.</p>
            </article>
          </div>
        ) : null}

        <div>
          <h4>Saved choices</h4>
          {nextSemesterOverview.savedSelections.length === 0 ? (
            <p className="muted">No next-semester sections saved yet.</p>
          ) : (
            <div className="registration-table-wrap">
              <table className="table registration-table">
                <thead>
                  <tr>
                    <th>Course</th>
                    <th>Teacher</th>
                    <th>Schedule</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {nextSemesterOverview.savedSelections.map((item) => (
                    <tr key={item.plannedRegistrationId}>
                      <td>
                        <strong>{item.subjectCode}</strong>
                        <div>{item.subjectName}</div>
                        <div className="muted">{item.credits} credits</div>
                      </td>
                      <td>{item.teacherName || "TBA"}</td>
                      <td>
                        <div className="schedule-chip-list">
                          {item.meetingTimes.map((slot, index) => (
                            <span key={`${item.sectionId}-${index}`} className="schedule-chip">
                              {formatMeetingSlot(slot)}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="ghost-danger-btn"
                          disabled={busySectionId === item.sectionId}
                          onClick={() => handleNextSemesterAction(item.sectionId, "remove")}
                        >
                          {busySectionId === item.sectionId ? "Saving..." : "Remove"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div>
          <h4>Recommended subjects</h4>
          {nextSemesterOverview.subjects.length === 0 ? (
            <p className="muted">No next-semester curriculum subjects were found.</p>
          ) : (
            <div className="stack">
              {nextSemesterOverview.subjects.map((subject) => (
                <article key={subject.subjectId} className="registration-hold-card">
                  <div className="section-heading">
                    <div>
                      <strong>{subject.subjectCode}</strong>
                      <div>{subject.subjectName}</div>
                      <div className="muted">
                        {subject.credits} credits {subject.required ? "| required" : ""}
                      </div>
                    </div>
                    {subject.selectedSectionId ? <span className="badge">Saved</span> : null}
                  </div>
                  {subject.sections.length === 0 ? (
                    <p className="muted">No sections published yet for this subject.</p>
                  ) : (
                    <div className="registration-table-wrap">
                      <table className="table registration-table">
                        <thead>
                          <tr>
                            <th>Teacher</th>
                            <th>Schedule</th>
                            <th>Seats</th>
                            <th>Status</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {subject.sections.map((section) => (
                            <tr key={section.sectionId}>
                              <td>{section.teacherName || "TBA"}</td>
                              <td>
                                <div className="schedule-chip-list">
                                  {section.meetingTimes.map((slot, index) => (
                                    <span key={`${section.sectionId}-${index}`} className="schedule-chip">
                                      {formatMeetingSlot(slot)}
                                    </span>
                                  ))}
                                </div>
                              </td>
                              <td>
                                {section.occupiedSeats}/{section.capacity}
                              </td>
                              <td>
                                {section.selected ? (
                                  <span className="badge">Saved</span>
                                ) : section.canSelect ? (
                                  <span className="badge badge-warning">Can save</span>
                                ) : (
                                  <span className="badge badge-neutral">Blocked</span>
                                )}
                                {!section.selected && section.blockedReasons.length > 0 ? (
                                  <p className="muted compact-text">{joinReasons(section.blockedReasons)}</p>
                                ) : null}
                              </td>
                              <td>
                                {section.selected ? (
                                  <button
                                    type="button"
                                    className="ghost-danger-btn"
                                    disabled={busySectionId === section.sectionId}
                                    onClick={() => handleNextSemesterAction(section.sectionId, "remove")}
                                  >
                                    {busySectionId === section.sectionId ? "Saving..." : "Remove"}
                                  </button>
                                ) : (
                                  <button
                                    type="button"
                                    disabled={busySectionId === section.sectionId || !section.canSelect || !nextSemesterOverview.selectionEnabled}
                                    onClick={() => handleNextSemesterAction(section.sectionId, "save")}
                                  >
                                    {busySectionId === section.sectionId ? "Saving..." : "Save section"}
                                  </button>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </div>
      </section>
    );
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Registration Center</h2>
          <p className="muted">
            Course catalog, section availability, add/drop controls, FX requests, and registration window status.
          </p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/student/notifications">
            Open notifications
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
          <p>Loading registration center...</p>
        </section>
      ) : null}

      {!loading && overview ? (
        <>
          <section className="card registration-hero-card">
            <div className="registration-hero">
              <div>
                <span className="assistant-eyebrow">Current academic cycle</span>
                <h3>{overview.currentSemesterName || "No current semester"}</h3>
                <p className="muted">
                  Credits: {overview.currentCredits}
                  {overview.creditLimit ? ` / ${overview.creditLimit}` : ""}
                </p>
              </div>
              <div className="registration-hero-stats">
                <div className="registration-mini-stat">
                  <strong>{overview.currentRegistrations.length}</strong>
                  <span>Current sections</span>
                </div>
                <div className="registration-mini-stat">
                  <strong>{overview.eligibleFxCount}</strong>
                  <span>FX eligible</span>
                </div>
                <div className="registration-mini-stat">
                  <strong>{overview.fxRequestCount}</strong>
                  <span>FX requests</span>
                </div>
              </div>
            </div>

            {overview.hasRegistrationHold ? (
              <div className="registration-alert registration-alert-danger">
                <strong>Registration is currently blocked.</strong>
                <span>Resolve active holds or overdue finance items to continue.</span>
              </div>
            ) : null}

            {overview.holds.length > 0 ? (
              <div className="registration-hold-list">
                {overview.holds.map((hold) => (
                  <article key={hold.id} className="registration-hold-card">
                    <span className="badge badge-danger">{hold.type}</span>
                    <strong>{hold.reason}</strong>
                    <span className="muted">{hold.createdAt.slice(0, 10)}</span>
                  </article>
                ))}
              </div>
            ) : null}
          </section>

          <section className="card">
            <div className="registration-window-grid">
              {overview.windows.map((window) => (
                <article key={window.id} className={`window-card ${window.openNow ? "window-card-open" : ""}`}>
                  <div className="window-card-head">
                    <h3>{formatWindowType(window.type)}</h3>
                    <span className={`badge ${window.openNow ? "" : "badge-neutral"}`}>
                      {window.openNow ? "Open now" : window.active ? "Scheduled" : "Inactive"}
                    </span>
                  </div>
                  <p className="muted">{formatWindow(window)}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="card">
            <div className="segmented-control">
              {tabs.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`segment-btn ${tab === item.id ? "segment-btn-active" : ""}`}
                  onClick={() => setTab(item.id)}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </section>

          {tab === "nextSemester" ? renderNextSemesterBoard() : null}

          {tab === "catalog" ? (
            <section className="card">
              <div className="section-heading">
                <div>
                  <h3>Course Catalog</h3>
                  <p className="muted">All current-semester sections available for your program.</p>
                </div>
              </div>
              {renderCatalogRows(catalog)}
            </section>
          ) : null}

          {tab === "available" ? (
            <section className="card">
              <div className="section-heading">
                <div>
                  <h3>Available Sections</h3>
                  <p className="muted">Sections you can act on right now through registration or add/drop.</p>
                </div>
              </div>
              {availableRegistrationItems.length > 0 ? renderCatalogRows(availableRegistrationItems) : <p className="muted">No sections are currently available.</p>}
            </section>
          ) : null}

          {tab === "addDrop" ? (
            <section className="card stack">
              <div className="section-heading">
                <div>
                  <h3>Add / Drop</h3>
                  <p className="muted">Manage current sections while the add/drop window is active.</p>
                </div>
              </div>
              <div>
                <h4>Current Enrollments</h4>
                {overview.currentRegistrations.length > 0 ? (
                  renderCurrentRegistrationRows(overview.currentRegistrations)
                ) : (
                  <p className="muted">No active enrollments for the current semester.</p>
                )}
              </div>
              <div>
                <h4>Add During Add/Drop</h4>
                {addDropAdditions.length > 0 ? renderCatalogRows(addDropAdditions) : <p className="muted">No additional sections available in add/drop right now.</p>}
              </div>
            </section>
          ) : null}

          {tab === "fx" && fxOverview ? (
            <section className="card stack">
              <div className="section-heading">
                <div>
                  <h3>FX Registration</h3>
                  <p className="muted">Submit FX requests for eligible failed courses and track their status.</p>
                </div>
                <span className={`badge ${fxOverview.windowOpen ? "" : "badge-neutral"}`}>
                  {fxOverview.windowOpen ? "FX window open" : "FX window closed"}
                </span>
              </div>

              <div>
                <h4>Eligible Courses</h4>
                {fxOverview.eligibleCourses.length === 0 ? (
                  <p className="muted">No eligible FX courses right now.</p>
                ) : (
                  <div className="registration-table-wrap">
                    <table className="table registration-table">
                      <thead>
                        <tr>
                          <th>Course</th>
                          <th>Final score</th>
                          <th>Status</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {fxOverview.eligibleCourses.map((item) => (
                          <tr key={item.sectionId}>
                            <td>
                              <strong>{item.subjectCode}</strong>
                              <div>{item.subjectName}</div>
                            </td>
                            <td>{item.finalScore.toFixed(1)}</td>
                            <td>
                              {item.alreadyRequested ? (
                                <span className="badge badge-neutral">Already requested</span>
                              ) : (
                                <span className="badge">Eligible</span>
                              )}
                            </td>
                            <td>
                              <button
                                type="button"
                                onClick={() => handleAction(item.sectionId, "fx")}
                                disabled={busySectionId === item.sectionId || item.alreadyRequested || !fxOverview.windowOpen}
                              >
                                {busySectionId === item.sectionId ? "Saving..." : "Request FX"}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              <div>
                <h4>My FX Requests</h4>
                {fxOverview.registrations.length === 0 ? (
                  <p className="muted">You have not submitted any FX requests yet.</p>
                ) : (
                  <div className="registration-table-wrap">
                    <table className="table registration-table">
                      <thead>
                        <tr>
                          <th>Course</th>
                          <th>Status</th>
                          <th>Created</th>
                        </tr>
                      </thead>
                      <tbody>
                        {fxOverview.registrations.map((item) => (
                          <tr key={item.id}>
                            <td>
                              <strong>{item.subjectCode}</strong>
                              <div>{item.subjectName}</div>
                            </td>
                            <td>
                              <span className="badge badge-neutral">{item.status}</span>
                            </td>
                            <td>{item.createdAt.slice(0, 10)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

