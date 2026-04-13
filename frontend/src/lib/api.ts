import { clearAuthSession, getAccessToken, getRefreshToken, saveAuthSession } from "./auth";
import type { LoginResponse, RegisterResponse } from "../types/auth";
import type {
  AdminAnalyticsDashboard,
  AdminAssistantReply,
  AdminExamItem,
  AdminFacultyItem,
  AdminFxItem,
  AdminGradeChangeItem,
  AdminHoldItem,
  AdminNotificationCenterData,
  AdminProgramItem,
  AdminRequestPage,
  AdminSectionItem,
  AdminSubjectDetail,
  AdminSubjectUpsertPayload,
  AdminSubjectUpsertResult,
  AdminTeacherDetail,
  AdminTeacherUpsertPayload,
  AdminTeacherUpsertResult,
  AdminStudentDetail,
  AdminSimpleStudentItem,
  AdminStudentUpsertPayload,
  AdminStudentUpsertResult,
  AdminSimpleSubjectItem,
  AdminSimpleTeacherItem,
  AdminStats,
  AdminTermItem,
  AdminUserPage,
  AdminWorkflowOverview,
  AdminWindowItem
} from "../types/admin";
import type {
  PublicNewsItem,
  PublicProfessorListItem,
  PublicProfessorProfile
} from "../types/public";
import type {
  StudentAssistantReply,
  StudentAttendanceData,
  StudentEnrollmentItem,
  StudentEnrollmentOptions,
  StudentExamScheduleItem,
  StudentFileItem,
  StudentFinancialData,
  StudentFxOverview,
  StudentJournalItem,
  StudentJournalOptions,
  StudentNewsItem,
  StudentNextSemesterOverview,
  StudentNotificationCenterData,
  StudentProfile,
  StudentRegistrationOverview,
  StudentCourseCatalogItem,
  StudentActionResult,
  StudentPlannerDashboard,
  StudentPlannerSimulation,
  StudentRiskDashboard,
  StudentRequestPage,
  StudentSectionDetail,
  StudentScheduleItem,
  StudentScheduleOptions,
  StudentTranscriptData,
  StudentWorkflowOverview
} from "../types/student";
import type {
  TeacherAnnouncementItem,
  TeacherAssistantReply,
  TeacherActiveAttendancePayload,
  TeacherComponentItem,
  TeacherGradeChangeRequestItem,
  TeacherMaterialItem,
  TeacherAttendanceRecordItem,
  TeacherNotificationCenterData,
  TeacherNoteItem,
  TeacherRiskDashboard,
  TeacherStudentFileItem,
  TeacherProfile,
  TeacherRosterItem,
  TeacherSectionItem
} from "../types/teacher";
import type { ApiPageResponse, WorkflowTimeline, WorkflowType } from "../types/common";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

interface ApiRequestOptions {
  method?: HttpMethod;
  body?: unknown;
  auth?: boolean;
  headers?: Record<string, string>;
  retryOnUnauthorized?: boolean;
}

export class ApiError extends Error {
  status: number;
  code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

let refreshPromise: Promise<boolean> | null = null;

function buildApiUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  return `${API_BASE_URL}${path}`;
}

async function refreshAccessToken(): Promise<boolean> {
  const currentRefreshToken = getRefreshToken();
  if (!currentRefreshToken) {
    clearAuthSession();
    return false;
  }

  const response = await fetch(buildApiUrl("/api/v1/auth/refresh"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: currentRefreshToken })
  });

  if (!response.ok) {
    clearAuthSession();
    return false;
  }

  const payload = (await response.json()) as LoginResponse;
  saveAuthSession(payload);
  return true;
}

async function ensureRefreshedSession(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken();
  }
  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

async function request<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const retryOnUnauthorized = options.retryOnUnauthorized ?? true;
  const token = getAccessToken();
  const headers: Record<string, string> = { ...(options.headers || {}) };

  const hasBody = options.body !== undefined && options.body !== null;
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
  if (hasBody && !isFormData) {
    headers["Content-Type"] = "application/json";
  }

  if (options.auth !== false && token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(buildApiUrl(path), {
    method: options.method || "GET",
    headers,
    body: !hasBody ? undefined : isFormData ? (options.body as FormData) : JSON.stringify(options.body)
  });

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const payload = isJson ? await response.json() : null;

  if (!response.ok) {
    if (response.status === 401 && options.auth !== false && retryOnUnauthorized) {
      const refreshed = await ensureRefreshedSession();
      if (refreshed) {
        return request<T>(path, { ...options, retryOnUnauthorized: false });
      }
      clearAuthSession();
    } else if (response.status === 401) {
      clearAuthSession();
    }
    const message = payload?.message || `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status, payload?.code);
  }

  return payload as T;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { email, password },
    auth: false
  });
}

export async function register(
  email: string,
  password: string,
  confirmPassword: string,
  fullName: string
): Promise<RegisterResponse> {
  return request<RegisterResponse>("/api/v1/auth/register", {
    method: "POST",
    body: { email, password, confirmPassword, fullName },
    auth: false
  });
}

export async function logout(): Promise<void> {
  const refreshToken = localStorage.getItem("kbtu_refresh_token");
  if (!refreshToken) return;
  await request<void>("/api/v1/auth/logout", {
    method: "POST",
    body: { refreshToken },
    auth: false
  });
}

export async function fetchStudentProfile(): Promise<StudentProfile> {
  return request<StudentProfile>("/api/v1/student/profile");
}

export async function updateStudentProfile(payload: {
  phone: string;
  address: string;
  emergencyContact: string;
}): Promise<StudentProfile> {
  return request<StudentProfile>("/api/v1/student/profile", {
    method: "PUT",
    body: payload
  });
}

export async function uploadStudentProfilePhoto(file: File): Promise<StudentProfile> {
  const formData = new FormData();
  formData.append("file", file);
  return request<StudentProfile>("/api/v1/student/profile-photo", {
    method: "POST",
    body: formData
  });
}

export async function fetchStudentSchedule(semesterId?: number): Promise<StudentScheduleItem[]> {
  const suffix = semesterId ? `?semesterId=${semesterId}` : "";
  return request<StudentScheduleItem[]>(`/api/v1/student/schedule${suffix}`);
}

export async function fetchStudentScheduleOptions(): Promise<StudentScheduleOptions> {
  return request<StudentScheduleOptions>("/api/v1/student/schedule/options");
}

export async function fetchStudentSectionDetail(sectionId: number): Promise<StudentSectionDetail> {
  return request<StudentSectionDetail>(`/api/v1/student/sections/${sectionId}`);
}

export async function fetchStudentEnrollments(semesterId?: number): Promise<StudentEnrollmentItem[]> {
  const suffix = semesterId ? `?semesterId=${semesterId}` : "";
  return request<StudentEnrollmentItem[]>(`/api/v1/student/enrollments${suffix}`);
}

export async function fetchStudentEnrollmentOptions(): Promise<StudentEnrollmentOptions> {
  return request<StudentEnrollmentOptions>("/api/v1/student/enrollments/options");
}

export async function fetchStudentJournal(semesterId?: number): Promise<StudentJournalItem[]> {
  const suffix = semesterId ? `?semesterId=${semesterId}` : "";
  return request<StudentJournalItem[]>(`/api/v1/student/journal${suffix}`);
}

export async function fetchStudentJournalOptions(): Promise<StudentJournalOptions> {
  return request<StudentJournalOptions>("/api/v1/student/journal/options");
}

export async function fetchStudentTranscript(): Promise<StudentTranscriptData> {
  return request<StudentTranscriptData>("/api/v1/student/transcript");
}

export async function fetchStudentAttendance(): Promise<StudentAttendanceData> {
  return request<StudentAttendanceData>("/api/v1/student/attendance");
}

export async function fetchStudentActiveAttendance(): Promise<StudentAttendanceData["activeSessions"]> {
  return request<StudentAttendanceData["activeSessions"]>("/api/v1/student/attendance/active");
}

export async function checkInStudentAttendance(sessionId: number, code?: string): Promise<{
  sessionId: number;
  status: string;
  markedBy: string;
  teacherConfirmed: boolean;
  updatedAt: string | null;
}> {
  return request(`/api/v1/student/attendance-sessions/${sessionId}/check-in`, {
    method: "POST",
    body: code ? { code } : {}
  });
}

export async function fetchStudentRegistrationOverview(): Promise<StudentRegistrationOverview> {
  return request<StudentRegistrationOverview>("/api/v1/student/course-registration/overview");
}

export async function fetchStudentRegistrationCatalog(): Promise<StudentCourseCatalogItem[]> {
  return request<StudentCourseCatalogItem[]>("/api/v1/student/course-registration/catalog");
}

export async function fetchStudentNextSemesterOverview(): Promise<StudentNextSemesterOverview> {
  return request<StudentNextSemesterOverview>("/api/v1/student/course-registration/next-semester");
}

export async function saveStudentNextSemesterSubject(subjectId: number): Promise<StudentActionResult> {
  return request<StudentActionResult>("/api/v1/student/course-registration/next-semester/subjects/save", {
    method: "POST",
    body: { subjectId }
  });
}

export async function removeStudentNextSemesterSubject(subjectId: number): Promise<StudentActionResult> {
  return request<StudentActionResult>("/api/v1/student/course-registration/next-semester/subjects/remove", {
    method: "POST",
    body: { subjectId }
  });
}

export async function saveStudentNextSemesterSection(subjectId: number, sectionId: number): Promise<StudentActionResult> {
  return request<StudentActionResult>("/api/v1/student/course-registration/next-semester/sections/select", {
    method: "POST",
    body: { subjectId, sectionId }
  });
}

export async function clearStudentNextSemesterSection(subjectId: number): Promise<StudentActionResult> {
  return request<StudentActionResult>("/api/v1/student/course-registration/next-semester/sections/remove", {
    method: "POST",
    body: { subjectId }
  });
}

export async function submitStudentRegistration(sectionId: number): Promise<StudentActionResult> {
  return request<StudentActionResult>("/api/v1/student/course-registration/submit", {
    method: "POST",
    body: { sectionId }
  });
}

export async function addStudentCourse(sectionId: number): Promise<StudentActionResult> {
  return request<StudentActionResult>("/api/v1/student/add-drop/add", {
    method: "POST",
    body: { sectionId }
  });
}

export async function dropStudentCourse(sectionId: number): Promise<StudentActionResult> {
  return request<StudentActionResult>("/api/v1/student/add-drop/drop", {
    method: "POST",
    body: { sectionId }
  });
}

export async function fetchStudentFxOverview(): Promise<StudentFxOverview> {
  return request<StudentFxOverview>("/api/v1/student/fx");
}

export async function createStudentFxRegistration(sectionId: number): Promise<void> {
  await request("/api/v1/student/fx", {
    method: "POST",
    body: { sectionId }
  });
}

export async function fetchStudentNotifications(): Promise<StudentNotificationCenterData> {
  return request<StudentNotificationCenterData>("/api/v1/student/notifications");
}

export async function markStudentNotificationRead(id: number): Promise<void> {
  await request(`/api/v1/student/notifications/${id}/read`, { method: "POST" });
}

export async function markAllStudentNotificationsRead(): Promise<void> {
  await request("/api/v1/student/notifications/read-all", { method: "POST" });
}

export async function askStudentAssistant(message: string): Promise<StudentAssistantReply> {
  return request<StudentAssistantReply>("/api/v1/student/assistant/chat", {
    method: "POST",
    body: { message }
  });
}

export async function fetchStudentScheduleDemo(): Promise<StudentAssistantReply> {
  return request<StudentAssistantReply>("/api/v1/student/assistant/schedule-demo");
}

export async function fetchStudentRiskDashboard(): Promise<StudentRiskDashboard> {
  return request<StudentRiskDashboard>("/api/v1/student/analytics/risk");
}

export async function fetchStudentPlanner(): Promise<StudentPlannerDashboard> {
  return request<StudentPlannerDashboard>("/api/v1/student/planner");
}

export async function simulateStudentPlanner(
  projectedFinals: Array<{ sectionId: number; projectedFinalScore: number }>
): Promise<StudentPlannerSimulation> {
  return request<StudentPlannerSimulation>("/api/v1/student/planner/simulate", {
    method: "POST",
    body: { projectedFinals }
  });
}

export async function fetchStudentWorkflows(): Promise<StudentWorkflowOverview> {
  return request<StudentWorkflowOverview>("/api/v1/student/workflows");
}

export async function fetchStudentExamSchedule(): Promise<StudentExamScheduleItem[]> {
  return request<StudentExamScheduleItem[]>("/api/v1/student/exam-schedule");
}

export async function fetchStudentNews(): Promise<StudentNewsItem[]> {
  return request<StudentNewsItem[]>("/api/v1/student/news");
}

export async function fetchStudentRequests(page = 0, size = 20): Promise<StudentRequestPage> {
  return request<StudentRequestPage>(`/api/v1/student/requests?page=${page}&size=${size}`);
}

export async function fetchStudentFinancial(): Promise<StudentFinancialData> {
  return request<StudentFinancialData>("/api/v1/student/financial");
}

export async function fetchStudentFiles(): Promise<StudentFileItem[]> {
  return request<StudentFileItem[]>("/api/v1/student/files");
}

export async function createStudentRequest(category: string, description: string): Promise<void> {
  await request("/api/v1/student/requests", {
    method: "POST",
    body: { category, description }
  });
}

export async function fetchTeacherProfile(): Promise<TeacherProfile> {
  return request<TeacherProfile>("/api/v1/teacher/profile");
}

export async function uploadTeacherProfilePhoto(file: File): Promise<TeacherProfile> {
  const formData = new FormData();
  formData.append("file", file);
  return request<TeacherProfile>("/api/v1/teacher/profile-photo", {
    method: "POST",
    body: formData
  });
}

export async function fetchTeacherSections(): Promise<TeacherSectionItem[]> {
  return request<TeacherSectionItem[]>("/api/v1/teacher/sections");
}

export async function fetchTeacherNotifications(): Promise<TeacherNotificationCenterData> {
  return request<TeacherNotificationCenterData>("/api/v1/teacher/notifications");
}

export async function markTeacherNotificationRead(id: number): Promise<void> {
  await request(`/api/v1/teacher/notifications/${id}/read`, { method: "POST" });
}

export async function markAllTeacherNotificationsRead(): Promise<void> {
  await request("/api/v1/teacher/notifications/read-all", { method: "POST" });
}

export async function askTeacherAssistant(message: string): Promise<TeacherAssistantReply> {
  return request<TeacherAssistantReply>("/api/v1/teacher/assistant/chat", {
    method: "POST",
    body: { message }
  });
}

export async function fetchTeacherRiskDashboard(): Promise<TeacherRiskDashboard> {
  return request<TeacherRiskDashboard>("/api/v1/teacher/analytics/risk");
}

export async function fetchTeacherRoster(sectionId: number): Promise<TeacherRosterItem[]> {
  return request<TeacherRosterItem[]>(`/api/v1/teacher/sections/${sectionId}/roster`);
}

export async function fetchTeacherComponents(sectionId: number): Promise<TeacherComponentItem[]> {
  return request<TeacherComponentItem[]>(`/api/v1/teacher/sections/${sectionId}/components`);
}

export async function uploadTeacherStudentFile(
  sectionId: number,
  studentId: number,
  file: File
): Promise<TeacherStudentFileItem> {
  const formData = new FormData();
  formData.append("studentId", String(studentId));
  formData.append("file", file);
  return request<TeacherStudentFileItem>(`/api/v1/teacher/sections/${sectionId}/student-files`, {
    method: "POST",
    body: formData
  });
}

export interface TeacherAttendanceMarkInput {
  studentId: number;
  status: "PRESENT" | "LATE" | "ABSENT";
  reason?: string;
}

export async function markTeacherAttendance(
  sectionId: number,
  classDate: string,
  marks: TeacherAttendanceMarkInput[]
): Promise<void> {
  await request(`/api/v1/teacher/sections/${sectionId}/attendance`, {
    method: "POST",
    body: { classDate, marks }
  });
}

export async function fetchTeacherActiveAttendance(
  sectionId: number,
  classDate?: string
): Promise<TeacherActiveAttendancePayload> {
  const query = classDate ? `?classDate=${encodeURIComponent(classDate)}` : "";
  return request<TeacherActiveAttendancePayload>(`/api/v1/teacher/sections/${sectionId}/attendance/active${query}`);
}

export async function openTeacherAttendanceSession(
  sectionId: number,
  payload: {
    classDate: string;
    closeAt?: string;
    checkInMode: "ONE_CLICK" | "CODE";
    checkInCode?: string;
    allowTeacherOverride: boolean;
  }
): Promise<TeacherActiveAttendancePayload> {
  return request<TeacherActiveAttendancePayload>(`/api/v1/teacher/sections/${sectionId}/attendance/open`, {
    method: "POST",
    body: payload
  });
}

export async function closeTeacherAttendanceSession(
  sessionId: number
): Promise<TeacherActiveAttendancePayload> {
  return request<TeacherActiveAttendancePayload>(`/api/v1/teacher/attendance-sessions/${sessionId}/close`, {
    method: "POST"
  });
}

export async function fetchTeacherAttendanceRecords(
  sessionId: number
): Promise<TeacherAttendanceRecordItem[]> {
  return request<TeacherAttendanceRecordItem[]>(`/api/v1/teacher/attendance-sessions/${sessionId}/records`);
}

export async function overrideTeacherAttendance(
  sessionId: number,
  studentId: number,
  status: "PRESENT" | "LATE" | "ABSENT",
  reason = ""
): Promise<TeacherAttendanceRecordItem> {
  return request<TeacherAttendanceRecordItem>(`/api/v1/teacher/attendance-sessions/${sessionId}/students/${studentId}`, {
    method: "PUT",
    body: { status, reason }
  });
}

export async function createTeacherComponent(
  sectionId: number,
  name: string,
  type: "QUIZ" | "MIDTERM" | "FINAL" | "LAB" | "PROJECT" | "OTHER",
  weightPercent: number
): Promise<TeacherComponentItem> {
  return request<TeacherComponentItem>(`/api/v1/teacher/sections/${sectionId}/components`, {
    method: "POST",
    body: { name, type, weightPercent }
  });
}

export async function setTeacherComponentPublish(
  sectionId: number,
  componentId: number,
  published: boolean
): Promise<TeacherComponentItem> {
  return request<TeacherComponentItem>(
    `/api/v1/teacher/sections/${sectionId}/components/${componentId}/publish?published=${published}`,
    { method: "POST" }
  );
}

export async function setTeacherComponentLock(
  sectionId: number,
  componentId: number,
  locked: boolean
): Promise<TeacherComponentItem> {
  return request<TeacherComponentItem>(
    `/api/v1/teacher/sections/${sectionId}/components/${componentId}/lock?locked=${locked}`,
    { method: "POST" }
  );
}

export async function saveTeacherGrade(
  sectionId: number,
  studentId: number,
  componentId: number,
  gradeValue: number,
  maxGradeValue: number,
  comment: string
): Promise<void> {
  await request(`/api/v1/teacher/sections/${sectionId}/grades`, {
    method: "POST",
    body: { studentId, componentId, gradeValue, maxGradeValue, comment }
  });
}

export async function saveTeacherFinalGrade(
  sectionId: number,
  studentId: number,
  numericValue: number,
  letterValue: string,
  points: number
): Promise<void> {
  await request(`/api/v1/teacher/sections/${sectionId}/final-grades`, {
    method: "POST",
    body: { studentId, numericValue, letterValue, points }
  });
}

export async function publishTeacherFinalGrade(sectionId: number, studentId: number): Promise<void> {
  await request(`/api/v1/teacher/sections/${sectionId}/final-grades/${studentId}/publish`, { method: "POST" });
}

export async function fetchTeacherAnnouncements(sectionId: number): Promise<TeacherAnnouncementItem[]> {
  return request<TeacherAnnouncementItem[]>(`/api/v1/teacher/sections/${sectionId}/announcements`);
}

export async function createTeacherAnnouncement(
  sectionId: number,
  title: string,
  content: string,
  publicVisible = false,
  pinned = false,
  scheduledAt = ""
): Promise<void> {
  await request(`/api/v1/teacher/sections/${sectionId}/announcements`, {
    method: "POST",
    body: { title, content, publicVisible, pinned, scheduledAt }
  });
}

export async function fetchTeacherMaterials(sectionId: number): Promise<TeacherMaterialItem[]> {
  return request<TeacherMaterialItem[]>(`/api/v1/teacher/sections/${sectionId}/materials`);
}

export async function uploadTeacherMaterial(
  sectionId: number,
  title: string,
  description: string,
  visibility: "ENROLLED_ONLY" | "PUBLIC",
  file: File
): Promise<TeacherMaterialItem> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("description", description);
  formData.append("visibility", visibility);
  formData.append("file", file);
  return request<TeacherMaterialItem>(`/api/v1/teacher/sections/${sectionId}/materials`, {
    method: "POST",
    body: formData
  });
}

export async function setTeacherMaterialVisibility(materialId: number, published: boolean): Promise<void> {
  await request(`/api/v1/teacher/materials/${materialId}/visibility?published=${published}`, { method: "POST" });
}

export async function deleteTeacherMaterial(materialId: number): Promise<void> {
  await request(`/api/v1/teacher/materials/${materialId}`, { method: "DELETE" });
}

export async function fetchTeacherNotes(sectionId: number): Promise<TeacherNoteItem[]> {
  return request<TeacherNoteItem[]>(`/api/v1/teacher/sections/${sectionId}/student-notes`);
}

export async function upsertTeacherNote(
  sectionId: number,
  studentId: number,
  note: string,
  riskFlag: "NONE" | "LOW_ATTENDANCE" | "LOW_GRADES" | "COMBINED_RISK"
): Promise<void> {
  await request(`/api/v1/teacher/sections/${sectionId}/student-notes`, {
    method: "POST",
    body: { studentId, note, riskFlag }
  });
}

export async function fetchTeacherGradeChangeRequests(): Promise<TeacherGradeChangeRequestItem[]> {
  return request<TeacherGradeChangeRequestItem[]>("/api/v1/teacher/grade-change-requests");
}

export async function createTeacherGradeChangeRequest(
  sectionId: number,
  gradeId: number,
  newValue: number,
  reason: string
): Promise<void> {
  await request(`/api/v1/teacher/sections/${sectionId}/grade-change-requests`, {
    method: "POST",
    body: { gradeId, newValue, reason }
  });
}

export async function fetchAdminStats(): Promise<AdminStats> {
  return request<AdminStats>("/api/v1/admin/stats");
}

export async function fetchAdminUsers(page = 0, size = 20): Promise<AdminUserPage> {
  return request<AdminUserPage>(`/api/v1/admin/users?page=${page}&size=${size}`);
}

export async function setAdminUserPermissions(userId: number, permissions: string[]): Promise<void> {
  await request(`/api/v1/admin/users/${userId}/permissions`, {
    method: "POST",
    body: { permissions }
  });
}

export async function fetchAdminRequests(page = 0, size = 20): Promise<AdminRequestPage> {
  return request<AdminRequestPage>(`/api/v1/admin/requests?page=${page}&size=${size}`);
}

export async function assignAdminRequest(requestId: number, userId: number): Promise<void> {
  await request(`/api/v1/admin/requests/${requestId}/assign`, {
    method: "POST",
    body: { userId }
  });
}

export async function updateAdminRequestStatus(
  requestId: number,
  status: "NEW" | "IN_REVIEW" | "NEED_INFO" | "APPROVED" | "REJECTED" | "DONE"
): Promise<void> {
  await request(`/api/v1/admin/requests/${requestId}/status`, {
    method: "POST",
    body: { status }
  });
}

export async function fetchAdminTerms(): Promise<AdminTermItem[]> {
  return request<AdminTermItem[]>("/api/v1/admin/terms");
}

export async function fetchAdminWindows(): Promise<AdminWindowItem[]> {
  return request<AdminWindowItem[]>("/api/v1/admin/windows");
}

export async function createAdminTerm(
  name: string,
  startDate: string,
  endDate: string,
  current: boolean
): Promise<void> {
  await request("/api/v1/admin/terms", {
    method: "POST",
    body: { name, startDate, endDate, current }
  });
}

export async function fetchAdminSubjects(): Promise<AdminSimpleSubjectItem[]> {
  return request<AdminSimpleSubjectItem[]>("/api/v1/admin/subjects");
}

export async function fetchAdminSubjectDetail(subjectId: number): Promise<AdminSubjectDetail> {
  return request<AdminSubjectDetail>(`/api/v1/admin/subjects/${subjectId}`);
}

export async function fetchAdminTeachers(): Promise<AdminSimpleTeacherItem[]> {
  return request<AdminSimpleTeacherItem[]>("/api/v1/admin/teachers");
}

export async function fetchAdminTeacherDetail(teacherId: number): Promise<AdminTeacherDetail> {
  return request<AdminTeacherDetail>(`/api/v1/admin/teachers/${teacherId}`);
}

export async function fetchAdminStudents(): Promise<AdminSimpleStudentItem[]> {
  return request<AdminSimpleStudentItem[]>("/api/v1/admin/students");
}

export async function fetchAdminStudentDetail(studentId: number): Promise<AdminStudentDetail> {
  return request<AdminStudentDetail>(`/api/v1/admin/students/${studentId}`);
}

export async function fetchAdminFaculties(): Promise<AdminFacultyItem[]> {
  return request<AdminFacultyItem[]>("/api/v1/admin/faculties");
}

export async function fetchAdminPrograms(facultyId?: number): Promise<AdminProgramItem[]> {
  const suffix = facultyId ? `?facultyId=${facultyId}` : "";
  return request<AdminProgramItem[]>(`/api/v1/admin/programs${suffix}`);
}

export async function createAdminStudent(payload: AdminStudentUpsertPayload): Promise<AdminStudentUpsertResult> {
  return request<AdminStudentUpsertResult>("/api/v1/admin/students", {
    method: "POST",
    body: payload
  });
}

export async function createAdminTeacher(payload: AdminTeacherUpsertPayload): Promise<AdminTeacherUpsertResult> {
  return request<AdminTeacherUpsertResult>("/api/v1/admin/teachers", {
    method: "POST",
    body: payload
  });
}

export async function createAdminSubject(payload: AdminSubjectUpsertPayload): Promise<AdminSubjectUpsertResult> {
  return request<AdminSubjectUpsertResult>("/api/v1/admin/subjects", {
    method: "POST",
    body: payload
  });
}

export async function updateAdminStudent(
  studentId: number,
  payload: AdminStudentUpsertPayload
): Promise<AdminStudentUpsertResult> {
  return request<AdminStudentUpsertResult>(`/api/v1/admin/students/${studentId}`, {
    method: "PUT",
    body: payload
  });
}

export async function updateAdminTeacher(
  teacherId: number,
  payload: AdminTeacherUpsertPayload
): Promise<AdminTeacherUpsertResult> {
  return request<AdminTeacherUpsertResult>(`/api/v1/admin/teachers/${teacherId}`, {
    method: "PUT",
    body: payload
  });
}

export async function updateAdminSubject(
  subjectId: number,
  payload: AdminSubjectUpsertPayload
): Promise<AdminSubjectUpsertResult> {
  return request<AdminSubjectUpsertResult>(`/api/v1/admin/subjects/${subjectId}`, {
    method: "PUT",
    body: payload
  });
}

export async function fetchAdminSections(semesterId?: number): Promise<AdminSectionItem[]> {
  const suffix = semesterId ? `?semesterId=${semesterId}` : "";
  return request<AdminSectionItem[]>(`/api/v1/admin/sections${suffix}`);
}

export async function createAdminSection(
  subjectId: number,
  semesterId: number,
  teacherId: number,
  capacity: number,
  lessonType: "LECTURE" | "PRACTICE" | "LAB"
): Promise<void> {
  await request("/api/v1/admin/sections", {
    method: "POST",
    body: { subjectId, semesterId, teacherId, capacity, lessonType }
  });
}

export async function assignAdminProfessor(sectionId: number, teacherId: number): Promise<void> {
  await request(`/api/v1/admin/sections/${sectionId}/assign-professor`, {
    method: "POST",
    body: { teacherId }
  });
}

export async function addAdminMeetingTime(
  sectionId: number,
  dayOfWeek: string,
  startTime: string,
  endTime: string,
  room: string,
  lessonType: "LECTURE" | "PRACTICE" | "LAB"
): Promise<void> {
  await request(`/api/v1/admin/sections/${sectionId}/meeting-times`, {
    method: "POST",
    body: { dayOfWeek, startTime, endTime, room, lessonType }
  });
}

export async function upsertAdminWindow(
  semesterId: number,
  type: "REGISTRATION" | "ADD_DROP" | "FX" | "GRADE_PUBLISH",
  startDate: string,
  endDate: string,
  active: boolean
): Promise<void> {
  await request("/api/v1/admin/windows", {
    method: "POST",
    body: { semesterId, type, startDate, endDate, active }
  });
}

export async function fetchAdminExams(semesterId?: number): Promise<AdminExamItem[]> {
  const suffix = semesterId ? `?semesterId=${semesterId}` : "";
  return request<AdminExamItem[]>(`/api/v1/admin/exams${suffix}`);
}

export async function fetchAdminFx(): Promise<AdminFxItem[]> {
  return request<AdminFxItem[]>("/api/v1/admin/fx");
}

export async function updateAdminFxStatus(
  fxId: number,
  status: "PENDING" | "APPROVED" | "PAID" | "CONFIRMED"
): Promise<void> {
  await request(`/api/v1/admin/fx/${fxId}/status`, {
    method: "POST",
    body: { status }
  });
}

export async function createAdminExam(
  sectionId: number,
  examDate: string,
  examTime: string,
  room: string,
  format: string
): Promise<void> {
  await request("/api/v1/admin/exams", {
    method: "POST",
    body: { sectionId, examDate, examTime, room, format }
  });
}

export async function updateAdminExam(
  examId: number,
  sectionId: number,
  examDate: string,
  examTime: string,
  room: string,
  format: string
): Promise<void> {
  await request(`/api/v1/admin/exams/${examId}`, {
    method: "PUT",
    body: { sectionId, examDate, examTime, room, format }
  });
}

export async function deleteAdminExam(examId: number): Promise<void> {
  await request(`/api/v1/admin/exams/${examId}`, { method: "DELETE" });
}

export async function fetchAdminHolds(): Promise<AdminHoldItem[]> {
  return request<AdminHoldItem[]>("/api/v1/admin/holds");
}

export async function fetchAdminNotifications(): Promise<AdminNotificationCenterData> {
  return request<AdminNotificationCenterData>("/api/v1/admin/notifications");
}

export async function fetchAdminAnalytics(): Promise<AdminAnalyticsDashboard> {
  return request<AdminAnalyticsDashboard>("/api/v1/admin/analytics");
}

export async function fetchAdminWorkflows(): Promise<AdminWorkflowOverview> {
  return request<AdminWorkflowOverview>("/api/v1/admin/workflows");
}

export async function fetchAdminWorkflowTimeline(type: WorkflowType, id: number): Promise<WorkflowTimeline> {
  return request<WorkflowTimeline>(`/api/v1/admin/workflows/${type}/${id}/timeline`);
}

export async function askAdminAssistant(message: string): Promise<AdminAssistantReply> {
  return request<AdminAssistantReply>("/api/v1/admin/assistant/chat", {
    method: "POST",
    body: { message }
  });
}

export async function markAdminNotificationRead(id: number): Promise<void> {
  await request(`/api/v1/admin/notifications/${id}/read`, { method: "POST" });
}

export async function markAllAdminNotificationsRead(): Promise<void> {
  await request("/api/v1/admin/notifications/read-all", { method: "POST" });
}

export async function createAdminHold(
  studentId: number,
  type: "FINANCIAL" | "ACADEMIC" | "DISCIPLINARY" | "MANUAL",
  reason: string
): Promise<void> {
  await request("/api/v1/admin/holds", {
    method: "POST",
    body: { studentId, type, reason }
  });
}

export async function removeAdminHold(holdId: number, removalReason: string): Promise<void> {
  await request(`/api/v1/admin/holds/${holdId}/remove`, {
    method: "POST",
    body: { removalReason }
  });
}

export async function createAdminInvoice(
  studentId: number,
  amount: number,
  description: string,
  dueDate: string
): Promise<void> {
  await request("/api/v1/admin/finance/invoices", {
    method: "POST",
    body: { studentId, amount, description, dueDate }
  });
}

export async function createAdminPayment(
  studentId: number,
  chargeId: number,
  amount: number,
  date: string
): Promise<void> {
  await request("/api/v1/admin/finance/payments", {
    method: "POST",
    body: { studentId, chargeId, amount, date }
  });
}

export async function fetchAdminGradeChangeRequests(
  page = 0,
  size = 20
): Promise<ApiPageResponse<AdminGradeChangeItem>> {
  return request<ApiPageResponse<AdminGradeChangeItem>>(
    `/api/v1/admin/grade-change-requests?page=${page}&size=${size}`
  );
}

export async function reviewAdminGradeChangeRequest(
  requestId: number,
  approve: boolean,
  comment: string
): Promise<void> {
  await request(`/api/v1/admin/grade-change-requests/${requestId}/review`, {
    method: "POST",
    body: { approve, comment }
  });
}

export async function updateAdminStudentStatus(
  studentId: number,
  status: "ACTIVE" | "ON_LEAVE" | "GRADUATED"
): Promise<void> {
  await request(`/api/v1/admin/students/${studentId}/status`, {
    method: "POST",
    body: { status }
  });
}

export async function createAdminNews(title: string, content: string, category: string): Promise<void> {
  await request("/api/v1/admin/news", {
    method: "POST",
    body: { title, content, category }
  });
}

export async function fetchPublicNews(): Promise<PublicNewsItem[]> {
  return request<PublicNewsItem[]>("/api/v1/public/news", { auth: false });
}

export async function fetchPublicProfessors(): Promise<PublicProfessorListItem[]> {
  return request<PublicProfessorListItem[]>("/api/v1/public/professors", { auth: false });
}

export async function fetchPublicProfessorById(id: number): Promise<PublicProfessorProfile> {
  return request<PublicProfessorProfile>(`/api/v1/public/professors/${id}`, { auth: false });
}

export function buildFileDownloadUrl(path: string): string {
  return buildApiUrl(path);
}

// ─── Dorm API ───────────────────────────────────────────

import type { DormBuilding, DormRoom, DormApplication } from "../types/dorm";

export async function fetchDormBuildings(): Promise<DormBuilding[]> {
  return request<DormBuilding[]>("/api/v1/student/dorm/buildings");
}

export async function fetchDormRooms(roomType?: string): Promise<DormRoom[]> {
  const suffix = roomType ? `?roomType=${roomType}` : "";
  return request<DormRoom[]>(`/api/v1/student/dorm/rooms${suffix}`);
}

export async function fetchDormApplications(): Promise<DormApplication[]> {
  return request<DormApplication[]>("/api/v1/student/dorm/applications");
}

export async function fetchDormApplication(id: number): Promise<DormApplication> {
  return request<DormApplication>(`/api/v1/student/dorm/applications/${id}`);
}

export async function createDormApplication(): Promise<DormApplication> {
  return request<DormApplication>("/api/v1/student/dorm/applications", { method: "POST" });
}

export async function updateDormStep1(
  id: number,
  body: { emergencyContactName: string; emergencyContactPhone: string; specialNeeds: string }
): Promise<DormApplication> {
  return request<DormApplication>(`/api/v1/student/dorm/applications/${id}/step1`, {
    method: "PUT",
    body
  });
}

export async function updateDormStep2(
  id: number,
  body: { roomTypePreference: string; dormRoomId: number | null }
): Promise<DormApplication> {
  return request<DormApplication>(`/api/v1/student/dorm/applications/${id}/step2`, {
    method: "PUT",
    body
  });
}

export async function updateDormStep3(
  id: number,
  body: { sleepSchedule: string; studyEnvironment: string; preferredRoommateUid: string }
): Promise<DormApplication> {
  return request<DormApplication>(`/api/v1/student/dorm/applications/${id}/step3`, {
    method: "PUT",
    body
  });
}

export async function submitDormApplication(id: number, termsAccepted: boolean): Promise<DormApplication> {
  return request<DormApplication>(`/api/v1/student/dorm/applications/${id}/submit`, {
    method: "POST",
    body: { termsAccepted }
  });
}

export async function cancelDormApplication(id: number): Promise<DormApplication> {
  return request<DormApplication>(`/api/v1/student/dorm/applications/${id}/cancel`, { method: "POST" });
}

// ─── Food API ───────────────────────────────────────────

import type { FoodCategory, FoodItem, FoodOrder } from "../types/food";

export async function fetchFoodCategories(): Promise<FoodCategory[]> {
  return request<FoodCategory[]>("/api/v1/student/food/categories");
}

export async function fetchFoodItems(categoryId?: number): Promise<FoodItem[]> {
  const suffix = categoryId ? `?categoryId=${categoryId}` : "";
  return request<FoodItem[]>(`/api/v1/student/food/items${suffix}`);
}

export async function fetchPopularFoodItems(): Promise<FoodItem[]> {
  return request<FoodItem[]>("/api/v1/student/food/items/popular");
}

export async function fetchFoodOrders(): Promise<FoodOrder[]> {
  return request<FoodOrder[]>("/api/v1/student/food/orders");
}

export async function createFoodOrder(
  items: Record<number, number>,
  note?: string,
  pickupTime?: string
): Promise<FoodOrder> {
  return request<FoodOrder>("/api/v1/student/food/orders", {
    method: "POST",
    body: { items, note, pickupTime }
  });
}

export async function cancelFoodOrder(id: number): Promise<FoodOrder> {
  return request<FoodOrder>(`/api/v1/student/food/orders/${id}/cancel`, { method: "POST" });
}

// ─── Campus Map API ─────────────────────────────────────

import type { CampusBuilding, CampusRoom, NavigationResult } from "../types/campus";

export async function fetchCampusBuildings(type?: string): Promise<CampusBuilding[]> {
  const suffix = type ? `?type=${type}` : "";
  return request<CampusBuilding[]>(`/api/v1/student/campus-map/buildings${suffix}`);
}

export async function fetchCampusBuilding(id: number): Promise<CampusBuilding> {
  return request<CampusBuilding>(`/api/v1/student/campus-map/buildings/${id}`);
}

export async function searchCampusBuildings(q: string): Promise<CampusBuilding[]> {
  return request<CampusBuilding[]>(`/api/v1/student/campus-map/buildings/search?q=${encodeURIComponent(q)}`);
}

export async function fetchCampusRoomsByBuilding(buildingId: number): Promise<CampusRoom[]> {
  return request<CampusRoom[]>(`/api/v1/student/campus-map/buildings/${buildingId}/rooms`);
}

export async function searchCampusRooms(q: string): Promise<CampusRoom[]> {
  return request<CampusRoom[]>(`/api/v1/student/campus-map/rooms/search?q=${encodeURIComponent(q)}`);
}

export async function navigateCampus(fromRoomId: number, toRoomId: number): Promise<NavigationResult> {
  return request<NavigationResult>(
    `/api/v1/student/campus-map/navigate?fromRoomId=${fromRoomId}&toRoomId=${toRoomId}`
  );
}

// ─── Laundry API ────────────────────────────────────────

import type { LaundryRoom as LaundryRoomType, LaundryMachine, LaundryRoomAvailability, LaundryBooking } from "../types/laundry";

export async function fetchLaundryRooms(): Promise<LaundryRoomType[]> {
  return request<LaundryRoomType[]>("/api/v1/student/laundry/rooms");
}

export async function fetchLaundryRoomAvailability(roomId: number): Promise<LaundryRoomAvailability> {
  return request<LaundryRoomAvailability>(`/api/v1/student/laundry/rooms/${roomId}/availability`);
}

export async function fetchLaundryMachines(roomId: number): Promise<LaundryMachine[]> {
  return request<LaundryMachine[]>(`/api/v1/student/laundry/rooms/${roomId}/machines`);
}

export async function fetchLaundryBookings(): Promise<LaundryBooking[]> {
  return request<LaundryBooking[]>("/api/v1/student/laundry/bookings");
}

export async function createLaundryBooking(
  machineId: number,
  startTime: string,
  durationMinutes: number
): Promise<LaundryBooking> {
  return request<LaundryBooking>("/api/v1/student/laundry/bookings", {
    method: "POST",
    body: { machineId, startTime, durationMinutes }
  });
}

export async function cancelLaundryBooking(id: number): Promise<LaundryBooking> {
  return request<LaundryBooking>(`/api/v1/student/laundry/bookings/${id}/cancel`, { method: "POST" });
}

// ─── Chat API ────────────────────────────────────────────

import type { ChatRoom, ChatMessagePage, ChatMember, ChatUserResult } from "../types/chat";

export async function fetchChatRooms(): Promise<ChatRoom[]> {
  return request<ChatRoom[]>("/api/v1/chat/rooms");
}

export async function getOrCreateSectionRoom(sectionId: number): Promise<ChatRoom> {
  return request<ChatRoom>(`/api/v1/chat/rooms/section/${sectionId}`, { method: "POST" });
}

export async function getOrCreateDirectRoom(userId: number): Promise<ChatRoom> {
  return request<ChatRoom>("/api/v1/chat/rooms/direct", {
    method: "POST",
    body: { userId }
  });
}

export async function createGroupRoom(name: string, userIds: number[]): Promise<ChatRoom> {
  return request<ChatRoom>("/api/v1/chat/rooms/group", {
    method: "POST",
    body: { name, userIds }
  });
}

export async function searchChatUsers(q: string): Promise<ChatUserResult[]> {
  const qs = q ? `?q=${encodeURIComponent(q)}` : "";
  return request<ChatUserResult[]>(`/api/v1/chat/users${qs}`);
}

export async function fetchChatMessages(roomId: number, page = 0, size = 50): Promise<ChatMessagePage> {
  return request<ChatMessagePage>(`/api/v1/chat/rooms/${roomId}/messages?page=${page}&size=${size}`);
}

export async function fetchChatMembers(roomId: number): Promise<ChatMember[]> {
  return request<ChatMember[]>(`/api/v1/chat/rooms/${roomId}/members`);
}

export async function downloadTeacherSectionGrades(sectionId: number): Promise<{ blob: Blob; fileName: string }> {
  const token = getAccessToken();
  const response = await fetch(buildApiUrl(`/api/v1/teacher/sections/${sectionId}/grades/export`), {
    method: "GET",
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthSession();
    }
    throw new ApiError(`Export failed with status ${response.status}`, response.status);
  }

  const disposition = response.headers.get("content-disposition") || "";
  const fileNameMatch = disposition.match(/filename="?([^"]+)"?/i);
  const fileName = fileNameMatch?.[1] || `grades_section_${sectionId}.xlsx`;
  const blob = await response.blob();
  return { blob, fileName };
}
