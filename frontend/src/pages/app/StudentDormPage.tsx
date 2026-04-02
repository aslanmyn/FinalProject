import { useEffect, useState } from "react";
import {
  ApiError,
  fetchDormApplications,
  fetchDormRooms,
  createDormApplication,
  updateDormStep1,
  updateDormStep2,
  updateDormStep3,
  submitDormApplication,
  cancelDormApplication
} from "../../lib/api";
import type { DormApplication, DormRoom } from "../../types/dorm";

const SLEEP_OPTIONS = ["Early Bird (Before 10 PM)", "Night Owl (After 12 AM)", "Flexible"];
const STUDY_OPTIONS = ["Quiet / Silent", "Background Music OK", "Flexible"];

const STEP_LABELS = ["Personal Info", "Room Selection", "Roommate Preferences", "Review & Submit"];

function formatPrice(price: number): string {
  return price.toLocaleString("en-US") + " \u20B8";
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
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
  const [error, setError] = useState<string | null>(null);
  const [activeApp, setActiveApp] = useState<DormApplication | null>(null);
  const [saving, setSaving] = useState(false);

  // form state
  const [emergencyName, setEmergencyName] = useState("");
  const [emergencyPhone, setEmergencyPhone] = useState("");
  const [specialNeeds, setSpecialNeeds] = useState("");
  const [roomType, setRoomType] = useState<"SINGLE_SUITE" | "DOUBLE_ROOM">("SINGLE_SUITE");
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [sleepSchedule, setSleepSchedule] = useState(SLEEP_OPTIONS[0]);
  const [studyEnv, setStudyEnv] = useState(STUDY_OPTIONS[0]);
  const [roommateUid, setRoommateUid] = useState("");
  const [termsAccepted, setTermsAccepted] = useState(false);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [apps, rms] = await Promise.all([fetchDormApplications(), fetchDormRooms()]);
      setApplications(apps);
      setRooms(rms);
      const draft = apps.find((a) => a.status === "DRAFT");
      if (draft) {
        setActiveApp(draft);
        populateForm(draft);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load data");
    } finally {
      setLoading(false);
    }
  }

  function populateForm(app: DormApplication) {
    setEmergencyName(app.emergencyContactName || "");
    setEmergencyPhone(app.emergencyContactPhone || "");
    setSpecialNeeds(app.specialNeeds || "");
    setRoomType(app.roomTypePreference || "SINGLE_SUITE");
    setSelectedRoomId(app.dormRoomId);
    setSleepSchedule(app.sleepSchedule || SLEEP_OPTIONS[0]);
    setStudyEnv(app.studyEnvironment || STUDY_OPTIONS[0]);
    setRoommateUid(app.preferredRoommateUid || "");
    setTermsAccepted(app.termsAccepted);
  }

  useEffect(() => {
    void loadData();
  }, []);

  async function handleCreate() {
    setSaving(true);
    try {
      const app = await createDormApplication();
      setActiveApp(app);
      populateForm(app);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create application");
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveStep1() {
    if (!activeApp) return;
    setSaving(true);
    try {
      const updated = await updateDormStep1(activeApp.id, {
        emergencyContactName: emergencyName,
        emergencyContactPhone: emergencyPhone,
        specialNeeds
      });
      setActiveApp(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save");
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveStep2() {
    if (!activeApp) return;
    setSaving(true);
    try {
      const updated = await updateDormStep2(activeApp.id, {
        roomTypePreference: roomType,
        dormRoomId: selectedRoomId
      });
      setActiveApp(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save");
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveStep3() {
    if (!activeApp) return;
    setSaving(true);
    try {
      const updated = await updateDormStep3(activeApp.id, {
        sleepSchedule,
        studyEnvironment: studyEnv,
        preferredRoommateUid: roommateUid
      });
      setActiveApp(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save");
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmit() {
    if (!activeApp) return;
    setSaving(true);
    try {
      const updated = await submitDormApplication(activeApp.id, termsAccepted);
      setActiveApp(null);
      await loadData();
      setError(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to submit");
    } finally {
      setSaving(false);
    }
  }

  async function handleCancel(id: number) {
    setSaving(true);
    try {
      await cancelDormApplication(id);
      setActiveApp(null);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to cancel");
    } finally {
      setSaving(false);
    }
  }

  const step = activeApp?.currentStep ?? 1;
  const progressPercent = activeApp ? Math.round(((step) / 4) * 100) : 0;
  const filteredRooms = rooms.filter((r) => r.roomType === roomType && r.hasSpace);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Dorm Registration</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <div className="banner banner-danger">{error}</div> : null}

      {!loading && !activeApp ? (
        <>
          {/* Existing applications */}
          {applications.length > 0 ? (
            <section className="card">
              <h3>Your Applications</h3>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Status</th>
                      <th>Room Type</th>
                      <th>Step</th>
                      <th>Created</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {applications.map((app) => (
                      <tr key={app.id}>
                        <td>#{app.id}</td>
                        <td>
                          <span className={`badge badge-${app.status === "APPROVED" ? "success" : app.status === "REJECTED" || app.status === "CANCELLED" ? "danger" : "info"}`}>
                            {app.status}
                          </span>
                        </td>
                        <td>{app.roomTypePreference || "—"}</td>
                        <td>{app.currentStep}/4</td>
                        <td>{formatDate(app.createdAt)}</td>
                        <td>
                          {app.status === "DRAFT" ? (
                            <button className="btn btn-sm" onClick={() => { setActiveApp(app); populateForm(app); }}>Continue</button>
                          ) : null}
                          {app.status !== "CANCELLED" ? (
                            <button className="btn btn-sm btn-danger" onClick={() => handleCancel(app.id)} disabled={saving}>Cancel</button>
                          ) : null}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {!applications.some((a) => a.status === "DRAFT" || a.status === "SUBMITTED" || a.status === "APPROVED") ? (
            <section className="card" style={{ textAlign: "center", padding: "2rem" }}>
              <h3>Ready to apply for dormitory housing?</h3>
              <p className="muted" style={{ marginBottom: "1rem" }}>Start your application by filling out a simple 4-step form.</p>
              <button className="btn btn-primary" onClick={handleCreate} disabled={saving}>
                {saving ? "Creating..." : "Start Application"}
              </button>
            </section>
          ) : null}
        </>
      ) : null}

      {/* Multi-step form */}
      {activeApp && activeApp.status === "DRAFT" ? (
        <>
          {/* Progress bar */}
          <section className="card">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.5rem" }}>
              <span className="badge badge-info">STEP {step} OF 4</span>
              <span className="muted">{progressPercent}% Complete</span>
            </div>
            <div style={{ background: "var(--border)", borderRadius: 6, height: 8, overflow: "hidden" }}>
              <div style={{ width: `${progressPercent}%`, height: "100%", background: "var(--accent)", borderRadius: 6, transition: "width 0.3s" }} />
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: "0.75rem" }}>
              {STEP_LABELS.map((label, i) => (
                <span key={i} style={{ fontSize: "0.75rem", color: i + 1 <= step ? "var(--accent)" : "var(--text-secondary)", fontWeight: i + 1 === step ? 600 : 400 }}>
                  {label}
                </span>
              ))}
            </div>
          </section>

          {/* Step 1: Personal Info */}
          {step >= 1 && step < 4 ? (
            <section className="card" style={{ display: step === 1 ? "block" : "none" }}>
              <h3>Step 1: Personal Information</h3>
              <div className="form-group">
                <label>Emergency Contact Name</label>
                <input type="text" className="input" value={emergencyName} onChange={(e) => setEmergencyName(e.target.value)} placeholder="Full name" />
              </div>
              <div className="form-group">
                <label>Emergency Contact Phone</label>
                <input type="tel" className="input" value={emergencyPhone} onChange={(e) => setEmergencyPhone(e.target.value)} placeholder="+7 (___) ___-__-__" />
              </div>
              <div className="form-group">
                <label>Special Needs (optional)</label>
                <textarea className="input" rows={3} value={specialNeeds} onChange={(e) => setSpecialNeeds(e.target.value)} placeholder="Allergies, disabilities, dietary requirements..." />
              </div>
              <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
                <button className="btn btn-primary" onClick={handleSaveStep1} disabled={saving}>
                  {saving ? "Saving..." : "Save & Continue"}
                </button>
              </div>
            </section>
          ) : null}

          {/* Step 2: Room Selection */}
          {step >= 2 && step < 4 ? (
            <section className="card" style={{ display: step === 2 ? "block" : "none" }}>
              <h3>Step 2: Room Selection</h3>
              <div className="form-group">
                <label>Select Room Type</label>
                <div style={{ display: "flex", gap: "1rem", marginTop: "0.5rem" }}>
                  {(["SINGLE_SUITE", "DOUBLE_ROOM"] as const).map((type) => (
                    <div
                      key={type}
                      onClick={() => setRoomType(type)}
                      style={{
                        flex: 1,
                        border: `2px solid ${roomType === type ? "var(--accent)" : "var(--border)"}`,
                        borderRadius: 12,
                        padding: "1rem",
                        cursor: "pointer",
                        background: roomType === type ? "var(--accent-subtle)" : "var(--card)"
                      }}
                    >
                      <strong style={{ color: roomType === type ? "var(--accent)" : "var(--text)" }}>
                        {type === "SINGLE_SUITE" ? "Single Suite" : "Double Room"}
                      </strong>
                      <p className="muted" style={{ fontSize: "0.85rem", margin: "0.25rem 0" }}>
                        {type === "SINGLE_SUITE" ? "Private bedroom, shared living area" : "Shared bedroom with one roommate"}
                      </p>
                      <strong style={{ color: "var(--accent)" }}>
                        {type === "SINGLE_SUITE" ? formatPrice(550000) : formatPrice(390000)}
                      </strong>
                      <span className="muted"> /semester</span>
                    </div>
                  ))}
                </div>
              </div>

              {filteredRooms.length > 0 ? (
                <div className="form-group">
                  <label>Available Rooms (optional — select specific room)</label>
                  <select className="input" value={selectedRoomId ?? ""} onChange={(e) => setSelectedRoomId(e.target.value ? Number(e.target.value) : null)}>
                    <option value="">Auto-assign</option>
                    {filteredRooms.map((r) => (
                      <option key={r.id} value={r.id}>
                        Room {r.roomNumber} — Floor {r.floor} ({r.occupied}/{r.capacity} occupied)
                      </option>
                    ))}
                  </select>
                </div>
              ) : null}

              <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
                <button className="btn" onClick={() => setActiveApp({ ...activeApp, currentStep: 1 })}>Back</button>
                <button className="btn btn-primary" onClick={handleSaveStep2} disabled={saving}>
                  {saving ? "Saving..." : "Save & Continue"}
                </button>
              </div>
            </section>
          ) : null}

          {/* Step 3: Roommate Preferences */}
          {step >= 3 && step < 4 ? (
            <section className="card" style={{ display: step === 3 ? "block" : "none" }}>
              <h3>Step 3: Roommate Preferences</h3>
              <div className="form-group">
                <label>Sleep Schedule</label>
                <select className="input" value={sleepSchedule} onChange={(e) => setSleepSchedule(e.target.value)}>
                  {SLEEP_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Study Environment</label>
                <select className="input" value={studyEnv} onChange={(e) => setStudyEnv(e.target.value)}>
                  {STUDY_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                  Preferred Roommate UID (optional)
                </label>
                <input type="text" className="input" value={roommateUid} onChange={(e) => setRoommateUid(e.target.value)} placeholder="Enter roommate's UID" />
              </div>
              <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
                <button className="btn" onClick={() => setActiveApp({ ...activeApp, currentStep: 2 })}>Back</button>
                <button className="btn btn-primary" onClick={handleSaveStep3} disabled={saving}>
                  {saving ? "Saving..." : "Save & Continue"}
                </button>
              </div>
            </section>
          ) : null}

          {/* Step 4: Review & Submit */}
          {step >= 4 ? (
            <section className="card" style={{ display: "block" }}>
              <h3>Step 4: Review & Submit</h3>
              <div className="kv-grid" style={{ marginBottom: "1rem" }}>
                <div><strong>Room Type:</strong> {roomType === "SINGLE_SUITE" ? "Single Suite" : "Double Room"}</div>
                <div><strong>Sleep Schedule:</strong> {sleepSchedule}</div>
                <div><strong>Study Environment:</strong> {studyEnv}</div>
                <div><strong>Emergency Contact:</strong> {emergencyName} ({emergencyPhone})</div>
                {roommateUid ? <div><strong>Preferred Roommate:</strong> {roommateUid}</div> : null}
                {specialNeeds ? <div><strong>Special Needs:</strong> {specialNeeds}</div> : null}
              </div>

              <div className="card" style={{ background: "var(--bg)", fontSize: "0.85rem", maxHeight: 150, overflow: "auto" }}>
                <p>
                  By submitting this application, I agree to the university housing policies and residential life handbook.
                  I understand that housing assignments are based on availability and priority status.
                  Cancellation fees apply after the priority deadline of June 15th.
                  I certify that all information provided is accurate and complete.
                </p>
              </div>
              <div className="form-group" style={{ marginTop: "0.75rem" }}>
                <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
                  <input type="checkbox" checked={termsAccepted} onChange={(e) => setTermsAccepted(e.target.checked)} />
                  I agree to the terms and conditions
                </label>
              </div>
              <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
                <button className="btn" onClick={() => setActiveApp({ ...activeApp, currentStep: 3 })}>Back</button>
                <button className="btn btn-primary" onClick={handleSubmit} disabled={saving || !termsAccepted}>
                  {saving ? "Submitting..." : "Submit Application"}
                </button>
              </div>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
