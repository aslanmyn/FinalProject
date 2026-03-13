import { useEffect, useState } from "react";
import { ApiError, fetchAdminUsers, setAdminUserPermissions } from "../../lib/api";
import type { AdminUserItem } from "../../types/admin";

export default function AdminUsersPage() {
  const [items, setItems] = useState<AdminUserItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionsByUserId, setPermissionsByUserId] = useState<Record<number, string>>({});

  async function loadUsers() {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchAdminUsers(0, 100);
      setItems(page.items);
      setPermissionsByUserId(
        page.items.reduce<Record<number, string>>((acc, user) => {
          acc[user.id] = user.permissions.join(",");
          return acc;
        }, {})
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load users");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadUsers();
  }, []);

  async function handleSavePermissions(userId: number) {
    const raw = permissionsByUserId[userId] || "";
    const permissions = raw
      .split(",")
      .map((item) => item.trim().toUpperCase())
      .filter(Boolean);
    setError(null);
    setSuccess(null);
    try {
      await setAdminUserPermissions(userId, permissions);
      setSuccess(`Permissions updated for user #${userId}`);
      await loadUsers();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update permissions");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Admin Users</h2>
      </header>

      <section className="card">
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        {!loading && !error ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Email</th>
                  <th>Full Name</th>
                  <th>Role</th>
                  <th>Enabled</th>
                  <th>Permissions</th>
                  <th>Save</th>
                </tr>
              </thead>
              <tbody>
                {items.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.email}</td>
                    <td>{user.fullName}</td>
                    <td>{user.role}</td>
                    <td>{user.enabled ? "Yes" : "No"}</td>
                    <td>
                      <input
                        value={permissionsByUserId[user.id] ?? ""}
                        onChange={(event) =>
                          setPermissionsByUserId((prev) => ({ ...prev, [user.id]: event.target.value }))
                        }
                        placeholder="SUPER,REGISTRAR,..."
                        disabled={user.role !== "ADMIN"}
                      />
                    </td>
                    <td>
                      <button type="button" onClick={() => handleSavePermissions(user.id)} disabled={user.role !== "ADMIN"}>
                        Save
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </div>
  );
}
