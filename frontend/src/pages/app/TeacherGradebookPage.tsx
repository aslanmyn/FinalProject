import { type FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  createTeacherComponent,
  fetchTeacherComponents,
  fetchTeacherRoster,
  fetchTeacherSections,
  publishTeacherFinalGrade,
  saveTeacherFinalGrade,
  saveTeacherGrade,
  setTeacherComponentLock,
  setTeacherComponentPublish
} from "../../lib/api";
import type { TeacherComponentItem, TeacherRosterItem, TeacherSectionItem } from "../../types/teacher";

const componentTypes = ["QUIZ", "MIDTERM", "FINAL", "LAB", "PROJECT", "OTHER"] as const;

export default function TeacherGradebookPage() {
  const [sections, setSections] = useState<TeacherSectionItem[]>([]);
  const [sectionId, setSectionId] = useState<number | "">("");
  const [roster, setRoster] = useState<TeacherRosterItem[]>([]);
  const [components, setComponents] = useState<TeacherComponentItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [componentName, setComponentName] = useState("");
  const [componentType, setComponentType] = useState<(typeof componentTypes)[number]>("QUIZ");
  const [componentWeight, setComponentWeight] = useState(10);

  const [gradeStudentId, setGradeStudentId] = useState<number | "">("");
  const [gradeComponentId, setGradeComponentId] = useState<number | "">("");
  const [gradeValue, setGradeValue] = useState(0);
  const [gradeMaxValue, setGradeMaxValue] = useState(100);
  const [gradeComment, setGradeComment] = useState("");

  const [finalStudentId, setFinalStudentId] = useState<number | "">("");
  const [finalNumericValue, setFinalNumericValue] = useState(0);
  const [finalLetterValue, setFinalLetterValue] = useState("A");
  const [finalPoints, setFinalPoints] = useState(4);

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
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    void loadSections();
    return () => {
      cancelled = true;
    };
  }, []);

  async function reloadSectionData(targetSectionId: number | "") {
    if (!targetSectionId) {
      setRoster([]);
      setComponents([]);
      return;
    }
    try {
      const [r, c] = await Promise.all([fetchTeacherRoster(Number(targetSectionId)), fetchTeacherComponents(Number(targetSectionId))]);
      setRoster(r);
      setComponents(c);
      setGradeStudentId(r[0]?.studentId ?? "");
      setFinalStudentId(r[0]?.studentId ?? "");
      setGradeComponentId(c[0]?.id ?? "");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load gradebook data");
    }
  }

  useEffect(() => {
    void reloadSectionData(sectionId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sectionId]);

  async function handleCreateComponent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId) return;
    setError(null);
    setSuccess(null);
    try {
      await createTeacherComponent(Number(sectionId), componentName.trim(), componentType, componentWeight);
      setComponentName("");
      setComponentWeight(10);
      setSuccess("Assessment component created");
      await reloadSectionData(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create component");
    }
  }

  async function togglePublish(component: TeacherComponentItem) {
    if (!sectionId) return;
    setError(null);
    setSuccess(null);
    try {
      await setTeacherComponentPublish(Number(sectionId), component.id, !component.published);
      setSuccess(`Component ${component.name} publish state updated`);
      await reloadSectionData(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update publish state");
    }
  }

  async function toggleLock(component: TeacherComponentItem) {
    if (!sectionId) return;
    setError(null);
    setSuccess(null);
    try {
      await setTeacherComponentLock(Number(sectionId), component.id, !component.locked);
      setSuccess(`Component ${component.name} lock state updated`);
      await reloadSectionData(sectionId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update lock state");
    }
  }

  async function handleSaveGrade(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId || !gradeStudentId || !gradeComponentId) return;
    setError(null);
    setSuccess(null);
    try {
      await saveTeacherGrade(
        Number(sectionId),
        Number(gradeStudentId),
        Number(gradeComponentId),
        Number(gradeValue),
        Number(gradeMaxValue),
        gradeComment
      );
      setSuccess("Grade saved");
      setGradeComment("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save grade");
    }
  }

  async function handleSaveFinalGrade(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionId || !finalStudentId) return;
    setError(null);
    setSuccess(null);
    try {
      await saveTeacherFinalGrade(
        Number(sectionId),
        Number(finalStudentId),
        Number(finalNumericValue),
        finalLetterValue,
        Number(finalPoints)
      );
      setSuccess("Final grade saved");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save final grade");
    }
  }

  async function handlePublishFinal() {
    if (!sectionId || !finalStudentId) return;
    setError(null);
    setSuccess(null);
    try {
      await publishTeacherFinalGrade(Number(sectionId), Number(finalStudentId));
      setSuccess("Final grade published");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to publish final grade");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Teacher Gradebook</h2>
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
            <h3>Assessment Components</h3>
            <form className="inline-form" onSubmit={handleCreateComponent}>
              <label>
                Name
                <input value={componentName} onChange={(event) => setComponentName(event.target.value)} required />
              </label>
              <label>
                Type
                <select
                  value={componentType}
                  onChange={(event) => setComponentType(event.target.value as (typeof componentTypes)[number])}
                >
                  {componentTypes.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Weight %
                <input
                  type="number"
                  min={0}
                  max={100}
                  step={0.1}
                  value={componentWeight}
                  onChange={(event) => setComponentWeight(Number(event.target.value))}
                />
              </label>
              <button type="submit" disabled={!sectionId}>
                Create Component
              </button>
            </form>

            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Weight</th>
                    <th>Status</th>
                    <th>Publish</th>
                    <th>Lock</th>
                  </tr>
                </thead>
                <tbody>
                  {components.map((component) => (
                    <tr key={component.id}>
                      <td>{component.id}</td>
                      <td>{component.name}</td>
                      <td>{component.type}</td>
                      <td>{component.weightPercent}</td>
                      <td>{component.status}</td>
                      <td>
                        <button type="button" onClick={() => togglePublish(component)}>
                          {component.published ? "Unpublish" : "Publish"}
                        </button>
                      </td>
                      <td>
                        <button type="button" onClick={() => toggleLock(component)}>
                          {component.locked ? "Unlock" : "Lock"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card">
            <h3>Save Component Grade</h3>
            <form className="inline-form" onSubmit={handleSaveGrade}>
              <label>
                Student
                <select
                  value={gradeStudentId}
                  onChange={(event) => setGradeStudentId(event.target.value ? Number(event.target.value) : "")}
                >
                  {roster.map((student) => (
                    <option key={student.registrationId} value={student.studentId}>
                      {student.studentName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Component
                <select
                  value={gradeComponentId}
                  onChange={(event) => setGradeComponentId(event.target.value ? Number(event.target.value) : "")}
                >
                  {components.map((component) => (
                    <option key={component.id} value={component.id}>
                      {component.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Grade
                <input type="number" step={0.01} value={gradeValue} onChange={(e) => setGradeValue(Number(e.target.value))} />
              </label>
              <label>
                Max
                <input type="number" step={0.01} value={gradeMaxValue} onChange={(e) => setGradeMaxValue(Number(e.target.value))} />
              </label>
              <label>
                Comment
                <input value={gradeComment} onChange={(e) => setGradeComment(e.target.value)} />
              </label>
              <button type="submit" disabled={!sectionId || !gradeStudentId || !gradeComponentId}>
                Save Grade
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Save / Publish Final Grade</h3>
            <form className="inline-form" onSubmit={handleSaveFinalGrade}>
              <label>
                Student
                <select
                  value={finalStudentId}
                  onChange={(event) => setFinalStudentId(event.target.value ? Number(event.target.value) : "")}
                >
                  {roster.map((student) => (
                    <option key={student.registrationId} value={student.studentId}>
                      {student.studentName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Numeric
                <input
                  type="number"
                  step={0.01}
                  value={finalNumericValue}
                  onChange={(e) => setFinalNumericValue(Number(e.target.value))}
                />
              </label>
              <label>
                Letter
                <input value={finalLetterValue} onChange={(e) => setFinalLetterValue(e.target.value)} />
              </label>
              <label>
                Points
                <input type="number" step={0.01} value={finalPoints} onChange={(e) => setFinalPoints(Number(e.target.value))} />
              </label>
              <button type="submit" disabled={!sectionId || !finalStudentId}>
                Save Final
              </button>
              <button type="button" disabled={!sectionId || !finalStudentId} onClick={handlePublishFinal}>
                Publish Final
              </button>
            </form>
          </section>
        </>
      ) : null}
    </div>
  );
}

