import { Navigate } from "react-router-dom";
import { getDefaultAppRoute } from "../lib/auth";

export default function RoleIndexRedirect() {
  return <Navigate to={getDefaultAppRoute()} replace />;
}
