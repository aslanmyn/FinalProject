import { useEffect, useState } from "react";
import { ApiError, buildFileDownloadUrl, fetchStudentFiles } from "../../lib/api";
import type { StudentFileItem } from "../../types/student";

export default function StudentFilesPage() {
  const [items, setItems] = useState<StudentFileItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentFiles();
        if (!cancelled) {
          setItems(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load files");
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
        <h2>Student Files</h2>
      </header>

      <section className="card">
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}

        {!loading && !error && items.length === 0 ? <p className="muted">No files.</p> : null}

        {!loading && !error && items.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>File Name</th>
                  <th>Category</th>
                  <th>Content Type</th>
                  <th>Size (bytes)</th>
                  <th>Uploaded</th>
                  <th>Download</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.id}</td>
                    <td>{item.fileName}</td>
                    <td>{item.category}</td>
                    <td>{item.contentType}</td>
                    <td>{item.sizeBytes}</td>
                    <td>{item.uploadedAt}</td>
                    <td>
                      <a href={buildFileDownloadUrl(item.downloadUrl)} target="_blank" rel="noreferrer">
                        Open
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </div>
  );
}

