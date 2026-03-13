import { useEffect, useState } from "react";
import { ApiError, fetchStudentFinancial } from "../../lib/api";
import type { StudentFinancialData } from "../../types/student";

export default function StudentFinancialPage() {
  const [data, setData] = useState<StudentFinancialData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchStudentFinancial();
        if (!cancelled) {
          setData(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load financial data");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Student Financial</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}

      {!loading && !error && data ? (
        <>
          <section className="card">
            <div className="kv-grid">
              <div>
                <strong>Balance:</strong> {data.balance}
              </div>
              <div>
                <strong>Financial Hold:</strong> {data.hasFinancialHold ? "Yes" : "No"}
              </div>
              <div>
                <strong>Charges:</strong> {data.charges.length}
              </div>
              <div>
                <strong>Payments:</strong> {data.payments.length}
              </div>
            </div>
          </section>

          <section className="card">
            <h3>Charges</h3>
            {data.charges.length === 0 ? <p className="muted">No charges.</p> : null}
            {data.charges.length > 0 ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Amount</th>
                      <th>Description</th>
                      <th>Due Date</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.charges.map((item) => (
                      <tr key={item.id}>
                        <td>{item.id}</td>
                        <td>{item.amount}</td>
                        <td>{item.description}</td>
                        <td>{item.dueDate}</td>
                        <td>{item.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </section>

          <section className="card">
            <h3>Payments</h3>
            {data.payments.length === 0 ? <p className="muted">No payments.</p> : null}
            {data.payments.length > 0 ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Amount</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.payments.map((item) => (
                      <tr key={item.id}>
                        <td>{item.id}</td>
                        <td>{item.amount}</td>
                        <td>{item.date}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </section>
        </>
      ) : null}
    </div>
  );
}

