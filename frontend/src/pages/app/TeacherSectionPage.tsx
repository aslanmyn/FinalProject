import { type FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ApiError,
  buildFileDownloadUrl,
  downloadTeacherSectionGrades,
  fetchTeacherComponents,
  fetchTeacherRoster,
  uploadTeacherStudentFile
} from "../../lib/api";
import type { TeacherComponentItem, TeacherRosterItem, TeacherStudentFileItem } from "../../types/teacher";

export default function TeacherSectionPage() {
  const { sectionId } = useParams();
  const [roster, setRoster] = useState<TeacherRosterItem[]>([]);
  const [components, setComponents] = useState<TeacherComponentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedStudentId, setSelectedStudentId] = useState<number | "">("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [lastUploadedFile, setLastUploadedFile] = useState<TeacherStudentFileItem | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (!sectionId) {
        setError("Invalid section id");
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const sid = Number(sectionId);
        const [r, c] = await Promise.all([fetchTeacherRoster(sid), fetchTeacherComponents(sid)]);
        if (!cancelled) {
          setRoster(r);
          setComponents(c);
          if (r.length > 0) {
            setSelectedStudentId(r[0].studentId);
          } else {
            setSelectedStudentId("");
          }
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load section");
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
  }, [sectionId]);

  async function handleExport() {
    if (!sectionId) return;
    setExporting(true);
    setError(null);
    try {
      const { blob, fileName } = await downloadTeacherSectionGrades(Number(sectionId));
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Export failed");
    } finally {
      setExporting(false);
    }
  }

  async function handleUploadStudentFile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId) return;
    if (!selectedStudentId || !selectedFile) {
      setError("Select student and file before upload");
      return;
    }

    setUploading(true);
    setError(null);
    try {
      const uploaded = await uploadTeacherStudentFile(Number(sectionId), Number(selectedStudentId), selectedFile);
      setLastUploadedFile(uploaded);
      setSelectedFile(null);
      const fileInput = document.getElementById("teacher-student-file-input") as HTMLInputElement | null;
      if (fileInput) {
        fileInput.value = "";
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to upload file");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Section #{sectionId}</h2>
        <div className="actions">
          <button onClick={handleExport} disabled={exporting}>
            {exporting ? "Exporting..." : "Export grades (XLSX)"}
          </button>
          <Link to="/app/teacher">Back</Link>
        </div>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error ? (
        <>
          <section className="card">
            <h3>Roster</h3>
            {roster.length === 0 ? <p className="muted">No students in roster.</p> : null}
            {roster.map((item) => (
              <div key={item.registrationId} className="row">
                <strong>{item.studentName}</strong> ({item.studentEmail}) | {item.status}
              </div>
            ))}
          </section>

          <section className="card">
            <h3>Assessment Components</h3>
            {components.length === 0 ? <p className="muted">No components.</p> : null}
            {components.map((item) => (
              <div key={item.id} className="row">
                <strong>{item.name}</strong> [{item.type}] | weight: {item.weightPercent}% | status: {item.status}
              </div>
            ))}
          </section>

          <section className="card">
            <h3>Upload File To Student</h3>
            {roster.length === 0 ? <p className="muted">No students in this section.</p> : null}
            {roster.length > 0 ? (
              <form className="inline-form" onSubmit={handleUploadStudentFile}>
                <label>
                  Student
                  <select
                    value={selectedStudentId}
                    onChange={(event) => setSelectedStudentId(Number(event.target.value))}
                  >
                    {roster.map((item) => (
                      <option key={item.registrationId} value={item.studentId}>
                        {item.studentName} ({item.studentEmail})
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  File
                  <input
                    id="teacher-student-file-input"
                    type="file"
                    onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
                  />
                </label>
                <button type="submit" disabled={uploading}>
                  {uploading ? "Uploading..." : "Upload"}
                </button>
              </form>
            ) : null}

            {lastUploadedFile ? (
              <p className="row">
                Uploaded:{" "}
                <a href={buildFileDownloadUrl(lastUploadedFile.downloadUrl)} target="_blank" rel="noreferrer">
                  {lastUploadedFile.fileName}
                </a>
              </p>
            ) : null}
          </section>
        </>
      ) : null}
    </div>
  );
}
