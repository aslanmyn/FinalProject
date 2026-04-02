import { useEffect, useState } from "react";
import {
  ApiError,
  fetchLaundryRooms,
  fetchLaundryRoomAvailability,
  fetchLaundryMachines,
  fetchLaundryBookings,
  createLaundryBooking,
  cancelLaundryBooking
} from "../../lib/api";
import type { LaundryRoom, LaundryMachine, LaundryRoomAvailability, LaundryBooking } from "../../types/laundry";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
  });
}

type Tab = "rooms" | "bookings";

export default function StudentLaundryPage() {
  const [tab, setTab] = useState<Tab>("rooms");
  const [laundryRooms, setLaundryRooms] = useState<LaundryRoom[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<LaundryRoom | null>(null);
  const [availability, setAvailability] = useState<LaundryRoomAvailability | null>(null);
  const [machines, setMachines] = useState<LaundryMachine[]>([]);
  const [bookings, setBookings] = useState<LaundryBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Booking form
  const [bookMachineId, setBookMachineId] = useState<number | null>(null);
  const [bookDate, setBookDate] = useState("");
  const [bookTime, setBookTime] = useState("");
  const [bookDuration, setBookDuration] = useState(60);

  async function loadRooms() {
    setLoading(true);
    setError(null);
    try {
      setLaundryRooms(await fetchLaundryRooms());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load rooms");
    } finally {
      setLoading(false);
    }
  }

  async function loadBookings() {
    setLoading(true);
    setError(null);
    try {
      setBookings(await fetchLaundryBookings());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load bookings");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadRooms();
  }, []);

  useEffect(() => {
    if (tab === "bookings") void loadBookings();
  }, [tab]);

  async function handleSelectRoom(room: LaundryRoom) {
    setSelectedRoom(room);
    setBookMachineId(null);
    try {
      const [avail, machs] = await Promise.all([
        fetchLaundryRoomAvailability(room.id),
        fetchLaundryMachines(room.id)
      ]);
      setAvailability(avail);
      setMachines(machs);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load room details");
    }
  }

  async function handleBook() {
    if (!bookMachineId || !bookDate || !bookTime) return;
    setSubmitting(true);
    setError(null);
    try {
      const startTime = new Date(`${bookDate}T${bookTime}`).toISOString();
      await createLaundryBooking(bookMachineId, startTime, bookDuration);
      setBookMachineId(null);
      setBookDate("");
      setBookTime("");
      if (selectedRoom) await handleSelectRoom(selectedRoom);
      setTab("bookings");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to book");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancelBooking(id: number) {
    try {
      await cancelLaundryBooking(id);
      await loadBookings();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to cancel booking");
    }
  }

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>Laundry</h2>
      </header>

      {error ? <div className="banner banner-danger">{error}</div> : null}

      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
        <button className={`btn ${tab === "rooms" ? "btn-primary" : ""}`} onClick={() => setTab("rooms")}>Laundry Rooms</button>
        <button className={`btn ${tab === "bookings" ? "btn-primary" : ""}`} onClick={() => setTab("bookings")}>My Bookings</button>
      </div>

      {loading ? <p>Loading...</p> : null}

      {/* Rooms tab */}
      {!loading && tab === "rooms" ? (
        <div style={{ display: "grid", gridTemplateColumns: "300px 1fr", gap: "1rem" }}>
          {/* Room list */}
          <section className="card">
            <h3>Laundry Rooms</h3>
            {laundryRooms.length === 0 ? <p className="muted">No laundry rooms.</p> : null}
            <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
              {laundryRooms.map((room) => (
                <div
                  key={room.id}
                  onClick={() => handleSelectRoom(room)}
                  style={{
                    padding: "0.75rem",
                    borderRadius: 8,
                    border: `1px solid ${selectedRoom?.id === room.id ? "var(--accent)" : "var(--border)"}`,
                    background: selectedRoom?.id === room.id ? "var(--accent-subtle)" : "var(--card)",
                    cursor: "pointer"
                  }}
                >
                  <strong>{room.name}</strong>
                  <div className="muted" style={{ fontSize: "0.85rem" }}>{room.totalMachines} machines</div>
                </div>
              ))}
            </div>
          </section>

          {/* Machine details */}
          <section className="card">
            {!selectedRoom ? (
              <p className="muted">Select a laundry room to view machines.</p>
            ) : (
              <>
                <h3>{selectedRoom.name}</h3>

                {/* Availability summary */}
                {availability ? (
                  <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem", flexWrap: "wrap" }}>
                    <div style={{ padding: "0.75rem 1rem", borderRadius: 8, background: "var(--bg)", textAlign: "center", flex: 1 }}>
                      <div style={{ fontSize: "1.5rem", fontWeight: 700, color: "var(--success)" }}>{availability.availableMachines}</div>
                      <div className="muted" style={{ fontSize: "0.8rem" }}>Available</div>
                    </div>
                    <div style={{ padding: "0.75rem 1rem", borderRadius: 8, background: "var(--bg)", textAlign: "center", flex: 1 }}>
                      <div style={{ fontSize: "1.5rem", fontWeight: 700, color: "var(--warning)" }}>{availability.inUse}</div>
                      <div className="muted" style={{ fontSize: "0.8rem" }}>In Use</div>
                    </div>
                    <div style={{ padding: "0.75rem 1rem", borderRadius: 8, background: "var(--bg)", textAlign: "center", flex: 1 }}>
                      <div style={{ fontSize: "1.5rem", fontWeight: 700, color: "var(--danger)" }}>{availability.outOfOrder}</div>
                      <div className="muted" style={{ fontSize: "0.8rem" }}>Out of Order</div>
                    </div>
                    <div style={{ padding: "0.75rem 1rem", borderRadius: 8, background: "var(--bg)", textAlign: "center", flex: 1 }}>
                      <div style={{ fontSize: "1.5rem", fontWeight: 700 }}>{availability.totalMachines}</div>
                      <div className="muted" style={{ fontSize: "0.8rem" }}>Total</div>
                    </div>
                  </div>
                ) : null}

                {/* Machines grid */}
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(100px, 1fr))", gap: "0.5rem", marginBottom: "1rem" }}>
                  {machines.map((m) => {
                    const isAvailable = m.status === "AVAILABLE";
                    const isSelected = bookMachineId === m.id;
                    return (
                      <div
                        key={m.id}
                        onClick={() => { if (isAvailable) setBookMachineId(m.id); }}
                        style={{
                          padding: "0.75rem",
                          borderRadius: 8,
                          textAlign: "center",
                          cursor: isAvailable ? "pointer" : "not-allowed",
                          border: `2px solid ${isSelected ? "var(--accent)" : isAvailable ? "var(--success)" : m.status === "IN_USE" ? "var(--warning)" : "var(--danger)"}`,
                          background: isSelected ? "var(--accent-subtle)" : "var(--card)",
                          opacity: m.status === "OUT_OF_ORDER" ? 0.5 : 1
                        }}
                      >
                        <div style={{ fontWeight: 600 }}>#{m.machineNumber}</div>
                        <div style={{
                          fontSize: "0.7rem",
                          color: isAvailable ? "var(--success)" : m.status === "IN_USE" ? "var(--warning)" : "var(--danger)"
                        }}>
                          {m.status.replace("_", " ")}
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Booking form */}
                {bookMachineId ? (
                  <div style={{ borderTop: "1px solid var(--border)", paddingTop: "1rem" }}>
                    <h4>Book Machine #{machines.find((m) => m.id === bookMachineId)?.machineNumber}</h4>
                    <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap", alignItems: "flex-end" }}>
                      <div className="form-group" style={{ flex: 1, minWidth: 140 }}>
                        <label>Date</label>
                        <input type="date" className="input" value={bookDate} onChange={(e) => setBookDate(e.target.value)} />
                      </div>
                      <div className="form-group" style={{ flex: 1, minWidth: 120 }}>
                        <label>Time</label>
                        <input type="time" className="input" value={bookTime} onChange={(e) => setBookTime(e.target.value)} />
                      </div>
                      <div className="form-group" style={{ flex: 1, minWidth: 120 }}>
                        <label>Duration</label>
                        <select className="input" value={bookDuration} onChange={(e) => setBookDuration(Number(e.target.value))}>
                          <option value={30}>30 min</option>
                          <option value={60}>1 hour</option>
                          <option value={90}>1.5 hours</option>
                          <option value={120}>2 hours</option>
                        </select>
                      </div>
                      <button className="btn btn-primary" onClick={handleBook} disabled={submitting || !bookDate || !bookTime} style={{ marginBottom: "0.5rem" }}>
                        {submitting ? "Booking..." : "Book"}
                      </button>
                    </div>
                  </div>
                ) : null}
              </>
            )}
          </section>
        </div>
      ) : null}

      {/* Bookings tab */}
      {!loading && tab === "bookings" ? (
        <section className="card">
          <h3>My Bookings</h3>
          {bookings.length === 0 ? <p className="muted">No bookings yet.</p> : null}
          {bookings.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Machine</th><th>Start</th><th>End</th><th>Status</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {bookings.map((b) => (
                    <tr key={b.id}>
                      <td>Machine #{b.machineNumber}</td>
                      <td>{formatDate(b.timeSlotStart)}</td>
                      <td>{formatDate(b.timeSlotEnd)}</td>
                      <td>
                        <span className={`badge badge-${b.status === "COMPLETED" ? "success" : b.status === "CANCELLED" ? "danger" : b.status === "IN_PROGRESS" ? "warning" : "info"}`}>
                          {b.status}
                        </span>
                      </td>
                      <td>
                        {b.status === "BOOKED" ? (
                          <button className="btn btn-sm btn-danger" onClick={() => handleCancelBooking(b.id)}>Cancel</button>
                        ) : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
