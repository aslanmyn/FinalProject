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
  profilePhotoUrl: string | null;
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
  semesterId: number | null;
  semesterName: string | null;
  academicYear: string | null;
  season: string | null;
  lessonType: string | null;
}

export interface StudentScheduleSemesterOption {
  id: number;
  name: string;
  academicYear: string;
  season: string;
  current: boolean;
}

export interface StudentScheduleOptions {
  currentSemesterId: number | null;
  semesters: StudentScheduleSemesterOption[];
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
  teacherName: string | null;
  credits: number | null;
  semesterId: number | null;
  semesterName: string | null;
  academicYear: string | null;
  season: string | null;
  status: string;
  createdAt: string;
}

export interface StudentEnrollmentOptions {
  currentSemesterId: number | null;
  semesters: StudentScheduleSemesterOption[];
}

export interface StudentJournalItem {
  sectionId: number;
  courseCode: string;
  courseName: string;
  semesterId: number | null;
  semesterName: string | null;
  academicYear: string | null;
  season: string | null;
  attestation1: number | null;
  attestation1Max: number | null;
  attestation2: number | null;
  attestation2Max: number | null;
  finalExam: number | null;
  finalExamMax: number | null;
  totalScore: number | null;
  letterValue: string | null;
}

export interface StudentJournalOptions {
  currentSemesterId: number | null;
  semesters: StudentScheduleSemesterOption[];
}

export interface StudentAssistantReply {
  answer: string;
  model: string;
  generatedAt: string;
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
