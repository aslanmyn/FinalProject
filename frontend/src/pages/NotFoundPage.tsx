import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="screen auth-screen">
      <div className="card" style={{ textAlign: "center", maxWidth: 400, padding: 40 }}>
        <h1 style={{ fontSize: 48, marginBottom: 8 }}>404</h1>
        <p className="muted" style={{ marginBottom: 16 }}>This page does not exist.</p>
        <Link className="link-btn" to="/">Go home</Link>
      </div>
    </div>
  );
}
