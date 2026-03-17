import NotificationCenter from "../../components/NotificationCenter";
import {
  fetchTeacherNotifications,
  markAllTeacherNotificationsRead,
  markTeacherNotificationRead
} from "../../lib/api";

export default function TeacherNotificationsPage() {
  return (
    <NotificationCenter
      title="Teacher Notifications"
      subtitle="Assignments, workflow events, and system updates related to your sections and teaching duties."
      loadData={fetchTeacherNotifications}
      markRead={markTeacherNotificationRead}
      markAllRead={markAllTeacherNotificationsRead}
      emptyTitle="No teacher notifications yet"
      emptyText="Section assignments, request activity, and future academic workflow alerts will show up here."
    />
  );
}
