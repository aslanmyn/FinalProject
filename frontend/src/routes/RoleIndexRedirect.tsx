import { Navigate } from "react-router-dom";
import { getUserRole } from "../lib/auth";

export default function RoleIndexRedirect() {
  const role = getUserRole();
  if (role === "STUDENT") return <Navigate to="/app/student" replace />;
  if (role === "PROFESSOR") return <Navigate to="/app/teacher" replace />;
  if (role === "ADMIN") return <Navigate to="/app/admin" replace />;
  return <Navigate to="/login" replace />;
}

