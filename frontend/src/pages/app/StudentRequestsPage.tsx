import { FormEvent, useEffect, useState } from "react";
import { ApiError, createStudentRequest, fetchStudentRequests } from "../../lib/api";
import type { StudentRequestItem } from "../../types/student";

export default function StudentRequestsPage() {
  const [items, setItems] = useState<StudentRequestItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [category, setCategory] = useState("GENERAL");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchStudentRequests(0, 50);
      setItems(page.items);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load requests");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createStudentRequest(category, description.trim());
      setDescription("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create request");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Student Requests</h2>
      </header>

      <section className="card">
        <h3>Create request</h3>
        <form onSubmit={handleSubmit} className="form">
          <label>
            Category
            <input value={category} onChange={(e) => setCategory(e.target.value)} required />
          </label>
          <label>
            Description
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              minLength={3}
            />
          </label>
          <button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Submit request"}
          </button>
        </form>
      </section>

      <section className="card">
        <h3>My requests</h3>
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {!loading &&
          !error &&
          items.map((item) => (
            <div key={item.id} className="row">
              <strong>#{item.id}</strong> [{item.status}] {item.category}
              <p>{item.description}</p>
            </div>
          ))}
      </section>
    </div>
  );
}

