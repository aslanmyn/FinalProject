import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchAdminWorkflowTimeline, fetchAdminWorkflows } from "../../lib/api";
import type { WorkflowItem, WorkflowOverview, WorkflowTimeline, WorkflowType } from "../../types/common";

type FilterType = "ALL" | WorkflowType;

function formatWorkflowType(value: WorkflowType): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatStatus(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatDateTime(value: string | null): string {
  if (!value) return "-";
  return value.slice(0, 19).replace("T", " ");
}

function workflowBadgeClass(item: WorkflowItem): string {
  if (item.overdue) return "badge badge-danger";
  if (item.status === "NEED_INFO" || item.status === "REJECTED") return "badge badge-warning";
  return "badge badge-neutral";
}

export default function AdminWorkflowsPage() {
  const [overview, setOverview] = useState<WorkflowOverview | null>(null);
  const [selected, setSelected] = useState<WorkflowItem | null>(null);
  const [timeline, setTimeline] = useState<WorkflowTimeline | null>(null);
  const [loading, setLoading] = useState(true);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState<FilterType>("ALL");
  const [overdueOnly, setOverdueOnly] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchAdminWorkflows();
        if (!cancelled) {
          setOverview(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load workflow queue");
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

  const items = overview?.items ?? [];
  const visibleItems = useMemo(
    () =>
      items.filter((item) => {
        if (typeFilter !== "ALL" && item.type !== typeFilter) {
          return false;
        }
        if (overdueOnly && !item.overdue) {
          return false;
        }
        return true;
      }),
    [items, overdueOnly, typeFilter]
  );
  const typeOptions = useMemo(() => Array.from(new Set(items.map((item) => item.type))), [items]);
  const overdueCount = useMemo(() => items.filter((item) => item.overdue).length, [items]);

  useEffect(() => {
    if (visibleItems.length === 0) {
      setSelected(null);
      return;
    }
    if (!selected || !visibleItems.some((item) => item.type === selected.type && item.entityId === selected.entityId)) {
      setSelected(visibleItems[0]);
    }
  }, [selected, visibleItems]);

  useEffect(() => {
    if (!selected) {
      setTimeline(null);
      return;
    }

    const workflowItem = selected;

    let cancelled = false;

    async function loadTimeline() {
      setTimelineLoading(true);
      try {
        const payload = await fetchAdminWorkflowTimeline(workflowItem.type, workflowItem.entityId);
        if (!cancelled) {
          setTimeline(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load workflow timeline");
        }
      } finally {
        if (!cancelled) {
          setTimelineLoading(false);
        }
      }
    }

    void loadTimeline();
    return () => {
      cancelled = true;
    };
  }, [selected]);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>Workflow Engine</h2>
          <p className="muted">Track queue state, deadlines, next transitions, and audit timeline for every workflow.</p>
        </div>
        <div className="actions">
          <Link className="link-btn" to="/app/admin/analytics">
            Back to analytics
          </Link>
          <Link className="link-btn" to="/app/admin/assistant">
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

      {!loading ? (
        <>
          <section className="card analytics-hero-card analytics-hero-card-admin">
            <div className="analytics-hero analytics-hero-split">
              <div className="analytics-hero-main">
                <span className="assistant-eyebrow">State machine control</span>
                <h3>Operational queue and audit trail</h3>
                <p className="muted">
                  Watch deadlines, inspect allowed next states, and trace every workflow through audit history.
                </p>
                <div className="analytics-pill-group">
                  <span className="badge badge-warning">{overdueCount} overdue</span>
                  <span className="badge badge-neutral">{typeOptions.length} workflow types</span>
                  <span className="badge badge-neutral">{selected ? `${formatWorkflowType(selected.type)} selected` : "Select an item"}</span>
                </div>
              </div>
              <div className="analytics-hero-side-grid">
                <div className="stats-grid">
                  <div className="stat-card">
                    <strong>{items.length}</strong>
                    <span>Active items</span>
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
                <div className="analytics-spotlight-card">
                  <span className="assistant-summary-label">Audit spotlight</span>
                  <strong>{selected ? selected.title : "No item selected"}</strong>
                  <p className="muted">
                    {selected
                      ? `${selected.subject} is currently in ${formatStatus(selected.status)} with ${selected.nextStatuses.length || 0} possible next states.`
                      : "Select an item from the queue to inspect its timeline."}
                  </p>
                </div>
              </div>
            </div>
          </section>

          <section className="workflow-filter-card card">
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
                <input type="checkbox" checked={overdueOnly} onChange={(event) => setOverdueOnly(event.target.checked)} />
                <span>Only overdue</span>
              </label>
            </div>
          </section>

          <div className="workflow-layout">
            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Queue</h3>
                  <p className="muted">Select a workflow item to inspect its audit timeline.</p>
                </div>
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
                        <th>Status</th>
                        <th>Due</th>
                        <th>Next</th>
                      </tr>
                    </thead>
                    <tbody>
                      {visibleItems.map((item) => {
                        const isSelected = selected?.type === item.type && selected.entityId === item.entityId;
                        return (
                          <tr
                            key={`${item.type}-${item.entityId}`}
                            className={`workflow-row${isSelected ? " workflow-row-selected" : ""}`}
                            onClick={() => setSelected(item)}
                          >
                            <td>
                              <span className="badge badge-neutral">{formatWorkflowType(item.type)}</span>
                            </td>
                            <td>
                              <strong>{item.title}</strong>
                              <div className="muted">{item.subject}</div>
                            </td>
                            <td>
                              <span className={workflowBadgeClass(item)}>
                                {item.overdue ? `Overdue | ${formatStatus(item.status)}` : formatStatus(item.status)}
                              </span>
                            </td>
                            <td>{formatDateTime(item.dueAt)}</td>
                            <td>{item.nextStatuses.length > 0 ? item.nextStatuses.map(formatStatus).join(", ") : "Terminal"}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section className="card analytics-panel">
              <div className="section-heading">
                <div>
                  <h3>Audit timeline</h3>
                  <p className="muted">Every transition is taken from audit log events.</p>
                </div>
                {selected ? (
                  <Link className="link-btn" to={selected.link}>
                    Open source page
                  </Link>
                ) : null}
              </div>

              {selected ? (
                <div className="workflow-selected-meta">
                  <span className="badge badge-neutral">{formatWorkflowType(selected.type)}</span>
                  <strong>{selected.title}</strong>
                  <span>{selected.subject}</span>
                  <span className={workflowBadgeClass(selected)}>
                    {selected.overdue ? `Overdue | ${formatStatus(selected.status)}` : formatStatus(selected.status)}
                  </span>
                </div>
              ) : null}

              {timelineLoading ? (
                <p>Loading timeline...</p>
              ) : timeline && timeline.items.length > 0 ? (
                <div className="workflow-timeline">
                  {timeline.items.map((item, index) => (
                    <article key={`${item.createdAt}-${index}`} className="workflow-timeline-item">
                      <div className="workflow-timeline-dot" />
                      <div className="workflow-timeline-content">
                        <strong>{item.action}</strong>
                        <span className="muted">{formatDateTime(item.createdAt)}</span>
                        <p>{item.details || "No additional details recorded."}</p>
                        <span className="muted">{item.actorEmail || "System"}</span>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="muted">No audit entries found for the selected workflow item yet.</p>
              )}
            </section>
          </div>
        </>
      ) : null}
    </div>
  );
}
