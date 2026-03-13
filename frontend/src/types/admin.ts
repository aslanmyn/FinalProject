import type { ApiPageResponse } from "./common";

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

export interface AdminSectionItem {
  id: number;
  capacity: number;
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
