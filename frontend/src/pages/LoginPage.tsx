import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, login } from "../lib/api";
import { saveAuthSession } from "../lib/auth";

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const payload = await login(email.trim(), password);
      saveAuthSession(payload);
      navigate("/app", { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Login failed");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="screen auth-screen">
      <div className="auth-shell">
        <section className="auth-brand">
          <span className="auth-kicker">KBTU Portal</span>
          <div className="auth-brand-copy">
            <h1>Access the whole campus workflow from one place.</h1>
            <p className="muted">
              One workspace for academic records, attendance, registration, analytics,
              requests, communication and live updates.
            </p>
          </div>

          <div className="auth-list">
            <article className="auth-list-item">
              <strong>Students</strong>
              <span>Registration, planner, transcript, attendance and assistant.</span>
            </article>
            <article className="auth-list-item">
              <strong>Faculty</strong>
              <span>Sections, gradebook, live attendance, risk dashboard and materials.</span>
            </article>
            <article className="auth-list-item">
              <strong>Administration</strong>
              <span>Academic setup, finance, workflows, analytics and notifications.</span>
            </article>
          </div>

          <div className="auth-link-row">
            <Link className="link-btn" to="/">
              Home
            </Link>
            <Link className="link-btn" to="/news">
              Public news
            </Link>
            <Link className="link-btn" to="/professors">
              Professors
            </Link>
          </div>
        </section>

        <section className="card auth-card">
          <span className="auth-kicker">Welcome back</span>
          <h2>Sign in</h2>
          <p className="muted">Use your university account to continue.</p>

          <form onSubmit={handleSubmit} className="form">
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="username"
              />
            </label>

            <label>
              Password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </label>

            {error ? <p className="error">{error}</p> : null}

            <button type="submit" disabled={loading}>
              {loading ? "Signing in..." : "Sign in"}
            </button>
          </form>

          <div className="auth-footer">
            <span className="muted">Need an account?</span>
            <Link to="/register">Register</Link>
          </div>
        </section>
      </div>
    </div>
  );
}
