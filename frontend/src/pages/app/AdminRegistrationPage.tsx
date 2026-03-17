import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  fetchAdminFx,
  fetchAdminHolds,
  fetchAdminWindows,
  updateAdminFxStatus
} from "../../lib/api";
import type { AdminFxItem, AdminHoldItem, AdminWindowItem } from "../../types/admin";

const fxStatuses = ["PENDING", "APPROVED", "PAID", "CONFIRMED"] as const;

function formatWindowType(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char: string) => char.toUpperCase());
}

export default function AdminRegistrationPage() {
  const [windows, setWindows] = useState<AdminWindowItem[]>([]);
  const [fxItems, setFxItems] = useState<AdminFxItem[]>([]);
  const [holds, setHolds] = useState<AdminHoldItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busyFxId, setBusyFxId] = useState<number | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [windowsData, fxData, holdsData] = await Promise.all([
        fetchAdminWindows(),
        fetchAdminFx(),
        fetchAdminHolds()
      ]);
      setWindows(windowsData);
      setFxItems(fxData);
      setHolds(holdsData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load registration operations");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  const sortedWindows = useMemo(
    () => [...windows].sort((left, right) => `${right.startDate}${right.type}`.localeCompare(`${left.startDate}${left.type}`)),
    [windows]
  );

  async function handleFxStatusChange(fxId: number, status: (typeof fxStatuses)[number]) {
    setBusyFxId(fxId);
    setError(null);
    setSuccess(null);
    try {
      await updateAdminFxStatus(fxId, status);
      setSuccess(`FX #${fxId} updated to ${status}`);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update FX status");
    } finally {
      setBusyFxId(null);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Registration Operations</h2>
          <p className="muted">Monitor registration windows, active holds, and FX processing from one place.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/admin/academic">
            Academic setup
          </Link>
          <Link className="link-btn" to="/app/admin/finance">
            Finance actions
          </Link>
        </div>
      </header>

      {loading ? <section className="card"><p>Loading registration operations...</p></section> : null}
      {error ? <section className="card"><p className="error">{error}</p></section> : null}
      {success ? <section className="card"><p className="success">{success}</p></section> : null}

      {!loading ? (
        <>
          <section className="card">
            <div className="stats-grid">
              <div className="stat-card">
                <strong>{windows.length}</strong>
                <span>Configured windows</span>
              </div>
              <div className="stat-card">
                <strong>{fxItems.length}</strong>
                <span>FX applications</span>
              </div>
              <div className="stat-card">
                <strong>{holds.length}</strong>
                <span>Active holds</span>
              </div>
            </div>
          </section>

          <section className="card">
            <div className="section-heading">
              <div>
                <h3>Registration Windows</h3>
                <p className="muted">Current and scheduled windows across semesters.</p>
              </div>
            </div>
            <div className="registration-table-wrap">
              <table className="table registration-table">
                <thead>
                  <tr>
                    <th>Semester</th>
                    <th>Window</th>
                    <th>Range</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedWindows.map((window) => (
                    <tr key={window.id}>
                      <td>{window.semesterName || "No semester"}</td>
                      <td>{formatWindowType(window.type)}</td>
                      <td>
                        {window.startDate} {" -> "} {window.endDate}
                      </td>
                      <td>
                        <span className={`badge ${window.active ? "" : "badge-neutral"}`}>
                          {window.active ? "Active" : "Inactive"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card">
            <div className="section-heading">
              <div>
                <h3>FX Queue</h3>
                <p className="muted">Approve, track payment state, and confirm FX requests.</p>
              </div>
            </div>
            <div className="registration-table-wrap">
              <table className="table registration-table">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Course</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th>Update</th>
                  </tr>
                </thead>
                <tbody>
                  {fxItems.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.studentName}</strong>
                        <div className="muted">#{item.studentId}</div>
                      </td>
                      <td>
                        <strong>{item.subjectCode}</strong>
                        <div>{item.subjectName}</div>
                      </td>
                      <td>
                        <span className="badge badge-neutral">{item.status}</span>
                      </td>
                      <td>{item.createdAt.slice(0, 10)}</td>
                      <td>
                        <select
                          value={item.status}
                          onChange={(event) =>
                            void handleFxStatusChange(item.id, event.target.value as (typeof fxStatuses)[number])
                          }
                          disabled={busyFxId === item.id}
                        >
                          {fxStatuses.map((status) => (
                            <option key={status} value={status}>
                              {status}
                            </option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card">
            <div className="section-heading">
              <div>
                <h3>Active Holds</h3>
                <p className="muted">Current student restrictions affecting registration and finance flows.</p>
              </div>
            </div>
            <div className="registration-table-wrap">
              <table className="table registration-table">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Type</th>
                    <th>Reason</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {holds.map((hold) => (
                    <tr key={hold.id}>
                      <td>
                        <strong>{hold.studentName}</strong>
                        <div className="muted">#{hold.studentId}</div>
                      </td>
                      <td>
                        <span className="badge badge-danger">{hold.type}</span>
                      </td>
                      <td>{hold.reason}</td>
                      <td>{hold.createdAt.slice(0, 10)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}

