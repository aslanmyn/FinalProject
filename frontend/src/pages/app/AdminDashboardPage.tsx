import { useEffect, useState } from "react";
import { ApiError, fetchAdminStats } from "../../lib/api";
import type { AdminStats } from "../../types/admin";

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchAdminStats();
        if (!cancelled) {
          setStats(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load admin stats");
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

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Admin Overview</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error && stats ? (
        <section className="card">
          <div className="stats-grid">
            <div className="stat-card">
              <strong>{stats.students}</strong>
              <span>Students</span>
            </div>
            <div className="stat-card">
              <strong>{stats.teachers}</strong>
              <span>Teachers</span>
            </div>
            <div className="stat-card">
              <strong>{stats.sections}</strong>
              <span>Sections</span>
            </div>
            <div className="stat-card">
              <strong>{stats.requests}</strong>
              <span>Requests</span>
            </div>
            <div className="stat-card">
              <strong>{stats.activeHolds}</strong>
              <span>Active Holds</span>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  );
}

