import { beforeEach, describe, expect, it } from "vitest";
import {
  clearAuthSession,
  getAccessToken,
  getDefaultAppRoute,
  getRefreshToken,
  getUserPermissions,
  getUserRole,
  hasAdminPermission,
  saveAuthSession
} from "./auth";

describe("auth session helpers", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("stores and restores role, tokens, and permissions", () => {
    saveAuthSession({
      tokenType: "Bearer",
      accessToken: "access-token",
      accessTokenExpiresInSeconds: 3600,
      refreshToken: "refresh-token",
      refreshTokenExpiresInDays: 7,
      role: "ADMIN",
      permissions: ["FINANCE"]
    });

    expect(getAccessToken()).toBe("access-token");
    expect(getRefreshToken()).toBe("refresh-token");
    expect(getUserRole()).toBe("ADMIN");
    expect(getUserPermissions()).toEqual(["FINANCE"]);
    expect(hasAdminPermission("FINANCE")).toBe(true);
    expect(hasAdminPermission("REGISTRAR")).toBe(false);
    expect(getDefaultAppRoute()).toBe("/app/admin/finance");
  });

  it("lets super admin access every admin workspace", () => {
    saveAuthSession({
      tokenType: "Bearer",
      accessToken: "access-token",
      accessTokenExpiresInSeconds: 3600,
      refreshToken: "refresh-token",
      refreshTokenExpiresInDays: 7,
      role: "ADMIN",
      permissions: ["SUPER"]
    });

    expect(hasAdminPermission("CONTENT")).toBe(true);
    expect(hasAdminPermission("SUPPORT")).toBe(true);
    expect(getDefaultAppRoute()).toBe("/app/admin");
  });

  it("clears the full auth session", () => {
    saveAuthSession({
      tokenType: "Bearer",
      accessToken: "access-token",
      accessTokenExpiresInSeconds: 3600,
      refreshToken: "refresh-token",
      refreshTokenExpiresInDays: 7,
      role: "STUDENT",
      permissions: []
    });

    clearAuthSession();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(getUserRole()).toBeNull();
    expect(getUserPermissions()).toEqual([]);
    expect(getDefaultAppRoute()).toBe("/login");
  });
});
