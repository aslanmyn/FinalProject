import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchStudentWorkflows } from "../../lib/api";
import type { WorkflowItem, WorkflowOverview, WorkflowType } from "../../types/common";

type FilterType = "ALL" | WorkflowType;

function formatWorkflowType(value: WorkflowType): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatStatus(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatDate(value: string | null): string {
  if (!value) return "-";
  return value.slice(0, 10);
}

function statusBadgeClass(item: WorkflowItem): string {
  if (item.overdue) return "badge badge-danger";
  if (item.status === "NEED_INFO" || item.status === "REJECTED") return "badge badge-warning";
  return "badge badge-neutral";
}

export default function StudentWorkflowsPage() {
  const [workflowOverview, setWorkflowOverview] = useState<WorkflowOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState<FilterType>("ALL");
  const [onlyOverdue, setOnlyOverdue] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentWorkflows();
        if (!cancelled) {
          setWorkflowOverview(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load workflows");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  const items = workflowOverview?.items ?? [];
  const visibleItems = useMemo(
    () =>
      items.filter((item) => {
        if (typeFilter !== "ALL" && item.type !== typeFilter) {
          return false;
        }
        if (onlyOverdue && !item.overdue) {
          return false;
        }
        return true;
      }),
    [items, onlyOverdue, typeFilter]
  );

  const typeOptions = useMemo(() => Array.from(new Set(items.map((item) => item.type))), [items]);
  const overdueCount = useMemo(() => items.filter((item) => item.overdue).length, [items]);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Workflow Tracker</h2>
          <p className="muted">Track active requests, FX, mobility, clearance, and registration actions with deadlines.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/student/registration">
            Registration center
          </Link>
          <Link className="link-btn" to="/app/student/assistant">
            Ask AI assistant
          </Link>
        </div>
      </header>

      {loading ? (
        <section className="card">
          <p>Loading workflows...</p>
        </section>
      ) : null}

      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      {!loading && !error ? (
        <>
          <section className="card">
            <div className="stats-grid">
              <div className="stat-card">
                <strong>{items.length}</strong>
                <span>Open workflows</span>
              </div>
              <div className="stat-card">
                <strong>{overdueCount}</strong>
                <span>Overdue items</span>
              </div>
              <div className="stat-card">
                <strong>{typeOptions.length}</strong>
                <span>Workflow types</span>
              </div>
            </div>
          </section>

          <section className="card analytics-panel">
            <div className="workflow-filter-row">
              <label>
                <span>Workflow type</span>
                <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value as FilterType)}>
                  <option value="ALL">All</option>
                  {typeOptions.map((type) => (
                    <option key={type} value={type}>
                      {formatWorkflowType(type)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="workflow-toggle">
                <input type="checkbox" checked={onlyOverdue} onChange={(event) => setOnlyOverdue(event.target.checked)} />
                <span>Show only overdue items</span>
              </label>
            </div>

            {visibleItems.length === 0 ? (
              <p className="muted">No workflows match the current filters.</p>
            ) : (
              <div className="table-wrap">
                <table className="table analytics-table">
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Title</th>
                      <th>Subject</th>
                      <th>Status</th>
                      <th>Created</th>
                      <th>Due</th>
                      <th>Next states</th>
                      <th>Open</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleItems.map((item) => (
                      <tr key={`${item.type}-${item.entityId}`}>
                        <td>
                          <span className="badge badge-neutral">{formatWorkflowType(item.type)}</span>
                        </td>
                        <td>
                          <strong>{item.title}</strong>
                        </td>
                        <td>{item.subject}</td>
                        <td>
                          <span className={statusBadgeClass(item)}>
                            {item.overdue ? `Overdue | ${formatStatus(item.status)}` : formatStatus(item.status)}
                          </span>
                        </td>
                        <td>{formatDate(item.createdAt)}</td>
                        <td>{formatDate(item.dueAt)}</td>
                        <td>{item.nextStatuses.length > 0 ? item.nextStatuses.map(formatStatus).join(", ") : "Terminal"}</td>
                        <td>
                          <Link className="link-btn" to={item.link}>
                            Open
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}
