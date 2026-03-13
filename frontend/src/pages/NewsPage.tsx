import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, fetchPublicNews } from "../lib/api";
import type { PublicNewsItem } from "../types/public";

export default function NewsPage() {
  const [items, setItems] = useState<PublicNewsItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchPublicNews();
        if (!cancelled) {
          setItems(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load news");
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
        <h2>Public News</h2>
        <Link to="/">Back</Link>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error
        ? items.map((item) => (
            <section key={item.id} className="card">
              <h3>{item.title}</h3>
              <p className="muted">
                {item.category || "General"} | {new Date(item.createdAt).toLocaleString()}
              </p>
              <p>{item.content}</p>
            </section>
          ))
        : null}
    </div>
  );
}

