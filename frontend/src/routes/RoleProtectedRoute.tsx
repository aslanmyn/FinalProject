import { Navigate } from "react-router-dom";
import { getUserRole } from "../lib/auth";
import type { UserRole } from "../types/auth";

interface RoleProtectedRouteProps {
  roles: UserRole[];
  children: JSX.Element;
}

export default function RoleProtectedRoute({ roles, children }: RoleProtectedRouteProps) {
  const role = getUserRole();
  if (!role || !roles.includes(role)) {
    return <Navigate to="/app" replace />;
  }
  return children;
}

