import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchPublicProfessors } from "../lib/api";
import type { PublicProfessorListItem } from "../types/public";

export default function ProfessorsPage() {
  const [items, setItems] = useState<PublicProfessorListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchPublicProfessors();
        if (!cancelled) {
          setItems(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load professors");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Professors</h2>
        <Link to="/">Back</Link>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error ? (
        <div className="grid">
          {items.map((item) => (
            <article key={item.id} className="card">
              <h3>{item.name}</h3>
              <p className="muted">{item.positionTitle || item.role}</p>
              <p>{item.department || "-"}</p>
              <p className="muted">{item.publicEmail || "-"}</p>
              <Link to={`/professors/${item.id}`}>Open profile</Link>
            </article>
          ))}
        </div>
      ) : null}
    </div>
  );
}

