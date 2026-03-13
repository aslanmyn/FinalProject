import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import NewsPage from "./pages/NewsPage";
import ProfessorsPage from "./pages/ProfessorsPage";
import ProfessorProfilePage from "./pages/ProfessorProfilePage";
import StudentDashboardPage from "./pages/app/StudentDashboardPage";
import StudentSchedulePage from "./pages/app/StudentSchedulePage";
import StudentEnrollmentsPage from "./pages/app/StudentEnrollmentsPage";
import StudentJournalPage from "./pages/app/StudentJournalPage";
import StudentTranscriptPage from "./pages/app/StudentTranscriptPage";
import StudentAttendancePage from "./pages/app/StudentAttendancePage";
import StudentExamsPage from "./pages/app/StudentExamsPage";
import StudentFinancialPage from "./pages/app/StudentFinancialPage";
import StudentFilesPage from "./pages/app/StudentFilesPage";
import StudentNewsPage from "./pages/app/StudentNewsPage";
import StudentRequestsPage from "./pages/app/StudentRequestsPage";
import TeacherDashboardPage from "./pages/app/TeacherDashboardPage";
import TeacherSectionPage from "./pages/app/TeacherSectionPage";
import TeacherAttendancePage from "./pages/app/TeacherAttendancePage";
import TeacherGradebookPage from "./pages/app/TeacherGradebookPage";
import TeacherAnnouncementsPage from "./pages/app/TeacherAnnouncementsPage";
import TeacherMaterialsPage from "./pages/app/TeacherMaterialsPage";
import TeacherNotesPage from "./pages/app/TeacherNotesPage";
import TeacherGradeChangesPage from "./pages/app/TeacherGradeChangesPage";
import AdminDashboardPage from "./pages/app/AdminDashboardPage";
import AdminAcademicPage from "./pages/app/AdminAcademicPage";
import AdminFinancePage from "./pages/app/AdminFinancePage";
import AdminModerationPage from "./pages/app/AdminModerationPage";
import AdminRequestsPage from "./pages/app/AdminRequestsPage";
import AdminUsersPage from "./pages/app/AdminUsersPage";
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
          path="student"
          element={
            <RoleProtectedRoute roles={["STUDENT"]}>
              <StudentDashboardPage />
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
          path="teacher"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherDashboardPage />
            </RoleProtectedRoute>
          }
        />
        <Route
          path="teacher/sections"
          element={
            <RoleProtectedRoute roles={["PROFESSOR"]}>
              <TeacherDashboardPage />
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
