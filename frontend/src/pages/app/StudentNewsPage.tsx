import { useEffect, useState } from "react";
import { ApiError, fetchStudentNews } from "../../lib/api";
import type { StudentNewsItem } from "../../types/student";

export default function StudentNewsPage() {
  const [items, setItems] = useState<StudentNewsItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentNews();
        if (!cancelled) {
          setItems(payload);
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
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>News</h2>
      </header>
      <section className="card">
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loading && !error && items.length === 0 ? <p className="muted">No news.</p> : null}
        {!loading && !error && items.length > 0 ? (
          <div className="stack">
            {items.map((item) => (
              <article key={item.id} className="news-item">
                <div className="news-meta">
                  <strong>{item.title}</strong>
                  <span>{item.createdAt}</span>
                </div>
                {item.imageUrl ? <img className="news-image" src={item.imageUrl} alt={item.title} /> : null}
                <p>{item.content}</p>
                <span className="badge">{item.category || "General"}</span>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  );
}

