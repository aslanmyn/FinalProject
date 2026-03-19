import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, register } from "../lib/api";

export default function RegisterPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await register(email.trim(), password, confirmPassword, fullName.trim());
      setSuccess("Registration successful. Redirecting to login...");
      setTimeout(() => navigate("/login", { replace: true }), 1000);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Registration failed");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="screen auth-screen">
      <div className="auth-shell">
        <section className="auth-brand">
          <span className="auth-kicker">Create a workspace</span>
          <div className="auth-brand-copy">
            <h1>Start with a clean campus profile and move through every workflow.</h1>
            <p className="muted">
              Your role is detected from your university email format, then the portal opens the
              correct student, faculty or admin workspace automatically.
            </p>
          </div>

          <div className="auth-list">
            <article className="auth-list-item">
              <strong>Registration-aware</strong>
              <span>Academic and service modules unlock based on your account and permissions.</span>
            </article>
            <article className="auth-list-item">
              <strong>Realtime-ready</strong>
              <span>Notifications, chat and attendance updates stay live after you sign in.</span>
            </article>
          </div>

          <div className="auth-link-row">
            <Link className="link-btn" to="/login">
              Sign in
            </Link>
            <Link className="link-btn" to="/professors">
              Professors
            </Link>
          </div>
        </section>

        <section className="card auth-card">
          <span className="auth-kicker">New account</span>
          <h2>Create account</h2>
          <p className="muted">Use your university email. The role is detected automatically.</p>

          <form onSubmit={handleSubmit} className="form">
            <label>
              Full name
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
            </label>

            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={6}
              />
            </label>

            <label>
              Confirm password
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={6}
              />
            </label>

            {error ? <p className="error">{error}</p> : null}
            {success ? <p className="success">{success}</p> : null}

            <button type="submit" disabled={loading}>
              {loading ? "Creating..." : "Create account"}
            </button>
          </form>

          <div className="auth-footer">
            <span className="muted">Already registered?</span>
            <Link to="/login">Sign in</Link>
          </div>
        </section>
      </div>
    </div>
  );
}
