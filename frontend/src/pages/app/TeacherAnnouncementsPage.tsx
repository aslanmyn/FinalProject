import { type FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  createTeacherAnnouncement,
  fetchTeacherAnnouncements,
  fetchTeacherSections
} from "../../lib/api";
import type { TeacherAnnouncementItem, TeacherSectionItem } from "../../types/teacher";

export default function TeacherAnnouncementsPage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [sectionId, setSectionId] = useState<number | "">("");
  const [items, setItems] = useState<TeacherAnnouncementItem[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [publicVisible, setPublicVisible] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function loadSections() {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchTeacherSections();
        if (!cancelled) {
          setSections(data);
          setSectionId(data[0]?.id ?? "");
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load sections");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void loadSections();
    return () => {
      cancelled = true;
    };
  }, []);

  async function loadAnnouncements(targetSectionId: number | "") {
    if (!targetSectionId) {
      setItems([]);
      return;
    }
    try {
      const data = await fetchTeacherAnnouncements(Number(targetSectionId));
      setItems(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load announcements");
    }
  }

  useEffect(() => {
    void loadAnnouncements(sectionId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sectionId]);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await createTeacherAnnouncement(Number(sectionId), title.trim(), content.trim(), publicVisible, pinned);
      setTitle("");
      setContent("");
      setPinned(false);
      setPublicVisible(false);
      setSuccess("Announcement created");
      await loadAnnouncements(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create announcement");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Announcements</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <>
          <section className="card">
            <label>
              Section
              <select
                value={sectionId}
                onChange={(event) => setSectionId(event.target.value ? Number(event.target.value) : "")}
              >
                {sections.map((section) => (
                  <option key={section.id} value={section.id}>
                    {section.subjectCode} - {section.subjectName}
                  </option>
                ))}
              </select>
            </label>
          </section>

          <section className="card">
            <h3>Create Announcement</h3>
            <form className="form" onSubmit={handleCreate}>
              <label>
                Title
                <input value={title} onChange={(event) => setTitle(event.target.value)} required />
              </label>
              <label>
                Content
                <textarea value={content} onChange={(event) => setContent(event.target.value)} rows={4} required />
              </label>
              <label>
                <input type="checkbox" checked={publicVisible} onChange={(e) => setPublicVisible(e.target.checked)} /> Public visible
              </label>
              <label>
                <input type="checkbox" checked={pinned} onChange={(e) => setPinned(e.target.checked)} /> Pinned
              </label>
              <button type="submit" disabled={saving || !sectionId}>
                {saving ? "Saving..." : "Create Announcement"}
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Section Announcements</h3>
            {items.length === 0 ? <p className="muted">No announcements yet.</p> : null}
            <div className="stack">
              {items.map((item) => (
                <article key={item.id} className="news-item">
                  <div className="news-meta">
                    <strong>{item.title}</strong>
                    <span>{item.publishedAt || ""}</span>
                  </div>
                  <p>{item.content}</p>
                  {item.pinned ? <span className="badge">Pinned</span> : null}
                </article>
              ))}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}

