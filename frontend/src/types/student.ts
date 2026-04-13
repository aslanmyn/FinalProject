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
  address: string | null;
  emergencyContact: string | null;
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

export interface StudentSectionTeacher {
  teacherId: number;
  teacherName: string;
  teacherEmail: string | null;
}

export interface StudentSectionMeetingSlot {
  dayOfWeek: string | null;
  startTime: string | null;
  endTime: string | null;
  room: string | null;
  lessonType: string | null;
}

export interface StudentSectionScoreSummary {
  attestation1: number | null;
  attestation1Max: number | null;
  attestation2: number | null;
  attestation2Max: number | null;
  finalExam: number | null;
  finalExamMax: number | null;
  totalScore: number | null;
  letterValue: string | null;
  points: number | null;
}

export interface StudentSectionComponentGrade {
  componentId: number | null;
  componentName: string | null;
  componentType: string | null;
  weightPercent: number | null;
  componentPublished: boolean;
  componentLocked: boolean;
  gradeId: number | null;
  gradeType: string | null;
  gradeValue: number | null;
  maxGradeValue: number | null;
  comment: string | null;
  createdAt: string | null;
}

export interface StudentSectionFinalGrade {
  id: number;
  numericValue: number;
  letterValue: string | null;
  points: number;
  status: string;
  publishedAt: string | null;
  updatedAt: string | null;
}

export interface StudentSectionAttendanceSummary {
  present: number;
  late: number;
  absent: number;
  total: number;
  percentage: number;
}

export interface StudentSectionAttendanceRecord {
  attendanceId: number;
  date: string;
  status: string;
  reason: string | null;
  markedBy: string | null;
  teacherConfirmed: boolean;
  markedAt: string | null;
  updatedAt: string | null;
  sessionId: number | null;
  sessionDate: string | null;
}

export interface StudentSectionExam {
  id: number;
  examDate: string;
  examTime: string;
  room: string | null;
  format: string | null;
}

export interface StudentSectionAnnouncement {
  id: number;
  title: string;
  content: string;
  teacherName: string | null;
  publishedAt: string | null;
  pinned: boolean;
}

export interface StudentSectionMaterial {
  id: number;
  title: string;
  description: string | null;
  originalFileName: string;
  contentType: string | null;
  sizeBytes: number;
  createdAt: string | null;
  downloadUrl: string;
}

export interface StudentSectionDetail {
  registrationId: number;
  sectionId: number;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  credits: number;
  registrationStatus: string;
  activeCourseAccess: boolean;
  contentBlockedReason: string | null;
  semesterId: number | null;
  semesterName: string | null;
  academicYear: string | null;
  season: string | null;
  teacher: StudentSectionTeacher | null;
  capacity: number;
  occupiedSeats: number;
  lessonType: string | null;
  meetingTimes: StudentSectionMeetingSlot[];
  scoreSummary: StudentSectionScoreSummary;
  componentGrades: StudentSectionComponentGrade[];
  finalGrade: StudentSectionFinalGrade | null;
  attendanceSummary: StudentSectionAttendanceSummary;
  attendanceRecords: StudentSectionAttendanceRecord[];
  activeAttendanceSessions: StudentAttendanceActiveSessionItem[];
  exam: StudentSectionExam | null;
  announcements: StudentSectionAnnouncement[];
  materials: StudentSectionMaterial[];
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
  scheduleRecommendation?: StudentAssistantScheduleRecommendation | null;
}

export interface StudentAssistantScheduleMeetingTime {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string | null;
}

export interface StudentAssistantSelectedSection {
  courseCode: string;
  courseName: string | null;
  sectionId: number;
  teacherName: string | null;
  meetingTimes: StudentAssistantScheduleMeetingTime[];
}

export interface StudentAssistantVisualScheduleItem {
  courseCode: string;
  courseName: string | null;
  startTime: string;
  endTime: string;
  room: string | null;
  teacherName: string | null;
}

export interface StudentAssistantScheduleRecommendation {
  semesterName: string | null;
  feasible: boolean;
  partial: boolean;
  summary: string | null;
  satisfiedPreferences: string[];
  unsatisfiedPreferences: string[];
  blockingCourses: string[];
  selectedSections: StudentAssistantSelectedSection[];
  warnings: string[];
  visualSchedule: Record<string, StudentAssistantVisualScheduleItem[]>;
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

export interface StudentAttendanceActiveSessionItem {
  sessionId: number;
  sectionId: number;
  subjectCode: string;
  subjectName: string;
  teacherName: string | null;
  classDate: string | null;
  attendanceCloseAt: string | null;
  checkInMode: "ONE_CLICK" | "CODE";
  currentStatus: string | null;
  markedBy: string | null;
  teacherConfirmed: boolean;
  registrationStatus: string;
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
  activeSessions: StudentAttendanceActiveSessionItem[];
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

export interface StudentNextSemesterSectionOption {
  sectionId: number;
  teacherName: string | null;
  capacity: number;
  occupiedSeats: number;
  meetingTimes: StudentRegistrationMeetingSlot[];
  selected: boolean;
  blockedReasons: string[];
  canSelect: boolean;
}

export interface StudentNextSemesterSubjectOption {
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  credits: number;
  required: boolean;
  displayOrder: number;
  saved: boolean;
  selectedSectionId: number | null;
  sections: StudentNextSemesterSectionOption[];
}

export interface StudentNextSemesterSavedSubject {
  plannedRegistrationId: number;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  credits: number;
  selectedSectionId: number | null;
  teacherName: string | null;
  meetingTimes: StudentRegistrationMeetingSlot[];
  sectionSelected: boolean;
}

export interface StudentNextSemesterOverview {
  semesterId: number | null;
  semesterName: string | null;
  academicYear: number | null;
  semesterNumber: number | null;
  selectionEnabled: boolean;
  maxSelections: number;
  selectedCount: number;
  message: string;
  subjects: StudentNextSemesterSubjectOption[];
  savedSubjects: StudentNextSemesterSavedSubject[];
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
