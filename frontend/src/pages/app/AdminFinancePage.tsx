import { type FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  createAdminHold,
  createAdminInvoice,
  createAdminPayment,
  fetchAdminHolds,
  fetchAdminStudents,
  removeAdminHold
} from "../../lib/api";
import type { AdminHoldItem, AdminSimpleStudentItem } from "../../types/admin";

const holdTypes = ["FINANCIAL", "ACADEMIC", "DISCIPLINARY", "MANUAL"] as const;

export default function AdminFinancePage() {
  const [students, setStudents] = useState<AdminSimpleStudentItem[]>([]);
  const [holds, setHolds] = useState<AdminHoldItem[]>([]);
  const [studentId, setStudentId] = useState<number | "">("");
  const [holdType, setHoldType] = useState<(typeof holdTypes)[number]>("FINANCIAL");
  const [holdReason, setHoldReason] = useState("");
  const [invoiceAmount, setInvoiceAmount] = useState(0);
  const [invoiceDescription, setInvoiceDescription] = useState("");
  const [invoiceDueDate, setInvoiceDueDate] = useState("");
  const [paymentChargeId, setPaymentChargeId] = useState<number | "">("");
  const [paymentAmount, setPaymentAmount] = useState(0);
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().slice(0, 10));
  const [removalReason, setRemovalReason] = useState("Resolved");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [studentsData, holdsData] = await Promise.all([fetchAdminStudents(), fetchAdminHolds()]);
      setStudents(studentsData);
      setHolds(holdsData);
      setStudentId(studentsData[0]?.id ?? "");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load finance data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  async function handleCreateHold(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!studentId) return;
    setError(null);
    setSuccess(null);
    try {
      await createAdminHold(Number(studentId), holdType, holdReason.trim());
      setSuccess("Hold created");
      setHoldReason("");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create hold");
    }
  }

  async function handleRemoveHold(holdId: number) {
    setError(null);
    setSuccess(null);
    try {
      await removeAdminHold(holdId, removalReason.trim());
      setSuccess("Hold removed");
      await loadData();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to remove hold");
    }
  }

  async function handleCreateInvoice(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!studentId) return;
    setError(null);
    setSuccess(null);
    try {
      await createAdminInvoice(Number(studentId), Number(invoiceAmount), invoiceDescription.trim(), invoiceDueDate);
      setSuccess("Invoice created");
      setInvoiceAmount(0);
      setInvoiceDescription("");
      setInvoiceDueDate("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create invoice");
    }
  }

  async function handleRegisterPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!studentId || !paymentChargeId) return;
    setError(null);
    setSuccess(null);
    try {
      await createAdminPayment(Number(studentId), Number(paymentChargeId), Number(paymentAmount), paymentDate);
      setSuccess("Payment registered");
      setPaymentChargeId("");
      setPaymentAmount(0);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to register payment");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Admin Finance</h2>
      </header>

      {loading ? <p>Loading...</p> : null}
      {error ? <p className="error">{error}</p> : null}
      {success ? <p className="success">{success}</p> : null}

      {!loading ? (
        <>
          <section className="card">
            <label>
              Student
              <select value={studentId} onChange={(e) => setStudentId(e.target.value ? Number(e.target.value) : "")}>
                {students.map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.id} - {student.name} ({student.email})
                  </option>
                ))}
              </select>
            </label>
          </section>

          <section className="card">
            <h3>Create Hold</h3>
            <form className="inline-form" onSubmit={handleCreateHold}>
              <label>
                Type
                <select value={holdType} onChange={(e) => setHoldType(e.target.value as (typeof holdTypes)[number])}>
                  {holdTypes.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Reason
                <input value={holdReason} onChange={(e) => setHoldReason(e.target.value)} required />
              </label>
              <button type="submit" disabled={!studentId}>
                Create Hold
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Create Invoice</h3>
            <form className="inline-form" onSubmit={handleCreateInvoice}>
              <label>
                Amount
                <input type="number" step={0.01} value={invoiceAmount} onChange={(e) => setInvoiceAmount(Number(e.target.value))} required />
              </label>
              <label>
                Description
                <input value={invoiceDescription} onChange={(e) => setInvoiceDescription(e.target.value)} required />
              </label>
              <label>
                Due Date
                <input type="date" value={invoiceDueDate} onChange={(e) => setInvoiceDueDate(e.target.value)} required />
              </label>
              <button type="submit" disabled={!studentId}>
                Create Invoice
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Register Payment</h3>
            <form className="inline-form" onSubmit={handleRegisterPayment}>
              <label>
                Charge ID
                <input
                  type="number"
                  value={paymentChargeId}
                  onChange={(e) => setPaymentChargeId(e.target.value ? Number(e.target.value) : "")}
                  required
                />
              </label>
              <label>
                Amount
                <input type="number" step={0.01} value={paymentAmount} onChange={(e) => setPaymentAmount(Number(e.target.value))} required />
              </label>
              <label>
                Date
                <input type="date" value={paymentDate} onChange={(e) => setPaymentDate(e.target.value)} required />
              </label>
              <button type="submit" disabled={!studentId || !paymentChargeId}>
                Register Payment
              </button>
            </form>
          </section>

          <section className="card">
            <h3>Active Holds</h3>
            <label>
              Removal Reason
              <input value={removalReason} onChange={(e) => setRemovalReason(e.target.value)} />
            </label>
            {holds.length === 0 ? <p className="muted">No active holds.</p> : null}
            {holds.length > 0 ? (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Student</th>
                      <th>Type</th>
                      <th>Reason</th>
                      <th>Created At</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {holds.map((hold) => (
                      <tr key={hold.id}>
                        <td>{hold.id}</td>
                        <td>
                          {hold.studentId} - {hold.studentName}
                        </td>
                        <td>{hold.type}</td>
                        <td>{hold.reason}</td>
                        <td>{hold.createdAt}</td>
                        <td>
                          <button type="button" onClick={() => handleRemoveHold(hold.id)}>
                            Remove
                          </button>
                        </td>
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

