import { type FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  addAdminMeetingTime,
  assignAdminProfessor,
  createAdminExam,
  createAdminSection,
  createAdminTerm,
  deleteAdminExam,
  fetchAdminExams,
  fetchAdminSections,
  fetchAdminSubjects,
  fetchAdminTeachers,
  fetchAdminTerms,
  upsertAdminWindow
} from "../../lib/api";
import type {
  AdminExamItem,
  AdminSectionItem,
  AdminSimpleSubjectItem,
  AdminSimpleTeacherItem,
  AdminTermItem
} from "../../types/admin";

const lessonTypes = ["LECTURE", "PRACTICE", "LAB"] as const;
const windowTypes = ["REGISTRATION", "ADD_DROP", "FX", "GRADE_PUBLISH"] as const;
const weekDays = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"] as const;

export default function AdminAcademicPage() {
  const [terms, setTerms] = useState<AdminTermItem[]>([]);
  const [subjects, setSubjects] = useState<AdminSimpleSubjectItem[]>([]);
  const [teachers, setTeachers] = useState<AdminSimpleTeacherItem[]>([]);
  const [sections, setSections] = useState<AdminSectionItem[]>([]);
  const [exams, setExams] = useState<AdminExamItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [termName, setTermName] = useState("");
  const [termStartDate, setTermStartDate] = useState("");
  const [termEndDate, setTermEndDate] = useState("");
  const [termCurrent, setTermCurrent] = useState(false);

  const [sectionSubjectId, setSectionSubjectId] = useState<number | "">("");
  const [sectionSemesterId, setSectionSemesterId] = useState<number | "">("");
  const [sectionTeacherId, setSectionTeacherId] = useState<number | "">("");
  const [sectionCapacity, setSectionCapacity] = useState(30);
  const [sectionLessonType, setSectionLessonType] = useState<(typeof lessonTypes)[number]>("LECTURE");

  const [assignSectionId, setAssignSectionId] = useState<number | "">("");
  const [assignTeacherId, setAssignTeacherId] = useState<number | "">("");

  const [meetingSectionId, setMeetingSectionId] = useState<number | "">("");
  const [meetingDay, setMeetingDay] = useState<(typeof weekDays)[number]>("MONDAY");
  const [meetingStartTime, setMeetingStartTime] = useState("09:00");
  const [meetingEndTime, setMeetingEndTime] = useState("10:00");
  const [meetingRoom, setMeetingRoom] = useState("A-101");
  const [meetingLessonType, setMeetingLessonType] = useState<(typeof lessonTypes)[number]>("LECTURE");

  const [windowSemesterId, setWindowSemesterId] = useState<number | "">("");
  const [windowType, setWindowType] = useState<(typeof windowTypes)[number]>("REGISTRATION");
  const [windowStartDate, setWindowStartDate] = useState("");
  const [windowEndDate, setWindowEndDate] = useState("");
  const [windowActive, setWindowActive] = useState(true);

  const [examSectionId, setExamSectionId] = useState<number | "">("");
  const [examDate, setExamDate] = useState("");
  const [examTime, setExamTime] = useState("09:00");
  const [examRoom, setExamRoom] = useState("Main Hall");
  const [examFormat, setExamFormat] = useState("WRITTEN");

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [termsData, subjectsData, teachersData, sectionsData, examsData] = await Promise.all([
        fetchAdminTerms(),
        fetchAdminSubjects(),
        fetchAdminTeachers(),
        fetchAdminSections(),
        fetchAdminExams()
      ]);
      setTerms(termsData);
      setSubjects(subjectsData);
      setTeachers(teachersData);
      setSections(sectionsData);
      setExams(examsData);

      setSectionSubjectId(subjectsData[0]?.id ?? "");
      setSectionSemesterId(termsData[0]?.id ?? "");
      setSectionTeacherId(teachersData[0]?.id ?? "");
      setAssignSectionId(sectionsData[0]?.id ?? "");
      setAssignTeacherId(teachersData[0]?.id ?? "");
      setMeetingSectionId(sectionsData[0]?.id ?? "");
      setWindowSemesterId(termsData[0]?.id ?? "");
      setExamSectionId(sectionsData[0]?.id ?? "");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load academic data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  async function handleCreateTerm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      await createAdminTerm(termName.trim(), termStartDate, termEndDate, termCurrent);
      setSuccess("Term created");
      setTermName("");
      setTermStartDate("");
      setTermEndDate("");
      setTermCurrent(false);
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create term");
    }
  }

  async function handleCreateSection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sectionSubjectId || !sectionSemesterId || !sectionTeacherId) return;
    setError(null);
    setSuccess(null);
    try {
      await createAdminSection(
        Number(sectionSubjectId),
        Number(sectionSemesterId),
        Number(sectionTeacherId),
        Number(sectionCapacity),
        sectionLessonType
      );
      setSuccess("Section created");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create section");
    }
  }

  async function handleAssignProfessor(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!assignSectionId || !assignTeacherId) return;
    setError(null);
    setSuccess(null);
    try {
      await assignAdminProfessor(Number(assignSectionId), Number(assignTeacherId));
      setSuccess("Professor assigned");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to assign professor");
    }
  }

  async function handleAddMeetingTime(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!meetingSectionId) return;
    setError(null);
    setSuccess(null);
    try {
      await addAdminMeetingTime(
        Number(meetingSectionId),
        meetingDay,
        meetingStartTime,
        meetingEndTime,
        meetingRoom.trim(),
        meetingLessonType
      );
      setSuccess("Meeting time added");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add meeting time");
    }
  }

  async function handleUpsertWindow(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!windowSemesterId) return;
    setError(null);
    setSuccess(null);
    try {
      await upsertAdminWindow(Number(windowSemesterId), windowType, windowStartDate, windowEndDate, windowActive);
      setSuccess("Registration window updated");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update window");
    }
  }

  async function handleCreateExam(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!examSectionId) return;
    setError(null);
    setSuccess(null);
    try {
      await createAdminExam(Number(examSectionId), examDate, examTime, examRoom.trim(), examFormat.trim());
      setSuccess("Exam session created");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create exam");
    }
  }

  async function handleDeleteExam(examId: number) {
    setError(null);
    setSuccess(null);
    try {
      await deleteAdminExam(examId);
      setSuccess("Exam session deleted");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete exam");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Admin Academic Setup</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <>
          <section className="card">
            <h3>Create Term</h3>
            <form className="inline-form" onSubmit={handleCreateTerm}>
              <label>
                Name
                <input value={termName} onChange={(e) => setTermName(e.target.value)} required />
              </label>
              <label>
                Start Date
                <input type="date" value={termStartDate} onChange={(e) => setTermStartDate(e.target.value)} required />
              </label>
              <label>
                End Date
                <input type="date" value={termEndDate} onChange={(e) => setTermEndDate(e.target.value)} required />
              </label>
              <label>
                <input type="checkbox" checked={termCurrent} onChange={(e) => setTermCurrent(e.target.checked)} /> Current
              </label>
              <button type="submit">Create Term</button>
            </form>
          </section>

          <section className="card">
            <h3>Create Section</h3>
            <form className="inline-form" onSubmit={handleCreateSection}>
              <label>
                Subject
                <select
                  value={sectionSubjectId}
                  onChange={(e) => setSectionSubjectId(e.target.value ? Number(e.target.value) : "")}
                >
                  {subjects.map((subject) => (
                    <option key={subject.id} value={subject.id}>
                      {subject.code} - {subject.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Term
                <select
                  value={sectionSemesterId}
                  onChange={(e) => setSectionSemesterId(e.target.value ? Number(e.target.value) : "")}
                >
                  {terms.map((term) => (
                    <option key={term.id} value={term.id}>
                      {term.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Teacher
                <select
                  value={sectionTeacherId}
                  onChange={(e) => setSectionTeacherId(e.target.value ? Number(e.target.value) : "")}
                >
                  {teachers.map((teacher) => (
                    <option key={teacher.id} value={teacher.id}>
                      {teacher.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Capacity
                <input type="number" min={1} value={sectionCapacity} onChange={(e) => setSectionCapacity(Number(e.target.value))} />
              </label>
              <label>
                Lesson Type
                <select value={sectionLessonType} onChange={(e) => setSectionLessonType(e.target.value as (typeof lessonTypes)[number])}>
                  {lessonTypes.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>
              <button type="submit">Create Section</button>
            </form>
          </section>

          <section className="card">
            <h3>Assign Professor</h3>
            <form className="inline-form" onSubmit={handleAssignProfessor}>
              <label>
                Section
                <select
                  value={assignSectionId}
                  onChange={(e) => setAssignSectionId(e.target.value ? Number(e.target.value) : "")}
                >
                  {sections.map((section) => (
                    <option key={section.id} value={section.id}>
                      {section.id} - {section.subject?.code || "N/A"}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Teacher
                <select
                  value={assignTeacherId}
                  onChange={(e) => setAssignTeacherId(e.target.value ? Number(e.target.value) : "")}
                >
                  {teachers.map((teacher) => (
                    <option key={teacher.id} value={teacher.id}>
                      {teacher.name}
                    </option>
                  ))}
                </select>
              </label>
              <button type="submit">Assign</button>
            </form>
          </section>

          <section className="card">
            <h3>Add Meeting Time</h3>
            <form className="inline-form" onSubmit={handleAddMeetingTime}>
              <label>
                Section
                <select
                  value={meetingSectionId}
                  onChange={(e) => setMeetingSectionId(e.target.value ? Number(e.target.value) : "")}
                >
                  {sections.map((section) => (
                    <option key={section.id} value={section.id}>
                      {section.id} - {section.subject?.code || "N/A"}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Day
                <select value={meetingDay} onChange={(e) => setMeetingDay(e.target.value as (typeof weekDays)[number])}>
                  {weekDays.map((day) => (
                    <option key={day} value={day}>
                      {day}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Start
                <input type="time" value={meetingStartTime} onChange={(e) => setMeetingStartTime(e.target.value)} />
              </label>
              <label>
                End
                <input type="time" value={meetingEndTime} onChange={(e) => setMeetingEndTime(e.target.value)} />
              </label>
              <label>
                Room
                <input value={meetingRoom} onChange={(e) => setMeetingRoom(e.target.value)} />
              </label>
              <label>
                Lesson Type
                <select
                  value={meetingLessonType}
                  onChange={(e) => setMeetingLessonType(e.target.value as (typeof lessonTypes)[number])}
                >
                  {lessonTypes.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </label>
              <button type="submit">Add Meeting</button>
            </form>
          </section>

          <section className="card">
            <h3>Registration Window</h3>
            <form className="inline-form" onSubmit={handleUpsertWindow}>
              <label>
                Term
                <select
                  value={windowSemesterId}
                  onChange={(e) => setWindowSemesterId(e.target.value ? Number(e.target.value) : "")}
                >
                  {terms.map((term) => (
                    <option key={term.id} value={term.id}>
                      {term.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Type
                <select value={windowType} onChange={(e) => setWindowType(e.target.value as (typeof windowTypes)[number])}>
                  {windowTypes.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Start
                <input type="date" value={windowStartDate} onChange={(e) => setWindowStartDate(e.target.value)} required />
              </label>
              <label>
                End
                <input type="date" value={windowEndDate} onChange={(e) => setWindowEndDate(e.target.value)} required />
              </label>
              <label>
                <input type="checkbox" checked={windowActive} onChange={(e) => setWindowActive(e.target.checked)} /> Active
              </label>
              <button type="submit">Save Window</button>
            </form>
          </section>

          <section className="card">
            <h3>Exam Scheduling</h3>
            <form className="inline-form" onSubmit={handleCreateExam}>
              <label>
                Section
                <select value={examSectionId} onChange={(e) => setExamSectionId(e.target.value ? Number(e.target.value) : "")}>
                  {sections.map((section) => (
                    <option key={section.id} value={section.id}>
                      {section.id} - {section.subject?.code || "N/A"}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Date
                <input type="date" value={examDate} onChange={(e) => setExamDate(e.target.value)} required />
              </label>
              <label>
                Time
                <input type="time" value={examTime} onChange={(e) => setExamTime(e.target.value)} required />
              </label>
              <label>
                Room
                <input value={examRoom} onChange={(e) => setExamRoom(e.target.value)} />
              </label>
              <label>
                Format
                <input value={examFormat} onChange={(e) => setExamFormat(e.target.value)} />
              </label>
              <button type="submit">Create Exam</button>
            </form>

            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Section</th>
                    <th>Date</th>
                    <th>Time</th>
                    <th>Room</th>
                    <th>Format</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {exams.map((exam) => (
                    <tr key={exam.id}>
                      <td>{exam.id}</td>
                      <td>{exam.subjectOffering?.id || "-"}</td>
                      <td>{exam.examDate}</td>
                      <td>{exam.examTime}</td>
                      <td>{exam.room}</td>
                      <td>{exam.format}</td>
                      <td>
                        <button type="button" onClick={() => handleDeleteExam(exam.id)}>
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}

