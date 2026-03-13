import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="screen">
      <div className="card">
        <h2>404</h2>
        <p>Page not found.</p>
        <Link to="/">Go home</Link>
      </div>
    </div>
  );
}
