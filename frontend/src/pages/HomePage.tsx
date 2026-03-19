import { Link, Navigate } from "react-router-dom";
import { isAuthenticated } from "../lib/auth";

export default function HomePage() {
  if (isAuthenticated()) {
    return <Navigate to="/app" replace />;
  }

  return (
    <div className="screen landing-screen">
      <section className="card landing-hero-card">
        <div className="landing-hero-copy">
          <span className="auth-kicker">KBTU digital campus</span>
          <h1>Academic operations, teaching and administration in one place.</h1>
          <p className="muted">
            A unified university workspace with student services, faculty tools, admin operations,
            live attendance, realtime communication and assistant-driven analytics.
          </p>

          <div className="actions">
            <Link className="link-btn" to="/login">
              Sign in
            </Link>
            <Link className="link-btn" to="/register">
              Create account
            </Link>
            <Link className="link-btn" to="/news">
              Public news
            </Link>
            <Link className="link-btn" to="/professors">
              Professors
            </Link>
          </div>
        </div>

        <div className="landing-highlights">
          <article className="landing-highlight-card">
            <span className="profile-fact-label">Student workspace</span>
            <strong>Registration, planner, transcript, attendance and AI guidance.</strong>
          </article>
          <article className="landing-highlight-card">
            <span className="profile-fact-label">Faculty tools</span>
            <strong>Sections, gradebook, materials, realtime attendance and risk views.</strong>
          </article>
          <article className="landing-highlight-card">
            <span className="profile-fact-label">Administration</span>
            <strong>Academic setup, finance, requests, workflows and analytics.</strong>
          </article>
          <article className="landing-highlight-card">
            <span className="profile-fact-label">Realtime layer</span>
            <strong>Notifications, chat and attendance updates without reloading.</strong>
          </article>
        </div>
      </section>
    </div>
  );
}
