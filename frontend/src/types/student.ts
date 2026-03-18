import type { ApiPageResponse, NotificationCenterData, WorkflowOverview } from "./common";

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

export interface StudentRiskCourse {
  sectionId: number;
  courseCode: string;
  courseName: string;
  teacherName: string | null;
  credits: number;
  attestation1: number;
  attestation2: number;
  finalExam: number | null;
  totalScore: number | null;
  attendanceRate: number;
  present: number;
  late: number;
  absent: number;
  level: string;
  riskScore: number;
  reasons: string[];
  neededForPass: number | null;
  neededForB: number | null;
  neededForA: number | null;
}

export interface StudentRiskDashboard {
  studentId: number;
  studentName: string;
  facultyName: string | null;
  semesterName: string;
  level: string;
  riskScore: number;
  publishedGpa: number;
  attendanceRate: number;
  hasFinancialHold: boolean;
  activeHolds: number;
  overdueCharges: number;
  openRequests: number;
  reasons: string[];
  courses: StudentRiskCourse[];
}

export interface StudentPlannerCourse {
  sectionId: number;
  courseCode: string;
  courseName: string;
  teacherName: string | null;
  credits: number;
  attestation1: number;
  attestation2: number;
  publishedFinal: number | null;
  publishedTotal: number | null;
  publishedLetter: string | null;
  subtotal: number;
  maxTotal: number;
  neededForPass: number | null;
  neededForB: number | null;
  neededForA: number | null;
}

export interface StudentPlannerDashboard {
  studentId: number;
  studentName: string;
  semesterId: number | null;
  semesterName: string | null;
  currentPublishedGpa: number;
  publishedFinalCount: number;
  maxProjectionGpa: number;
  courses: StudentPlannerCourse[];
}

export interface StudentPlannerSimulationCourse {
  sectionId: number;
  courseCode: string;
  courseName: string;
  teacherName: string | null;
  credits: number;
  attestation1: number;
  attestation2: number;
  publishedFinal: number | null;
  projectedFinal: number;
  projectedTotal: number;
  projectedLetter: string;
  projectedPoints: number;
}

export interface StudentPlannerSimulation {
  currentPublishedGpa: number;
  projectedTermGpa: number;
  projectedOverallGpa: number;
  courses: StudentPlannerSimulationCourse[];
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

export interface StudentHoldItem {
  id: number;
  type: string;
  reason: string;
  createdAt: string;
}

export interface StudentRegistrationMeetingSlot {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string | null;
  lessonType: string | null;
}

export interface StudentWindowStatusItem {
  id: number;
  type: string;
  startDate: string;
  endDate: string;
  active: boolean;
  openNow: boolean;
}

export interface StudentRegistrationBoardItem {
  registrationId: number;
  sectionId: number;
  subjectCode: string;
  subjectName: string;
  teacherName: string | null;
  credits: number;
  status: string;
  canDrop: boolean;
  dropBlockedReasons: string[];
  meetingTimes: StudentRegistrationMeetingSlot[];
}

export interface StudentRegistrationOverview {
  currentSemesterId: number | null;
  currentSemesterName: string | null;
  currentCredits: number;
  creditLimit: number | null;
  hasRegistrationHold: boolean;
  holds: StudentHoldItem[];
  windows: StudentWindowStatusItem[];
  currentRegistrations: StudentRegistrationBoardItem[];
  eligibleFxCount: number;
  fxRequestCount: number;
}

export interface StudentCourseCatalogItem {
  sectionId: number;
  subjectCode: string;
  subjectName: string;
  credits: number;
  semesterId: number | null;
  semesterName: string | null;
  academicYear: string | null;
  season: string | null;
  teacherId: number | null;
  teacherName: string | null;
  capacity: number;
  occupiedSeats: number;
  lessonType: string | null;
  meetingTimes: StudentRegistrationMeetingSlot[];
  registrationStatus: string | null;
  canRegister: boolean;
  registrationBlockedReasons: string[];
  canAddDrop: boolean;
  addDropBlockedReasons: string[];
  canDrop: boolean;
  dropBlockedReasons: string[];
}

export interface StudentFxEligibleCourse {
  sectionId: number;
  subjectCode: string;
  subjectName: string;
  finalScore: number;
  alreadyRequested: boolean;
}

export interface StudentFxRegistrationItem {
  id: number;
  sectionId: number | null;
  subjectCode: string | null;
  subjectName: string | null;
  status: string;
  createdAt: string;
}

export interface StudentFxOverview {
  windowOpen: boolean;
  eligibleCourses: StudentFxEligibleCourse[];
  registrations: StudentFxRegistrationItem[];
}

export type StudentNotificationCenterData = NotificationCenterData;
export type StudentWorkflowOverview = WorkflowOverview;

export interface StudentActionResult {
  success: boolean;
  message: string;
  errors: string[];
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
