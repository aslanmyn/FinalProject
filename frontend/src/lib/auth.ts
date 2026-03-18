import type { LoginResponse } from "../types/auth";
import type { UserRole } from "../types/auth";

const ACCESS_TOKEN_KEY = "kbtu_access_token";
const REFRESH_TOKEN_KEY = "kbtu_refresh_token";
const USER_ROLE_KEY = "kbtu_user_role";
const USER_PERMISSIONS_KEY = "kbtu_user_permissions";

export function saveAuthSession(payload: LoginResponse): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken);
  localStorage.setItem(USER_ROLE_KEY, payload.role);
  localStorage.setItem(USER_PERMISSIONS_KEY, JSON.stringify(payload.permissions ?? []));
}

export function clearAuthSession(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_ROLE_KEY);
  localStorage.removeItem(USER_PERMISSIONS_KEY);
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getUserRole(): UserRole | null {
  const role = localStorage.getItem(USER_ROLE_KEY);
  if (role === "STUDENT" || role === "PROFESSOR" || role === "ADMIN") {
    return role;
  }
  return null;
}

export function getUserPermissions(): string[] {
  const raw = localStorage.getItem(USER_PERMISSIONS_KEY);
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
}

export function hasAdminPermission(permission: string): boolean {
  const permissions = getUserPermissions();
  return permissions.includes("SUPER") || permissions.includes(permission);
}

export function getDefaultAppRoute(): string {
  const role = getUserRole();
  if (role === "STUDENT") return "/app/student";
  if (role === "PROFESSOR") return "/app/teacher";
  if (role === "ADMIN") {
    if (hasAdminPermission("SUPER")) return "/app/admin";
    if (hasAdminPermission("REGISTRAR")) return "/app/admin/registration";
    if (hasAdminPermission("FINANCE")) return "/app/admin/finance";
    if (hasAdminPermission("SUPPORT")) return "/app/admin/requests";
    if (hasAdminPermission("CONTENT")) return "/app/admin/moderation";
    return "/app/admin/notifications";
  }
  return "/login";
}

export function isAuthenticated(): boolean {
  return Boolean(getAccessToken());
}
