export interface ApiPageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface AppNotificationItem {
  id: number;
  type: string;
  title: string;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationCenterData {
  notifications: AppNotificationItem[];
  unreadCount: number;
}

export type WorkflowType = "REQUEST" | "FX" | "MOBILITY" | "CLEARANCE" | "GRADE_CHANGE" | "REGISTRATION";

export interface WorkflowItem {
  type: WorkflowType;
  entityId: number;
  title: string;
  subject: string;
  status: string;
  createdAt: string | null;
  updatedAt: string | null;
  dueAt: string | null;
  overdue: boolean;
  nextStatuses: string[];
  link: string;
}

export interface WorkflowOverview {
  items: WorkflowItem[];
}

export interface WorkflowTimelineItem {
  createdAt: string;
  action: string;
  actorEmail: string | null;
  details: string | null;
}

export interface WorkflowTimeline {
  type: WorkflowType;
  entityId: number;
  items: WorkflowTimelineItem[];
}
