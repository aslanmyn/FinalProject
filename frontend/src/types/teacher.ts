import type { NotificationCenterData } from "./common";

export interface TeacherProfile {
  id: number;
  name: string;
  email: string;
  department: string;
  position: string;
  officeHours: string;
  officeRoom: string;
  teacherRole: string;
  faculty: string;
  profilePhotoUrl: string | null;
}

export interface TeacherSectionItem {
  id: number;
  subjectCode: string;
  subjectName: string;
  credits: number;
  programName: string | null;
  facultyName: string | null;
  semesterId: number | null;
  semesterName: string;
  currentSemester: boolean;
  capacity: number;
  enrolledCount: number;
  lessonType: string;
  meetingTimes: TeacherSectionMeetingTimeItem[];
}

export interface TeacherSectionMeetingTimeItem {
  id: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string | null;
  lessonType: string;
}

export interface TeacherRosterItem {
  registrationId: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  status: string;
}

export interface TeacherAttendanceSessionItem {
  id: number;
  sectionId: number;
  subjectCode: string;
  subjectName: string;
  classDate: string;
  status: "DRAFT" | "OPEN" | "CLOSED";
  checkInMode: "ONE_CLICK" | "CODE";
  allowTeacherOverride: boolean;
  locked: boolean;
  attendanceCloseAt: string | null;
  openedAt: string | null;
  closedAt: string | null;
  checkInCode: string | null;
}

export interface TeacherAttendanceRecordItem {
  studentId: number;
  studentName: string;
  studentEmail: string;
  attendanceId: number | null;
  status: "PRESENT" | "LATE" | "ABSENT" | null;
  reason: string | null;
  markedBy: "STUDENT" | "TEACHER" | "SYSTEM" | null;
  teacherConfirmed: boolean;
  markedAt: string | null;
  updatedAt: string | null;
}

export interface TeacherActiveAttendancePayload {
  session: TeacherAttendanceSessionItem | null;
  records: TeacherAttendanceRecordItem[];
}

export interface TeacherComponentItem {
  id: number;
  name: string;
  type: string;
  weightPercent: number;
  status: string;
  published: boolean;
  locked: boolean;
}

export interface TeacherStudentFileItem {
  id: number;
  studentId: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
  downloadUrl: string;
}

export interface TeacherAnnouncementItem {
  id: number;
  title: string;
  content: string;
  pinned?: boolean;
  publishedAt?: string;
}

export interface TeacherMaterialItem {
  id: number;
  title: string;
  description: string;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  visibility: string;
  published: boolean;
  createdAt: string;
  downloadUrl: string;
}

export interface TeacherNoteItem {
  id: number;
  note: string;
  riskFlag: string;
  createdAt: string;
  updatedAt?: string;
  student?: {
    id: number;
    name: string;
    email?: string;
  };
}

export interface TeacherGradeChangeRequestItem {
  id: number;
  reason: string;
  status: string;
  oldValue?: number;
  newValue?: number;
  createdAt: string;
}

export interface TeacherAssistantReply {
  answer: string;
  model: string;
  generatedAt: string;
}

export interface TeacherRiskMeetingTimeItem {
  dayOfWeek: string;
  startTime: string | null;
  endTime: string | null;
  room: string | null;
  lessonType: string | null;
}

export interface TeacherSectionRiskItem {
  sectionId: number;
  courseCode: string;
  courseName: string;
  semesterName: string;
  capacity: number;
  enrolledCount: number;
  attendanceRate: number;
  atRiskStudents: number;
  pendingGradeChanges: number;
  unpublishedFinals: number;
  level: string;
  riskScore: number;
  reasons: string[];
  meetingTimes: TeacherRiskMeetingTimeItem[];
}

export interface TeacherRiskStudentItem {
  studentId: number;
  studentName: string;
  studentEmail: string;
  sectionId: number;
  courseCode: string;
  courseName: string;
  level: string;
  riskScore: number;
  reasons: string[];
  attendanceRate: number;
  attestationSubtotal: number;
  finalTotal: number | null;
}

export interface TeacherRiskDashboard {
  teacherId: number;
  teacherName: string;
  totalSections: number;
  currentSections: number;
  atRiskStudents: number;
  sectionsNeedingAttention: number;
  pendingGradeChanges: number;
  unpublishedFinals: number;
  sections: TeacherSectionRiskItem[];
  students: TeacherRiskStudentItem[];
}

export type TeacherNotificationCenterData = NotificationCenterData;
