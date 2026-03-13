import type { LoginResponse } from "../types/auth";
import type { UserRole } from "../types/auth";

const ACCESS_TOKEN_KEY = "kbtu_access_token";
const REFRESH_TOKEN_KEY = "kbtu_refresh_token";
const USER_ROLE_KEY = "kbtu_user_role";

export function saveAuthSession(payload: LoginResponse): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken);
  localStorage.setItem(USER_ROLE_KEY, payload.role);
}

export function clearAuthSession(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_ROLE_KEY);
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

export function isAuthenticated(): boolean {
  return Boolean(getAccessToken());
}
