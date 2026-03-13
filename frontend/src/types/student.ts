import type { ApiPageResponse } from "./common";

export interface StudentProfile {
  id: number;
  name: string;
  email: string;
  course: number;
  groupName: string;
  status: string;
  program: string | null;
  faculty: string | null;
  creditsEarned: number;
  phone: string | null;
}

export interface StudentScheduleItem {
  sectionId: number;
  courseCode: string;
  courseName: string;
  dayOfWeek: string | null;
  startTime: string | null;
  endTime: string | null;
  room: string;
  teacherName: string | null;
  status: string;
}

export interface StudentRequestItem {
  id: number;
  category: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export type StudentRequestPage = ApiPageResponse<StudentRequestItem>;

export interface StudentEnrollmentItem {
  id: number;
  sectionId: number;
  subjectCode: string;
  subjectName: string;
  status: string;
  createdAt: string;
}

export interface StudentJournalItem {
  id: number;
  courseCode: string;
  courseName: string;
  component: string;
  value: number;
  max: number;
  comment: string;
  createdAt: string;
}

export interface StudentTranscriptGradeItem {
  id: number;
  courseCode: string;
  courseName: string;
  credits: number;
  numericValue: number;
  letterValue: string;
  points: number;
  status: string;
}

export interface StudentTranscriptData {
  studentId: number;
  studentName: string;
  gpa: number;
  totalCredits: number;
  finalGrades: StudentTranscriptGradeItem[];
}

export interface StudentAttendanceRecordItem {
  date: string;
  subjectCode: string;
  subjectName: string;
  status: string;
  reason: string;
}

export interface StudentAttendanceSummary {
  present: number;
  late: number;
  absent: number;
  total: number;
  percentage: number;
}

export interface StudentAttendanceData {
  records: StudentAttendanceRecordItem[];
  summary: StudentAttendanceSummary;
}

export interface StudentExamScheduleItem {
  id: number;
  subjectCode: string;
  subjectName: string;
  examDate: string;
  examTime: string;
  room: string;
  format: string;
}

export interface StudentNewsItem {
  id: number;
  title: string;
  content: string;
  category: string;
  createdAt: string;
}

export interface StudentChargeItem {
  id: number;
  amount: number;
  description: string;
  dueDate: string;
  status: string;
}

export interface StudentPaymentItem {
  id: number;
  amount: number;
  date: string;
}

export interface StudentFinancialData {
  charges: StudentChargeItem[];
  payments: StudentPaymentItem[];
  balance: number;
  hasFinancialHold: boolean;
}

export interface StudentFileItem {
  id: number;
  fileName: string;
  category: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
  downloadUrl: string;
}
