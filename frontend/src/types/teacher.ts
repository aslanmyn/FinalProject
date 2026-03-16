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
  semesterName: string;
  capacity: number;
  lessonType: string;
}

export interface TeacherRosterItem {
  registrationId: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  status: string;
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
