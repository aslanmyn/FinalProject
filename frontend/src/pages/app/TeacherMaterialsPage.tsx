import { type FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  buildFileDownloadUrl,
  deleteTeacherMaterial,
  fetchTeacherMaterials,
  fetchTeacherSections,
  setTeacherMaterialVisibility,
  uploadTeacherMaterial
} from "../../lib/api";
import type { TeacherMaterialItem, TeacherSectionItem } from "../../types/teacher";

export default function TeacherMaterialsPage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [sectionId, setSectionId] = useState<number | "">("");
  const [items, setItems] = useState<TeacherMaterialItem[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [visibility, setVisibility] = useState<"ENROLLED_ONLY" | "PUBLIC">("ENROLLED_ONLY");
  const [file, setFile] = useState<File | null>(null);
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

  async function loadMaterials(targetSectionId: number | "") {
    if (!targetSectionId) {
      setItems([]);
      return;
    }
    try {
      const data = await fetchTeacherMaterials(Number(targetSectionId));
      setItems(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load materials");
    }
  }

  useEffect(() => {
    let cancelled = false;

    async function refreshMaterials() {
      if (!sectionId) {
        setItems([]);
        return;
      }
      try {
        const data = await fetchTeacherMaterials(Number(sectionId));
        if (!cancelled) {
          setItems(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load materials");
        }
      }
    }

    void refreshMaterials();
    return () => {
      cancelled = true;
    };
  }, [sectionId]);

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId || !file) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await uploadTeacherMaterial(Number(sectionId), title.trim(), description.trim(), visibility, file);
      setTitle("");
      setDescription("");
      setVisibility("ENROLLED_ONLY");
      setFile(null);
      const input = document.getElementById("teacher-material-file") as HTMLInputElement | null;
      if (input) input.value = "";
      setSuccess("Material uploaded");
      await loadMaterials(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to upload material");
    } finally {
      setSaving(false);
    }
  }

  async function toggleVisibility(item: TeacherMaterialItem) {
    setError(null);
    setSuccess(null);
    try {
      await setTeacherMaterialVisibility(item.id, !item.published);
      setSuccess(`Material ${item.originalFileName} updated`);
      await loadMaterials(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update material visibility");
    }
  }

  async function handleDelete(item: TeacherMaterialItem) {
    setError(null);
    setSuccess(null);
    try {
      await deleteTeacherMaterial(item.id);
      setSuccess(`Material ${item.originalFileName} deleted`);
      await loadMaterials(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete material");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Materials</h2>
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
            <h3>Upload Material</h3>
            <form className="inline-form" onSubmit={handleUpload}>
              <label>
                Title
                <input value={title} onChange={(event) => setTitle(event.target.value)} required />
              </label>
              <label>
                Description
                <input value={description} onChange={(event) => setDescription(event.target.value)} />
              </label>
              <label>
                Visibility
                <select value={visibility} onChange={(event) => setVisibility(event.target.value as "ENROLLED_ONLY" | "PUBLIC")}>
                  <option value="ENROLLED_ONLY">ENROLLED_ONLY</option>
                  <option value="PUBLIC">PUBLIC</option>
                </select>
              </label>
              <label>
                File
                <input id="teacher-material-file" type="file" onChange={(event) => setFile(event.target.files?.[0] || null)} required />
              </label>
              <button type="submit" disabled={saving || !sectionId}>
                {saving ? "Uploading..." : "Upload"}
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Section Materials</h3>
            {items.length === 0 ? <p className="muted">No materials uploaded.</p> : null}
            {items.length > 0 ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Title</th>
                      <th>File</th>
                      <th>Visibility</th>
                      <th>Published</th>
                      <th>Download</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr key={item.id}>
                        <td>{item.id}</td>
                        <td>{item.title}</td>
                        <td>{item.originalFileName}</td>
                        <td>{item.visibility}</td>
                        <td>{item.published ? "Yes" : "No"}</td>
                        <td>
                          <a href={buildFileDownloadUrl(item.downloadUrl)} target="_blank" rel="noreferrer">
                            Open
                          </a>
                        </td>
                        <td className="actions">
                          <button type="button" onClick={() => toggleVisibility(item)}>
                            {item.published ? "Hide" : "Publish"}
                          </button>
                          <button type="button" onClick={() => handleDelete(item)}>
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </section>
        </>
      ) : null}
    </div>
  );
}
