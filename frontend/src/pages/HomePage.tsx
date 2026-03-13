import { Link, Navigate } from "react-router-dom";
import { isAuthenticated } from "../lib/auth";

export default function HomePage() {
  if (isAuthenticated()) {
    return <Navigate to="/app" replace />;
  }

  return (
    <div className="screen">
      <section className="card">
        <h1>KBTU Portal</h1>
        <p className="muted">React frontend connected to `/api/v1/**`.</p>
        <div className="actions">
          <Link className="link-btn" to="/login">
            Login
          </Link>
          <Link className="link-btn" to="/register">
            Register
          </Link>
          <Link className="link-btn" to="/news">
            Public News
          </Link>
          <Link className="link-btn" to="/professors">
            Professors
          </Link>
        </div>
      </section>
    </div>
  );
}

