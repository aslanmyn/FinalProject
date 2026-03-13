export type UserRole = "STUDENT" | "PROFESSOR" | "ADMIN";

export interface LoginResponse {
  tokenType: "Bearer";
  accessToken: string;
  accessTokenExpiresInSeconds: number;
  refreshToken: string;
  refreshTokenExpiresInDays: number;
  role: UserRole;
  permissions: string[];
}

export interface RegisterResponse {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  status: string;
}
