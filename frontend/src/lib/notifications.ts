import {
  fetchAdminNotifications,
  fetchStudentNotifications,
  fetchTeacherNotifications
} from "./api";
import { connectStomp, subscribeTo } from "./ws";
import type { NotificationCenterData } from "../types/common";
import type { UserRole } from "../types/auth";

export const NOTIFICATION_LIVE_EVENT = "kbtu:notifications-live";

export interface NotificationLivePayload {
  eventType: string;
  unreadCount: number;
  notificationId: number | null;
  timestamp: string;
}

export async function loadNotificationCenterData(role: UserRole): Promise<NotificationCenterData> {
  switch (role) {
    case "STUDENT":
      return fetchStudentNotifications();
    case "PROFESSOR":
      return fetchTeacherNotifications();
    case "ADMIN":
      return fetchAdminNotifications();
    default:
      throw new Error("Unsupported role");
  }
}

export function emitNotificationLive(payload: NotificationLivePayload): void {
  window.dispatchEvent(new CustomEvent<NotificationLivePayload>(NOTIFICATION_LIVE_EVENT, { detail: payload }));
}

export function bindNotificationLiveSocket(onEvent: (payload: NotificationLivePayload) => void): () => void {
  connectStomp();
  return subscribeTo("/user/queue/notifications", (message) => {
    const payload = JSON.parse(message.body) as NotificationLivePayload;
    onEvent(payload);
    emitNotificationLive(payload);
  });
}

