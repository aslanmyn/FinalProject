import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { NotificationCenterData } from "../types/common";
import { ApiError } from "../lib/api";

interface NotificationCenterProps {
  title: string;
  subtitle: string;
  loadData: () => Promise<NotificationCenterData>;
  markRead: (id: number) => Promise<void>;
  markAllRead: () => Promise<void>;
  emptyTitle: string;
  emptyText: string;
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString();
}

function isInternalLink(link: string | null): link is string {
  return Boolean(link && link.startsWith("/"));
}

export default function NotificationCenter({
  title,
  subtitle,
  loadData,
  markRead,
  markAllRead,
  emptyTitle,
  emptyText
}: NotificationCenterProps) {
  const [data, setData] = useState<NotificationCenterData | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [markingAll, setMarkingAll] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function reload() {
    setLoading(true);
    setError(null);
    try {
      setData(await loadData());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load notifications");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void reload();
  }, []);

  async function handleMarkRead(id: number) {
    setBusyId(id);
    setError(null);
    try {
      await markRead(id);
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update notification");
    } finally {
      setBusyId(null);
    }
  }

  async function handleMarkAll() {
    setMarkingAll(true);
    setError(null);
    try {
      await markAllRead();
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update notifications");
    } finally {
      setMarkingAll(false);
    }
  }

  const total = data?.notifications.length ?? 0;
  const unread = data?.unreadCount ?? 0;

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <div>
          <h2>{title}</h2>
          <p className="muted">{subtitle}</p>
        </div>
        <div className="actions">
          <button type="button" onClick={handleMarkAll} disabled={markingAll || unread === 0}>
            {markingAll ? "Updating..." : "Mark all read"}
          </button>
        </div>
      </header>

      {error ? (
        <section className="card">
          <p className="error">{error}</p>
        </section>
      ) : null}

      <section className="card">
        <div className="stats-grid">
          <div className="stat-card">
            <strong>{total}</strong>
            <span>Total notifications</span>
          </div>
          <div className="stat-card">
            <strong>{unread}</strong>
            <span>Unread</span>
          </div>
        </div>
      </section>

      <section className="card">
        {loading ? <p>Loading notifications...</p> : null}
        {!loading && total === 0 ? (
          <div className="notification-empty">
            <h3>{emptyTitle}</h3>
            <p className="muted">{emptyText}</p>
          </div>
        ) : null}

        {!loading && total > 0 ? (
          <div className="notification-list">
            {data?.notifications.map((item) => (
              <article
                key={item.id}
                className={`notification-card ${item.read ? "notification-card-read" : "notification-card-unread"}`}
              >
                <div className="notification-card-head">
                  <div>
                    <span className="badge">{item.type.replace(/_/g, " ")}</span>
                    <h3>{item.title}</h3>
                  </div>
                  <span className="muted">{formatDateTime(item.createdAt)}</span>
                </div>
                <p className="notification-card-message">{item.message}</p>
                <div className="actions">
                  {!item.read ? (
                    <button
                      type="button"
                      onClick={() => handleMarkRead(item.id)}
                      disabled={busyId === item.id}
                    >
                      {busyId === item.id ? "Saving..." : "Mark read"}
                    </button>
                  ) : null}
                  {isInternalLink(item.link) ? (
                    <Link className="link-btn" to={item.link}>
                      Open link
                    </Link>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  );
}
