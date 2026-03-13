import { useEffect, useState } from "react";
import { ApiError, assignAdminRequest, fetchAdminRequests, updateAdminRequestStatus } from "../../lib/api";
import type { AdminRequestItem } from "../../types/admin";

const requestStatuses = ["NEW", "IN_REVIEW", "NEED_INFO", "APPROVED", "REJECTED", "DONE"] as const;

export default function AdminRequestsPage() {
  const [items, setItems] = useState<AdminRequestItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [statusById, setStatusById] = useState<Record<number, string>>({});
  const [assigneeById, setAssigneeById] = useState<Record<number, string>>({});

  async function loadRequests() {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchAdminRequests(0, 100);
      setItems(page.items);
      setStatusById(
        page.items.reduce<Record<number, string>>((acc, item) => {
          acc[item.id] = item.status;
          return acc;
        }, {})
      );
      setAssigneeById(
        page.items.reduce<Record<number, string>>((acc, item) => {
          acc[item.id] = item.assignedToUserId ? String(item.assignedToUserId) : "";
          return acc;
        }, {})
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load requests");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadRequests();
  }, []);

  async function handleAssign(requestId: number) {
    const assigneeRaw = assigneeById[requestId];
    if (!assigneeRaw) {
      setError("Enter assignee user ID");
      return;
    }
    setError(null);
    setSuccess(null);
    try {
      await assignAdminRequest(requestId, Number(assigneeRaw));
      setSuccess(`Request #${requestId} assigned`);
      await loadRequests();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to assign request");
    }
  }

  async function handleStatusUpdate(requestId: number) {
    const status = statusById[requestId];
    if (!status) return;
    setError(null);
    setSuccess(null);
    try {
      await updateAdminRequestStatus(requestId, status as (typeof requestStatuses)[number]);
      setSuccess(`Request #${requestId} status updated`);
      await loadRequests();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update status");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Admin Requests</h2>
      </header>

      <section className="card">
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        {!loading && !error && items.length === 0 ? <p className="muted">No requests.</p> : null}

        {!loading && !error && items.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Category</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Updated</th>
                  <th>Assigned To</th>
                  <th>Assign</th>
                  <th>Set Status</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.id}</td>
                    <td>{item.category}</td>
                    <td>{item.status}</td>
                    <td>{item.createdAt}</td>
                    <td>{item.updatedAt}</td>
                    <td>{item.assignedToUserId ?? "-"}</td>
                    <td>
                      <div className="inline-form">
                        <input
                          type="number"
                          placeholder="User ID"
                          value={assigneeById[item.id] ?? ""}
                          onChange={(event) =>
                            setAssigneeById((prev) => ({ ...prev, [item.id]: event.target.value }))
                          }
                        />
                        <button type="button" onClick={() => handleAssign(item.id)}>
                          Assign
                        </button>
                      </div>
                    </td>
                    <td>
                      <div className="inline-form">
                        <select
                          value={statusById[item.id] ?? item.status}
                          onChange={(event) =>
                            setStatusById((prev) => ({ ...prev, [item.id]: event.target.value }))
                          }
                        >
                          {requestStatuses.map((status) => (
                            <option key={status} value={status}>
                              {status}
                            </option>
                          ))}
                        </select>
                        <button type="button" onClick={() => handleStatusUpdate(item.id)}>
                          Update
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </div>
  );
}
