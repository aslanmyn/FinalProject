import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import NewsPage from "./pages/NewsPage";
import ProfessorsPage from "./pages/ProfessorsPage";
import ProfessorProfilePage from "./pages/ProfessorProfilePage";
import StudentDashboardPage from "./pages/app/StudentDashboardPage";
import StudentRegistrationPage from "./pages/app/StudentRegistrationPage";
import StudentNotificationsPage from "./pages/app/StudentNotificationsPage";
import StudentPlannerPage from "./pages/app/StudentPlannerPage";
import StudentWorkflowsPage from "./pages/app/StudentWorkflowsPage";
import StudentSchedulePage from "./pages/app/StudentSchedulePage";
import StudentEnrollmentsPage from "./pages/app/StudentEnrollmentsPage";
import StudentJournalPage from "./pages/app/StudentJournalPage";
import StudentSectionDetailPage from "./pages/app/StudentSectionDetailPage";
import StudentTranscriptPage from "./pages/app/StudentTranscriptPage";
import StudentAttendancePage from "./pages/app/StudentAttendancePage";
import StudentExamsPage from "./pages/app/StudentExamsPage";
import StudentAssistantPage from "./pages/app/StudentAssistantPage";
import StudentFinancialPage from "./pages/app/StudentFinancialPage";
import StudentFilesPage from "./pages/app/StudentFilesPage";
import StudentNewsPage from "./pages/app/StudentNewsPage";
import StudentRequestsPage from "./pages/app/StudentRequestsPage";
import StudentDormPage from "./pages/app/StudentDormPage";
import StudentFoodPage from "./pages/app/StudentFoodPage";
import StudentCampusMapPage from "./pages/app/StudentCampusMapPage";
import StudentLaundryPage from "./pages/app/StudentLaundryPage";
import TeacherDashboardPage from "./pages/app/TeacherDashboardPage";
import TeacherNotificationsPage from "./pages/app/TeacherNotificationsPage";
import TeacherAssistantPage from "./pages/app/TeacherAssistantPage";
import TeacherRiskPage from "./pages/app/TeacherRiskPage";
import TeacherSectionsPage from "./pages/app/TeacherSectionsPage";
import TeacherSectionPage from "./pages/app/TeacherSectionPage";
import TeacherAttendancePage from "./pages/app/TeacherAttendancePage";
import TeacherGradebookPage from "./pages/app/TeacherGradebookPage";
import TeacherAnnouncementsPage from "./pages/app/TeacherAnnouncementsPage";
import TeacherMaterialsPage from "./pages/app/TeacherMaterialsPage";
import TeacherNotesPage from "./pages/app/TeacherNotesPage";
import TeacherGradeChangesPage from "./pages/app/TeacherGradeChangesPage";
import AdminDashboardPage from "./pages/app/AdminDashboardPage";
import AdminAnalyticsPage from "./pages/app/AdminAnalyticsPage";
import AdminAssistantPage from "./pages/app/AdminAssistantPage";
import AdminRegistrationPage from "./pages/app/AdminRegistrationPage";
import AdminNotificationsPage from "./pages/app/AdminNotificationsPage";
import AdminWorkflowsPage from "./pages/app/AdminWorkflowsPage";
import AdminAcademicPage from "./pages/app/AdminAcademicPage";
import AdminFinancePage from "./pages/app/AdminFinancePage";
import AdminModerationPage from "./pages/app/AdminModerationPage";
import AdminRequestsPage from "./pages/app/AdminRequestsPage";
import AdminUsersPage from "./pages/app/AdminUsersPage";
import ChatPage from "./pages/app/ChatPage";
import NotFoundPage from "./pages/NotFoundPage";
import ProtectedRoute from "./routes/ProtectedRoute";
import RoleProtectedRoute from "./routes/RoleProtectedRoute";
import RoleIndexRedirect from "./routes/RoleIndexRedirect";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/news" element={<NewsPage />} />
      <Route path="/professors" element={<ProfessorsPage />} />
      <Route path="/professors/:id" element={<ProfessorProfilePage />} />
      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<RoleIndexRedirect />} />

        <Route
          path="chat"
          element={
            <RoleProtectedRoute roles={["STUDENT", "PROFESSOR"]}>
              <ChatPage />
            </RoleProtectedRoute>
          }
        />

        <Route
          path="student"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentDashboardPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/registration"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentRegistrationPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/notifications"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentNotificationsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/requests"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentRequestsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/planner"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentPlannerPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/workflows"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentWorkflowsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/schedule"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentSchedulePage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/enrollments"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentEnrollmentsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/journal"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentJournalPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/sections/:sectionId"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentSectionDetailPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/transcript"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentTranscriptPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/attendance"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentAttendancePage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/exams"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentExamsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/assistant"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentAssistantPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/financial"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentFinancialPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/files"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentFilesPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/news"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentNewsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/dorm"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentDormPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/food"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentFoodPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/campus-map"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentCampusMapPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="student/laundry"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentLaundryPage />
            </RoleProtectedRoute>
          }
        />

        <Route
          path="teacher"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherDashboardPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/assistant"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherAssistantPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/notifications"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherNotificationsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/risk"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherRiskPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/sections"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherSectionsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/sections/:sectionId"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherSectionPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/attendance"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherAttendancePage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/gradebook"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherGradebookPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/announcements"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherAnnouncementsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/materials"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherMaterialsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/notes"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherNotesPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/grade-changes"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherGradeChangesPage />
            </RoleProtectedRoute>
          }
        />

        <Route
          path="admin"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminDashboardPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/analytics"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminAnalyticsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/assistant"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminAssistantPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/workflows"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminWorkflowsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/registration"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminRegistrationPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/notifications"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminNotificationsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/users"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminUsersPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/requests"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminRequestsPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/academic"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminAcademicPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/finance"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminFinancePage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="admin/moderation"
          element={
            <RoleProtectedRoute roles={["ADMIN"]}>
              <AdminModerationPage />
            </RoleProtectedRoute>
          }
        />
      </Route>
      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
