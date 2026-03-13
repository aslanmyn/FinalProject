export interface PublicNewsItem {
  id: number;
  title: string;
  content: string;
  category: string;
  createdAt: string;
}

export interface PublicProfessorListItem {
  id: number;
  name: string;
  department: string;
  positionTitle: string;
  photoUrl: string;
  publicEmail: string;
  officeRoom: string;
  officeHours: string;
  faculty: string;
  role: "TEACHER" | "TA";
}

export interface PublicProfessorSection {
  id: number;
  subjectCode: string;
  subjectName: string;
  semesterName: string;
  lessonType: "LECTURE" | "PRACTICE" | "LAB";
  dayOfWeek: string | null;
  startTime: string | null;
  endTime: string | null;
  room: string;
}

export interface PublicProfessorAnnouncement {
  id: number;
  title: string;
  content: string;
  sectionId: number | null;
  sectionCode: string | null;
  publishedAt: string | null;
  pinned: boolean;
}

export interface PublicProfessorProfile {
  id: number;
  name: string;
  department: string;
  positionTitle: string;
  photoUrl: string;
  publicEmail: string;
  officeRoom: string;
  officeHours: string;
  bio: string;
  faculty: string;
  role: "TEACHER" | "TA";
  currentSections: PublicProfessorSection[];
  announcements: PublicProfessorAnnouncement[];
}

