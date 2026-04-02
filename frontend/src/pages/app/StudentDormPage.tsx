import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  cancelDormApplication,
  createDormApplication,
  fetchDormApplications,
  fetchDormRooms,
  submitDormApplication,
  updateDormStep1,
  updateDormStep2,
  updateDormStep3
} from "../../lib/api";
import type { DormApplication, DormRoom } from "../../types/dorm";

const SLEEP_OPTIONS = ["Early Bird", "Night Owl", "Flexible"];
const STUDY_OPTIONS = ["Quiet", "Background Music OK", "Flexible"];
const STEP_LABELS = ["Personal", "Room", "Preferences", "Review"];

function formatPrice(price: number): string {
  return `${price.toLocaleString("en-US")} KZT`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export default function StudentDormPage() {
  const [applications, setApplications] = useState<DormApplication[]>([]);
  const [rooms, setRooms] = useState<DormRoom[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeApp, setActiveApp] = useState<DormApplication | null>(null);

  const [emergencyName, setEmergencyName] = useState("");
  const [emergencyPhone, setEmergencyPhone] = useState("");
  const [specialNeeds, setSpecialNeeds] = useState("");
  const [roomType, setRoomType] = useState<"SINGLE_SUITE" | "DOUBLE_ROOM">("SINGLE_SUITE");
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [sleepSchedule, setSleepSchedule] = useState(SLEEP_OPTIONS[0]);
  const [studyEnvironment, setStudyEnvironment] = useState(STUDY_OPTIONS[0]);
  const [preferredRoommateUid, setPreferredRoommateUid] = useState("");
  const [termsAccepted, setTermsAccepted] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [loadedApplications, loadedRooms] = await Promise.all([
        fetchDormApplications(),
        fetchDormRooms()
      ]);
      setApplications(loadedApplications);
      setRooms(loadedRooms);

      const draft = loadedApplications.find((item) => item.status === "DRAFT") ?? null;
      setActiveApp(draft);
      if (draft) {
        populateForm(draft);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load dorm data");
    } finally {
      setLoading(false);
    }
  }, []);

  function populateForm(application: DormApplication) {
    setEmergencyName(application.emergencyContactName || "");
    setEmergencyPhone(application.emergencyContactPhone || "");
    setSpecialNeeds(application.specialNeeds || "");
    setRoomType(application.roomTypePreference || "SINGLE_SUITE");
    setSelectedRoomId(application.dormRoomId);
    setSleepSchedule(application.sleepSchedule || SLEEP_OPTIONS[0]);
    setStudyEnvironment(application.studyEnvironment || STUDY_OPTIONS[0]);
    setPreferredRoommateUid(application.preferredRoommateUid || "");
    setTermsAccepted(application.termsAccepted);
  }

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const currentStep = activeApp?.currentStep ?? 1;
  const filteredRooms = useMemo(
    () => rooms.filter((room) => room.roomType === roomType && room.hasSpace),
    [rooms, roomType]
  );

  async function handleCreate() {
    setSaving(true);
    setError(null);
    try {
      const created = await createDormApplication();
      setActiveApp(created);
      populateForm(created);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create dorm application");
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveStep1() {
    if (!activeApp) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await updateDormStep1(activeApp.id, {
        emergencyContactName: emergencyName,
        emergencyContactPhone: emergencyPhone,
        specialNeeds
      });
      setActiveApp(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save personal details");
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveStep2() {
    if (!activeApp) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await updateDormStep2(activeApp.id, {
        roomTypePreference: roomType,
        dormRoomId: selectedRoomId
      });
      setActiveApp(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save room choice");
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveStep3() {
    if (!activeApp) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await updateDormStep3(activeApp.id, {
        sleepSchedule,
        studyEnvironment,
        preferredRoommateUid
      });
      setActiveApp(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save preferences");
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmit() {
    if (!activeApp) return;
    setSaving(true);
    setError(null);
    try {
      await submitDormApplication(activeApp.id, termsAccepted);
      setActiveApp(null);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to submit dorm application");
    } finally {
      setSaving(false);
    }
  }

  async function handleCancel(applicationId: number) {
    setSaving(true);
    setError(null);
    try {
      await cancelDormApplication(applicationId);
      if (activeApp?.id === applicationId) {
        setActiveApp(null);
      }
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to cancel dorm application");
    } finally {
      setSaving(false);
    }
  }

  const progressPercent = activeApp ? Math.round((currentStep / 4) * 100) : 0;

  return (
    <div className="screen app-screen">
      <section className="card service-hero">
        <div className="service-hero-copy">
          <span className="auth-kicker">Campus Life</span>
          <h2>Dorm registration</h2>
          <p className="muted">
            Create, review, and submit a multi-step housing application without leaving the student portal.
          </p>
        </div>
        <div className="service-hero-metrics">
          <div className="service-metric-card">
            <span>Applications</span>
            <strong>{applications.length}</strong>
          </div>
          <div className="service-metric-card">
            <span>Available rooms</span>
            <strong>{filteredRooms.length}</strong>
          </div>
          <div className="service-metric-card">
            <span>Draft status</span>
            <strong>{activeApp ? `Step ${currentStep}/4` : "No draft"}</strong>
          </div>
        </div>
      </section>

      {loading ? <p>Loading dorm data...</p> : null}
      {error ? <div className="banner banner-danger">{error}</div> : null}

      {!loading && !activeApp ? (
        <>
          {applications.length > 0 ? (
            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>Your applications</h3>
                  <p className="muted">Resume drafts or review submitted housing requests.</p>
                </div>
              </div>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Status</th>
                      <th>Room type</th>
                      <th>Step</th>
                      <th>Created</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {applications.map((application) => (
                      <tr key={application.id}>
                        <td>#{application.id}</td>
                        <td>
                          <span className={`badge ${application.status === "APPROVED" ? "badge-success" : application.status === "REJECTED" || application.status === "CANCELLED" ? "badge-danger" : "badge-info"}`}>
                            {application.status}
                          </span>
                        </td>
                        <td>{application.roomTypePreference || "-"}</td>
                        <td>{application.currentStep}/4</td>
                        <td>{formatDate(application.createdAt)}</td>
                        <td>
                          <div className="inline-actions">
                            {application.status === "DRAFT" ? (
                              <button
                                className="btn btn-sm"
                                onClick={() => {
                                  setActiveApp(application);
                                  populateForm(application);
                                }}
                              >
                                Continue
                              </button>
                            ) : null}
                            {application.status !== "CANCELLED" ? (
                              <button className="btn btn-sm btn-danger" onClick={() => void handleCancel(application.id)} disabled={saving}>
                                Cancel
                              </button>
                            ) : null}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {!applications.some((item) => ["DRAFT", "SUBMITTED", "APPROVED"].includes(item.status)) ? (
            <section className="card service-empty-card">
              <h3>Ready to apply for housing?</h3>
              <p className="muted">Start a four-step dorm application and keep everything in one workflow.</p>
              <button className="btn btn-primary" onClick={() => void handleCreate()} disabled={saving}>
                {saving ? "Creating..." : "Start application"}
              </button>
            </section>
          ) : null}
        </>
      ) : null}

      {activeApp && activeApp.status === "DRAFT" ? (
        <>
          <section className="card">
            <div className="service-section-header">
              <div>
                <h3>Application progress</h3>
                <p className="muted">Complete each step in order and review the details before submitting.</p>
              </div>
              <span className="badge badge-info">Step {currentStep} of 4</span>
            </div>
            <div className="progress-track">
              <div className="progress-fill" style={{ width: `${progressPercent}%` }} />
            </div>
            <div className="service-progress-labels">
              {STEP_LABELS.map((label, index) => (
                <span key={label} className={index + 1 === currentStep ? "is-current" : index + 1 < currentStep ? "is-complete" : ""}>
                  {label}
                </span>
              ))}
            </div>
          </section>

          {currentStep === 1 ? (
            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>Step 1 - personal details</h3>
                  <p className="muted">Provide emergency and support information for the housing team.</p>
                </div>
              </div>
              <div className="service-form-grid">
                <div className="form-group">
                  <label>Emergency contact name</label>
                  <input className="input" value={emergencyName} onChange={(e) => setEmergencyName(e.target.value)} />
                </div>
                <div className="form-group">
                  <label>Emergency contact phone</label>
                  <input className="input" value={emergencyPhone} onChange={(e) => setEmergencyPhone(e.target.value)} />
                </div>
              </div>
              <div className="form-group">
                <label>Special needs</label>
                <textarea
                  className="input"
                  rows={4}
                  value={specialNeeds}
                  onChange={(e) => setSpecialNeeds(e.target.value)}
                  placeholder="Optional information for accommodation support"
                />
              </div>
              <div className="service-footer-actions">
                <button className="btn btn-primary" onClick={() => void handleSaveStep1()} disabled={saving}>
                  {saving ? "Saving..." : "Save and continue"}
                </button>
              </div>
            </section>
          ) : null}

          {currentStep === 2 ? (
            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>Step 2 - choose a room type</h3>
                  <p className="muted">Pick your housing preference and optionally reserve a specific room with space.</p>
                </div>
              </div>
              <div className="service-card-grid service-card-grid-compact">
                {(["SINGLE_SUITE", "DOUBLE_ROOM"] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    className={`service-list-item${roomType === type ? " is-active" : ""}`}
                    onClick={() => setRoomType(type)}
                  >
                    <div className="service-list-item-top">
                      <strong>{type === "SINGLE_SUITE" ? "Single suite" : "Double room"}</strong>
                      <span className="badge badge-info">{type === "SINGLE_SUITE" ? formatPrice(550000) : formatPrice(390000)}</span>
                    </div>
                    <p className="muted">
                      {type === "SINGLE_SUITE"
                        ? "Private bedroom with shared living area."
                        : "Shared bedroom with one roommate."}
                    </p>
                  </button>
                ))}
              </div>

              <div className="form-group" style={{ marginTop: 16 }}>
                <label>Specific room (optional)</label>
                <select
                  className="input"
                  value={selectedRoomId ?? ""}
                  onChange={(e) => setSelectedRoomId(e.target.value ? Number(e.target.value) : null)}
                >
                  <option value="">Auto-assign the best available room</option>
                  {filteredRooms.map((room) => (
                    <option key={room.id} value={room.id}>
                      Room {room.roomNumber} - Floor {room.floor} ({room.occupied}/{room.capacity} occupied)
                    </option>
                  ))}
                </select>
              </div>

              <div className="service-footer-actions">
                <button className="btn" onClick={() => setActiveApp({ ...activeApp, currentStep: 1 })}>
                  Back
                </button>
                <button className="btn btn-primary" onClick={() => void handleSaveStep2()} disabled={saving}>
                  {saving ? "Saving..." : "Save and continue"}
                </button>
              </div>
            </section>
          ) : null}

          {currentStep === 3 ? (
            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>Step 3 - roommate preferences</h3>
                  <p className="muted">Share your sleep and study habits to improve the room matching process.</p>
                </div>
              </div>
              <div className="service-form-grid">
                <div className="form-group">
                  <label>Sleep schedule</label>
                  <select className="input" value={sleepSchedule} onChange={(e) => setSleepSchedule(e.target.value)}>
                    {SLEEP_OPTIONS.map((option) => (
                      <option key={option} value={option}>{option}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Study environment</label>
                  <select className="input" value={studyEnvironment} onChange={(e) => setStudyEnvironment(e.target.value)}>
                    {STUDY_OPTIONS.map((option) => (
                      <option key={option} value={option}>{option}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label>Preferred roommate UID</label>
                <input
                  className="input"
                  value={preferredRoommateUid}
                  onChange={(e) => setPreferredRoommateUid(e.target.value)}
                  placeholder="Optional UID"
                />
              </div>
              <div className="service-footer-actions">
                <button className="btn" onClick={() => setActiveApp({ ...activeApp, currentStep: 2 })}>
                  Back
                </button>
                <button className="btn btn-primary" onClick={() => void handleSaveStep3()} disabled={saving}>
                  {saving ? "Saving..." : "Save and continue"}
                </button>
              </div>
            </section>
          ) : null}

          {currentStep >= 4 ? (
            <section className="card">
              <div className="service-section-header">
                <div>
                  <h3>Step 4 - review and submit</h3>
                  <p className="muted">Double-check your application before sending it to the housing office.</p>
                </div>
              </div>
              <div className="kv-grid">
                <div><strong>Room type:</strong> {roomType === "SINGLE_SUITE" ? "Single suite" : "Double room"}</div>
                <div><strong>Sleep schedule:</strong> {sleepSchedule}</div>
                <div><strong>Study environment:</strong> {studyEnvironment}</div>
                <div><strong>Emergency contact:</strong> {emergencyName || "-"} ({emergencyPhone || "-"})</div>
                <div><strong>Preferred roommate:</strong> {preferredRoommateUid || "-"}</div>
                <div><strong>Special needs:</strong> {specialNeeds || "-"}</div>
              </div>

              <div className="service-route-box">
                <p className="muted">
                  By submitting this application, you confirm that the information is accurate and you agree to the university housing terms.
                </p>
                <label className="service-checkbox">
                  <input type="checkbox" checked={termsAccepted} onChange={(e) => setTermsAccepted(e.target.checked)} />
                  <span>I agree to the housing terms and conditions.</span>
                </label>
              </div>

              <div className="service-footer-actions">
                <button className="btn" onClick={() => setActiveApp({ ...activeApp, currentStep: 3 })}>
                  Back
                </button>
                <button className="btn btn-primary" onClick={() => void handleSubmit()} disabled={saving || !termsAccepted}>
                  {saving ? "Submitting..." : "Submit application"}
                </button>
              </div>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

