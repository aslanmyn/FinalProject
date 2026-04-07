import type { ApiPageResponse, NotificationCenterData, WorkflowOverview } from "./common";

export interface AdminStats {
  adminId: number;
  students: number;
  teachers: number;
  sections: number;
  requests: number;
  activeHolds: number;
}

export interface AdminUserItem {
  id: number;
  email: string;
  fullName: string;
  role: string;
  permissions: string[];
  enabled: boolean;
}

export type AdminUserPage = ApiPageResponse<AdminUserItem>;

export interface AdminRequestItem {
  id: number;
  category: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  assignedToUserId: number | null;
}

export type AdminRequestPage = ApiPageResponse<AdminRequestItem>;

export interface AdminTermItem {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  current: boolean;
}

export interface AdminSimpleSubjectItem {
  id: number;
  code: string;
  name: string;
  credits: number;
}

export interface AdminSimpleTeacherItem {
  id: number;
  name: string;
  email: string;
}

export interface AdminSimpleStudentItem {
  id: number;
  name: string;
  email: string;
  status: string;
}

export interface AdminFacultyItem {
  id: number;
  name: string;
}

export interface AdminProgramItem {
  id: number;
  name: string;
  creditLimit: number;
  facultyId: number | null;
  facultyName: string | null;
}

export interface AdminStudentDetail {
  userId: number;
  studentId: number;
  email: string;
  fullName: string;
  course: number;
  groupName: string | null;
  status: string;
  facultyId: number | null;
  facultyName: string | null;
  programId: number | null;
  programName: string | null;
  currentSemesterId: number | null;
  currentSemesterName: string | null;
  creditsEarned: number;
  passportNumber: string | null;
  address: string | null;
  phone: string | null;
  emergencyContact: string | null;
  enabled: boolean;
}

export interface AdminStudentUpsertPayload {
  email: string;
  password: string;
  fullName: string;
  facultyId: number;
  programId: number;
  currentSemesterId: number;
  course: number;
  groupName: string;
  status: "ACTIVE" | "ON_LEAVE" | "GRADUATED";
  creditsEarned: number;
  passportNumber: string;
  address: string;
  phone: string;
  emergencyContact: string;
  enabled: boolean;
}

export interface AdminStudentUpsertResult {
  userId: number;
  studentId: number;
  email: string;
  fullName: string;
  course: number;
  status: string;
  facultyId: number;
  facultyName: string;
  programId: number;
  programName: string;
  currentSemesterId: number;
  currentSemesterName: string;
  enabled: boolean;
}

export interface AdminSectionItem {
  id: number;
  capacity: number;
  meetingTimes?: AdminSectionMeetingTimeItem[];
  dayOfWeek?: string;
  startTime?: string;
  endTime?: string;
  room?: string;
  lessonType?: string;
  subject?: {
    id: number;
    code: string;
    name: string;
  };
  semester?: {
    id: number;
    name: string;
  };
  teacher?: {
    id: number;
    name: string;
    email?: string;
  };
}

export interface AdminSectionMeetingTimeItem {
  id: number | null;
  sectionId: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string | null;
  lessonType: string | null;
}

export interface AdminExamItem {
  id: number;
  examDate: string;
  examTime: string;
  room: string;
  format: string;
  subjectOffering?: {
    id: number;
    subject?: {
      code: string;
      name: string;
    };
  };
}

export interface AdminHoldItem {
  id: number;
  studentId: number;
  studentName: string;
  type: string;
  reason: string;
  createdAt: string;
}

export interface AdminGradeChangeItem {
  id: number;
  teacherId: number;
  studentId: number;
  sectionId: number;
  oldValue?: number;
  newValue?: number;
  reason: string;
  status: string;
  createdAt: string;
}

export interface AdminWindowItem {
  id: number;
  semesterId: number | null;
  semesterName: string | null;
  type: string;
  startDate: string;
  endDate: string;
  active: boolean;
}

export interface AdminFxItem {
  id: number;
  studentId: number | null;
  studentName: string | null;
  sectionId: number | null;
  subjectCode: string | null;
  subjectName: string | null;
  status: string;
  createdAt: string;
}

export type AdminNotificationCenterData = NotificationCenterData;
export type AdminWorkflowOverview = WorkflowOverview;

export interface AdminAnalyticsMetrics {
  students: number;
  teachers: number;
  currentSections: number;
  requests: number;
  activeHolds: number;
  openWindows: number;
}

export interface AdminFacultyRiskItem {
  facultyName: string;
  studentCount: number;
  atRiskStudents: number;
  mediumRiskStudents: number;
  averageRisk: number;
  averageAttendance: number;
  studentsWithFinancialHolds: number;
}

export interface AdminOverloadedSectionItem {
  sectionId: number;
  courseCode: string;
  courseName: string;
  semesterName: string;
  teacherName: string;
  facultyName: string;
  capacity: number;
  enrolledCount: number;
  utilizationPercent: number;
}

export interface AdminRequestLoadItem {
  category: string;
  count: number;
}

export interface AdminWorkflowSummaryItem {
  workflowType: string;
  count: number;
}

export interface AdminCriticalStudentItem {
  studentId: number;
  studentName: string;
  facultyName: string | null;
  level: string;
  riskScore: number;
  primaryReason: string;
}

export interface AdminAnalyticsDashboard {
  metrics: AdminAnalyticsMetrics;
  facultyRisks: AdminFacultyRiskItem[];
  overloadedSections: AdminOverloadedSectionItem[];
  requestLoads: AdminRequestLoadItem[];
  workflowSummary: AdminWorkflowSummaryItem[];
  criticalStudents: AdminCriticalStudentItem[];
}

export interface AdminAssistantReply {
  answer: string;
  model: string;
  generatedAt: string;
}
