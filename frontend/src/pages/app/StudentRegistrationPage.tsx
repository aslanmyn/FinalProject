import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  addStudentCourse,
  ApiError,
  createStudentFxRegistration,
  dropStudentCourse,
  fetchStudentFxOverview,
  fetchStudentRegistrationCatalog,
  fetchStudentRegistrationOverview,
  submitStudentRegistration
} from "../../lib/api";
import type {
  StudentCourseCatalogItem,
  StudentFxOverview,
  StudentRegistrationBoardItem,
  StudentRegistrationMeetingSlot,
  StudentRegistrationOverview,
  StudentWindowStatusItem
} from "../../types/student";

type RegistrationTab = "catalog" | "available" | "addDrop" | "fx";

const tabs: Array<{ id: RegistrationTab; label: string }> = [
  { id: "catalog", label: "Course Catalog" },
  { id: "available", label: "Available Sections" },
  { id: "addDrop", label: "Add / Drop" },
  { id: "fx", label: "FX Registration" }
];

function formatWindowType(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char: string) => char.toUpperCase());
}

function formatMeetingSlot(slot: StudentRegistrationMeetingSlot): string {
  const day = slot.dayOfWeek.slice(0, 3);
  return `${day} ${slot.startTime.slice(0, 5)}-${slot.endTime.slice(0, 5)}${slot.room ? ` | ${slot.room}` : ""}`;
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [tab, setTab] = useState<RegistrationTab>("catalog");
  const [busySectionId, setBusySectionId] = useState<number | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [overviewData, catalogData, fxData] = await Promise.all([
        fetchStudentRegistrationOverview(),
        fetchStudentRegistrationCatalog(),
        fetchStudentFxOverview()
      ]);
      setOverview(overviewData);
      setCatalog(catalogData);
      setFxOverview(fxData);
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

