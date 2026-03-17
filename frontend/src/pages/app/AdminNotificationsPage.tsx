import NotificationCenter from "../../components/NotificationCenter";
import {
  fetchAdminNotifications,
  markAdminNotificationRead,
  markAllAdminNotificationsRead
} from "../../lib/api";

export default function AdminNotificationsPage() {
  return (
    <NotificationCenter
      title="Admin Notifications"
      subtitle="Requests, grade changes, and operational alerts that need attention from the back-office side."
      loadData={fetchAdminNotifications}
      markRead={markAdminNotificationRead}
      markAllRead={markAllAdminNotificationsRead}
      emptyTitle="No admin notifications yet"
      emptyText="New requests, FX actions, and registrar or finance events will appear here."
    />
  );
}
