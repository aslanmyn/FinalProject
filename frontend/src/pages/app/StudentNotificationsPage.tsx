import NotificationCenter from "../../components/NotificationCenter";
import {
  fetchStudentNotifications,
  markAllStudentNotificationsRead,
  markStudentNotificationRead
} from "../../lib/api";

export default function StudentNotificationsPage() {
  return (
    <NotificationCenter
      title="Notifications Center"
      subtitle="Academic events, finance updates, requests, and publication alerts for your student account."
      loadData={fetchStudentNotifications}
      markRead={markStudentNotificationRead}
      markAllRead={markAllStudentNotificationsRead}
      emptyTitle="No notifications yet"
      emptyText="When registration, grades, requests, finance, or announcements change, they will appear here."
    />
  );
}
