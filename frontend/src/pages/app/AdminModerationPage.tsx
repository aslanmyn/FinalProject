import { type FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  createAdminNews,
  fetchAdminGradeChangeRequests,
  fetchAdminStudents,
  reviewAdminGradeChangeRequest,
  updateAdminStudentStatus
} from "../../lib/api";
import type { AdminGradeChangeItem, AdminSimpleStudentItem } from "../../types/admin";

const studentStatuses = ["ACTIVE", "ON_LEAVE", "GRADUATED"] as const;

export default function AdminModerationPage() {
  const [students, setStudents] = useState<AdminSimpleStudentItem[]>([]);
  const [gradeChanges, setGradeChanges] = useState<AdminGradeChangeItem[]>([]);
  const [studentId, setStudentId] = useState<number | "">("");
  const [studentStatus, setStudentStatus] = useState<(typeof studentStatuses)[number]>("ACTIVE");
  const [newsTitle, setNewsTitle] = useState("");
  const [newsContent, setNewsContent] = useState("");
  const [newsCategory, setNewsCategory] = useState("GENERAL");
  const [reviewComment, setReviewComment] = useState("Reviewed by registrar");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [studentsData, gradeChangesPage] = await Promise.all([fetchAdminStudents(), fetchAdminGradeChangeRequests(0, 100)]);
      setStudents(studentsData);
      setGradeChanges(gradeChangesPage.items);
      const firstStudentId = studentsData[0]?.id ?? "";
      setStudentId(firstStudentId);
      const firstStudent = studentsData.find((item) => item.id === firstStudentId);
      if (firstStudent && (studentStatuses as readonly string[]).includes(firstStudent.status)) {
        setStudentStatus(firstStudent.status as (typeof studentStatuses)[number]);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load moderation data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  async function handleUpdateStudentStatus(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!studentId) return;
    setError(null);
    setSuccess(null);
    try {
      await updateAdminStudentStatus(Number(studentId), studentStatus);
      setSuccess("Student status updated");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update student status");
    }
  }

  async function handleCreateNews(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      await createAdminNews(newsTitle.trim(), newsContent.trim(), newsCategory.trim());
      setSuccess("News post created");
      setNewsTitle("");
      setNewsContent("");
      setNewsCategory("GENERAL");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create news");
    }
  }

  async function reviewGradeChange(requestId: number, approve: boolean) {
    setError(null);
    setSuccess(null);
    try {
      await reviewAdminGradeChangeRequest(requestId, approve, reviewComment);
      setSuccess(`Grade change request ${approve ? "approved" : "rejected"}`);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to review grade change");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Admin Moderation & Content</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <>
          <section className="card">
            <h3>Update Student Status</h3>
            <form className="inline-form" onSubmit={handleUpdateStudentStatus}>
              <label>
                Student
                <select
                  value={studentId}
                  onChange={(event) => {
                    const nextStudentId = event.target.value ? Number(event.target.value) : "";
                    setStudentId(nextStudentId);
                    const selected = students.find((item) => item.id === nextStudentId);
                    if (selected && (studentStatuses as readonly string[]).includes(selected.status)) {
                      setStudentStatus(selected.status as (typeof studentStatuses)[number]);
                    }
                  }}
                >
                  {students.map((student) => (
                    <option key={student.id} value={student.id}>
                      {student.id} - {student.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Status
                <select value={studentStatus} onChange={(event) => setStudentStatus(event.target.value as (typeof studentStatuses)[number])}>
                  {studentStatuses.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </label>
              <button type="submit" disabled={!studentId}>
                Update Status
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Grade Change Review</h3>
            <label>
              Review Comment
              <input value={reviewComment} onChange={(event) => setReviewComment(event.target.value)} />
            </label>
            {gradeChanges.length === 0 ? <p className="muted">No pending requests.</p> : null}
            {gradeChanges.length > 0 ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Teacher</th>
                      <th>Student</th>
                      <th>Section</th>
                      <th>Old</th>
                      <th>New</th>
                      <th>Reason</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {gradeChanges.map((item) => (
                      <tr key={item.id}>
                        <td>{item.id}</td>
                        <td>{item.teacherId}</td>
                        <td>{item.studentId}</td>
                        <td>{item.sectionId}</td>
                        <td>{item.oldValue ?? "-"}</td>
                        <td>{item.newValue ?? "-"}</td>
                        <td>{item.reason}</td>
                        <td>{item.status}</td>
                        <td className="actions">
                          <button type="button" onClick={() => reviewGradeChange(item.id, true)}>
                            Approve
                          </button>
                          <button type="button" onClick={() => reviewGradeChange(item.id, false)}>
                            Reject
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </section>

          <section className="card">
            <h3>Create News</h3>
            <form className="form" onSubmit={handleCreateNews}>
              <label>
                Title
                <input value={newsTitle} onChange={(event) => setNewsTitle(event.target.value)} required />
              </label>
              <label>
                Content
                <textarea value={newsContent} onChange={(event) => setNewsContent(event.target.value)} rows={4} required />
              </label>
              <label>
                Category
                <input value={newsCategory} onChange={(event) => setNewsCategory(event.target.value)} />
              </label>
              <button type="submit">Create News</button>
            </form>
          </section>
        </>
      ) : null}
    </div>
  );
}

