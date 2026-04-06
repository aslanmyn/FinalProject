import { type FormEvent, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  addAdminMeetingTime,
  assignAdminProfessor,
  createAdminExam,
  createAdminSection,
  createAdminStudent,
  createAdminTerm,
  deleteAdminExam,
  fetchAdminExams,
  fetchAdminFaculties,
  fetchAdminPrograms,
  fetchAdminSections,
  fetchAdminStudentDetail,
  fetchAdminStudents,
  fetchAdminSubjects,
  fetchAdminTeachers,
  fetchAdminTerms,
  updateAdminStudent,
  upsertAdminWindow
} from "../../lib/api";
import type {
  AdminExamItem,
  AdminFacultyItem,
  AdminProgramItem,
  AdminSectionItem,
  AdminSimpleStudentItem,
  AdminSimpleSubjectItem,
  AdminSimpleTeacherItem,
  AdminStudentDetail,
  AdminStudentUpsertPayload,
  AdminTermItem
} from "../../types/admin";

const lessonTypes = ["LECTURE", "PRACTICE", "LAB"] as const;
const windowTypes = ["REGISTRATION", "ADD_DROP", "FX", "GRADE_PUBLISH"] as const;
const weekDays = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"] as const;
const studentStatuses = ["ACTIVE", "ON_LEAVE", "GRADUATED"] as const;

type StudentStatus = (typeof studentStatuses)[number];

type StudentFormState = {
  email: string;
  password: string;
  fullName: string;
  facultyId: number | "";
  programId: number | "";
  currentSemesterId: number | "";
  course: number;
  groupName: string;
  status: StudentStatus;
  creditsEarned: number;
  passportNumber: string;
  address: string;
  phone: string;
  emergencyContact: string;
  enabled: boolean;
};

function createEmptyStudentForm(): StudentFormState {
  return {
    email: "",
    password: "student123",
    fullName: "",
    facultyId: "",
    programId: "",
    currentSemesterId: "",
    course: 1,
    groupName: "TBD",
    status: "ACTIVE",
    creditsEarned: 0,
    passportNumber: "",
    address: "",
    phone: "",
    emergencyContact: "",
    enabled: true
  };
}

function buildStudentFormDefaults(
  faculties: AdminFacultyItem[],
  programs: AdminProgramItem[],
  terms: AdminTermItem[]
): StudentFormState {
  const defaults = createEmptyStudentForm();
  const facultyId = faculties[0]?.id ?? "";
  const programId = facultyId ? programs.find((program) => program.facultyId === facultyId)?.id ?? "" : "";
  return {
    ...defaults,
    facultyId,
    programId,
    currentSemesterId: terms[0]?.id ?? ""
  };
}

function mapStudentDetailToForm(detail: AdminStudentDetail): StudentFormState {
  return {
    email: detail.email,
    password: "",
    fullName: detail.fullName,
    facultyId: detail.facultyId ?? "",
    programId: detail.programId ?? "",
    currentSemesterId: detail.currentSemesterId ?? "",
    course: detail.course,
    groupName: detail.groupName ?? "",
    status: (studentStatuses.includes(detail.status as StudentStatus) ? detail.status : "ACTIVE") as StudentStatus,
    creditsEarned: detail.creditsEarned,
    passportNumber: detail.passportNumber ?? "",
    address: detail.address ?? "",
    phone: detail.phone ?? "",
    emergencyContact: detail.emergencyContact ?? "",
    enabled: detail.enabled
  };
}

function toStudentPayload(form: StudentFormState): AdminStudentUpsertPayload {
  if (!form.facultyId || !form.programId || !form.currentSemesterId) {
    throw new Error("Faculty, program, and semester are required");
  }

  return {
    email: form.email.trim(),
    password: form.password,
    fullName: form.fullName.trim(),
    facultyId: Number(form.facultyId),
    programId: Number(form.programId),
    currentSemesterId: Number(form.currentSemesterId),
    course: Number(form.course),
    groupName: form.groupName.trim(),
    status: form.status,
    creditsEarned: Number(form.creditsEarned),
    passportNumber: form.passportNumber.trim(),
    address: form.address.trim(),
    phone: form.phone.trim(),
    emergencyContact: form.emergencyContact.trim(),
    enabled: form.enabled
  };
}

function syncProgramForFaculty(
  nextFacultyId: number | "",
  currentProgramId: number | "",
  programs: AdminProgramItem[]
): number | "" {
  if (!nextFacultyId) {
    return "";
  }
  const filteredPrograms = programs.filter((program) => program.facultyId === nextFacultyId);
  if (filteredPrograms.some((program) => program.id === currentProgramId)) {
    return currentProgramId;
  }
  return filteredPrograms[0]?.id ?? "";
}

export default function AdminAcademicPage() {
  const [terms, setTerms] = useState<AdminTermItem[]>([]);
  const [subjects, setSubjects] = useState<AdminSimpleSubjectItem[]>([]);
  const [teachers, setTeachers] = useState<AdminSimpleTeacherItem[]>([]);
  const [sections, setSections] = useState<AdminSectionItem[]>([]);
  const [exams, setExams] = useState<AdminExamItem[]>([]);
  const [students, setStudents] = useState<AdminSimpleStudentItem[]>([]);
  const [faculties, setFaculties] = useState<AdminFacultyItem[]>([]);
  const [programs, setPrograms] = useState<AdminProgramItem[]>([]);
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

  const [createStudentForm, setCreateStudentForm] = useState<StudentFormState>(createEmptyStudentForm());
  const [editStudentForm, setEditStudentForm] = useState<StudentFormState>(createEmptyStudentForm());
  const [selectedStudentId, setSelectedStudentId] = useState<number | "">("");
  const [selectedStudentMeta, setSelectedStudentMeta] = useState<AdminStudentDetail | null>(null);
  const [loadingStudentDetails, setLoadingStudentDetails] = useState(false);

  const createPrograms = useMemo(
    () => programs.filter((program) => !createStudentForm.facultyId || program.facultyId === createStudentForm.facultyId),
    [programs, createStudentForm.facultyId]
  );
  const editPrograms = useMemo(
    () => programs.filter((program) => !editStudentForm.facultyId || program.facultyId === editStudentForm.facultyId),
    [programs, editStudentForm.facultyId]
  );

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [termsData, subjectsData, teachersData, sectionsData, examsData, studentsData, facultiesData, programsData] = await Promise.all([
        fetchAdminTerms(),
        fetchAdminSubjects(),
        fetchAdminTeachers(),
        fetchAdminSections(),
        fetchAdminExams(),
        fetchAdminStudents(),
        fetchAdminFaculties(),
        fetchAdminPrograms()
      ]);
      setTerms(termsData);
      setSubjects(subjectsData);
      setTeachers(teachersData);
      setSections(sectionsData);
      setExams(examsData);
      setStudents(studentsData);
      setFaculties(facultiesData);
      setPrograms(programsData);

      setSectionSubjectId((current) => current || subjectsData[0]?.id || "");
      setSectionSemesterId((current) => current || termsData[0]?.id || "");
      setSectionTeacherId((current) => current || teachersData[0]?.id || "");
      setAssignSectionId((current) => current || sectionsData[0]?.id || "");
      setAssignTeacherId((current) => current || teachersData[0]?.id || "");
      setMeetingSectionId((current) => current || sectionsData[0]?.id || "");
      setWindowSemesterId((current) => current || termsData[0]?.id || "");
      setExamSectionId((current) => current || sectionsData[0]?.id || "");
      setSelectedStudentId((current) => current || studentsData[0]?.id || "");
      setCreateStudentForm((current) =>
        current.facultyId || current.currentSemesterId ? current : buildStudentFormDefaults(facultiesData, programsData, termsData)
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load academic data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  useEffect(() => {
    if (!selectedStudentId) {
      setSelectedStudentMeta(null);
      setEditStudentForm(createEmptyStudentForm());
      return;
    }

    let active = true;
    setLoadingStudentDetails(true);
    setError(null);

    void fetchAdminStudentDetail(Number(selectedStudentId))
      .then((detail) => {
        if (!active) return;
        setSelectedStudentMeta(detail);
        setEditStudentForm(mapStudentDetailToForm(detail));
      })
      .catch((err) => {
        if (!active) return;
        setError(err instanceof ApiError ? err.message : "Failed to load student details");
      })
      .finally(() => {
        if (active) {
          setLoadingStudentDetails(false);
        }
      });

    return () => {
      active = false;
    };
  }, [selectedStudentId]);

  function updateCreateStudentField<K extends keyof StudentFormState>(key: K, value: StudentFormState[K]) {
    setCreateStudentForm((prev) => {
      if (key === "facultyId") {
        const nextFacultyId = value as StudentFormState["facultyId"];
        return {
          ...prev,
          facultyId: nextFacultyId,
          programId: syncProgramForFaculty(nextFacultyId, prev.programId, programs)
        };
      }
      return { ...prev, [key]: value };
    });
  }

  function updateEditStudentField<K extends keyof StudentFormState>(key: K, value: StudentFormState[K]) {
    setEditStudentForm((prev) => {
      if (key === "facultyId") {
        const nextFacultyId = value as StudentFormState["facultyId"];
        return {
          ...prev,
          facultyId: nextFacultyId,
          programId: syncProgramForFaculty(nextFacultyId, prev.programId, programs)
        };
      }
      return { ...prev, [key]: value };
    });
  }

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

  async function handleCreateStudent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      const created = await createAdminStudent(toStudentPayload(createStudentForm));
      setSuccess(`Student created: ${created.fullName}`);
      await loadData();
      setSelectedStudentId(created.studentId);
      setCreateStudentForm(buildStudentFormDefaults(faculties, programs, terms));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : err instanceof Error ? err.message : "Failed to create student");
    }
  }

  async function handleUpdateStudent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedStudentId) return;
    setError(null);
    setSuccess(null);
    try {
      const updated = await updateAdminStudent(Number(selectedStudentId), toStudentPayload(editStudentForm));
      setSuccess(`Student updated: ${updated.fullName}`);
      await loadData();
      const refreshed = await fetchAdminStudentDetail(Number(selectedStudentId));
      setSelectedStudentMeta(refreshed);
      setEditStudentForm(mapStudentDetailToForm(refreshed));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : err instanceof Error ? err.message : "Failed to update student");
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
            <h3>Student Management</h3>
            <p className="muted">
              Create a new student with full academic data or update an existing profile without leaving the admin panel.
            </p>
          </section>

          <section className="card">
            <h3>Create Student</h3>
            <form className="form" onSubmit={handleCreateStudent}>
              <div className="inline-form">
                <label>
                  Email
                  <input
                    value={createStudentForm.email}
                    onChange={(event) => updateCreateStudentField("email", event.target.value)}
                    placeholder="a_testov@kbtu.kz"
                    required
                  />
                </label>
                <label>
                  Password
                  <input
                    value={createStudentForm.password}
                    onChange={(event) => updateCreateStudentField("password", event.target.value)}
                    minLength={6}
                    required
                  />
                </label>
                <label>
                  Full Name
                  <input
                    value={createStudentForm.fullName}
                    onChange={(event) => updateCreateStudentField("fullName", event.target.value)}
                    required
                  />
                </label>
                <label>
                  Course
                  <input
                    type="number"
                    min={1}
                    value={createStudentForm.course}
                    onChange={(event) => updateCreateStudentField("course", Number(event.target.value))}
                    required
                  />
                </label>
                <label>
                  Group
                  <input
                    value={createStudentForm.groupName}
                    onChange={(event) => updateCreateStudentField("groupName", event.target.value)}
                  />
                </label>
                <label>
                  Status
                  <select
                    value={createStudentForm.status}
                    onChange={(event) => updateCreateStudentField("status", event.target.value as StudentStatus)}
                  >
                    {studentStatuses.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Faculty
                  <select
                    value={createStudentForm.facultyId}
                    onChange={(event) =>
                      updateCreateStudentField("facultyId", event.target.value ? Number(event.target.value) : "")
                    }
                    required
                  >
                    <option value="">Select faculty</option>
                    {faculties.map((faculty) => (
                      <option key={faculty.id} value={faculty.id}>
                        {faculty.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Program
                  <select
                    value={createStudentForm.programId}
                    onChange={(event) =>
                      updateCreateStudentField("programId", event.target.value ? Number(event.target.value) : "")
                    }
                    required
                  >
                    <option value="">Select program</option>
                    {createPrograms.map((program) => (
                      <option key={program.id} value={program.id}>
                        {program.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Semester
                  <select
                    value={createStudentForm.currentSemesterId}
                    onChange={(event) =>
                      updateCreateStudentField("currentSemesterId", event.target.value ? Number(event.target.value) : "")
                    }
                    required
                  >
                    <option value="">Select semester</option>
                    {terms.map((term) => (
                      <option key={term.id} value={term.id}>
                        {term.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Credits Earned
                  <input
                    type="number"
                    min={0}
                    value={createStudentForm.creditsEarned}
                    onChange={(event) => updateCreateStudentField("creditsEarned", Number(event.target.value))}
                  />
                </label>
                <label>
                  Passport Number
                  <input
                    value={createStudentForm.passportNumber}
                    onChange={(event) => updateCreateStudentField("passportNumber", event.target.value)}
                  />
                </label>
                <label>
                  Phone
                  <input value={createStudentForm.phone} onChange={(event) => updateCreateStudentField("phone", event.target.value)} />
                </label>
                <label>
                  Emergency Contact
                  <input
                    value={createStudentForm.emergencyContact}
                    onChange={(event) => updateCreateStudentField("emergencyContact", event.target.value)}
                  />
                </label>
                <label>
                  Address
                  <input value={createStudentForm.address} onChange={(event) => updateCreateStudentField("address", event.target.value)} />
                </label>
                <label>
                  <input
                    type="checkbox"
                    checked={createStudentForm.enabled}
                    onChange={(event) => updateCreateStudentField("enabled", event.target.checked)}
                  />{" "}
                  Enabled
                </label>
              </div>
              <button type="submit">Create Student</button>
            </form>
          </section>

          <section className="card">
            <h3>Edit Existing Student</h3>
            <form className="inline-form" onSubmit={(event) => event.preventDefault()}>
              <label>
                Student
                <select
                  value={selectedStudentId}
                  onChange={(event) => setSelectedStudentId(event.target.value ? Number(event.target.value) : "")}
                >
                  <option value="">Select student</option>
                  {students.map((student) => (
                    <option key={student.id} value={student.id}>
                      {student.id} - {student.name} ({student.email})
                    </option>
                  ))}
                </select>
              </label>
            </form>

            {loadingStudentDetails ? <p>Loading student details...</p> : null}

            {selectedStudentMeta ? (
              <form className="form" onSubmit={handleUpdateStudent}>
                <div className="inline-form">
                  <label>
                    Email
                    <input
                      value={editStudentForm.email}
                      onChange={(event) => updateEditStudentField("email", event.target.value)}
                      required
                    />
                  </label>
                  <label>
                    New Password
                    <input
                      value={editStudentForm.password}
                      onChange={(event) => updateEditStudentField("password", event.target.value)}
                      placeholder="Leave blank to keep current password"
                    />
                  </label>
                  <label>
                    Full Name
                    <input
                      value={editStudentForm.fullName}
                      onChange={(event) => updateEditStudentField("fullName", event.target.value)}
                      required
                    />
                  </label>
                  <label>
                    Course
                    <input
                      type="number"
                      min={1}
                      value={editStudentForm.course}
                      onChange={(event) => updateEditStudentField("course", Number(event.target.value))}
                      required
                    />
                  </label>
                  <label>
                    Group
                    <input
                      value={editStudentForm.groupName}
                      onChange={(event) => updateEditStudentField("groupName", event.target.value)}
                    />
                  </label>
                  <label>
                    Status
                    <select
                      value={editStudentForm.status}
                      onChange={(event) => updateEditStudentField("status", event.target.value as StudentStatus)}
                    >
                      {studentStatuses.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Faculty
                    <select
                      value={editStudentForm.facultyId}
                      onChange={(event) =>
                        updateEditStudentField("facultyId", event.target.value ? Number(event.target.value) : "")
                      }
                      required
                    >
                      <option value="">Select faculty</option>
                      {faculties.map((faculty) => (
                        <option key={faculty.id} value={faculty.id}>
                          {faculty.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Program
                    <select
                      value={editStudentForm.programId}
                      onChange={(event) =>
                        updateEditStudentField("programId", event.target.value ? Number(event.target.value) : "")
                      }
                      required
                    >
                      <option value="">Select program</option>
                      {editPrograms.map((program) => (
                        <option key={program.id} value={program.id}>
                          {program.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Semester
                    <select
                      value={editStudentForm.currentSemesterId}
                      onChange={(event) =>
                        updateEditStudentField("currentSemesterId", event.target.value ? Number(event.target.value) : "")
                      }
                      required
                    >
                      <option value="">Select semester</option>
                      {terms.map((term) => (
                        <option key={term.id} value={term.id}>
                          {term.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Credits Earned
                    <input
                      type="number"
                      min={0}
                      value={editStudentForm.creditsEarned}
                      onChange={(event) => updateEditStudentField("creditsEarned", Number(event.target.value))}
                    />
                  </label>
                  <label>
                    Passport Number
                    <input
                      value={editStudentForm.passportNumber}
                      onChange={(event) => updateEditStudentField("passportNumber", event.target.value)}
                    />
                  </label>
                  <label>
                    Phone
                    <input value={editStudentForm.phone} onChange={(event) => updateEditStudentField("phone", event.target.value)} />
                  </label>
                  <label>
                    Emergency Contact
                    <input
                      value={editStudentForm.emergencyContact}
                      onChange={(event) => updateEditStudentField("emergencyContact", event.target.value)}
                    />
                  </label>
                  <label>
                    Address
                    <input value={editStudentForm.address} onChange={(event) => updateEditStudentField("address", event.target.value)} />
                  </label>
                  <label>
                    <input
                      type="checkbox"
                      checked={editStudentForm.enabled}
                      onChange={(event) => updateEditStudentField("enabled", event.target.checked)}
                    />{" "}
                    Enabled
                  </label>
                </div>
                <button type="submit" disabled={!selectedStudentId}>
                  Save Student Changes
                </button>
              </form>
            ) : selectedStudentId ? (
              <p className="muted">No student details loaded yet.</p>
            ) : (
              <p className="muted">Select a student to edit the existing profile.</p>
            )}
          </section>

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

