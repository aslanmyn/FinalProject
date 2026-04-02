import { useCallback, useEffect, useState } from "react";
import {
  ApiError,
  cancelLaundryBooking,
  createLaundryBooking,
  fetchLaundryBookings,
  fetchLaundryMachines,
  fetchLaundryRoomAvailability,
  fetchLaundryRooms
} from "../../lib/api";
import type { LaundryBooking, LaundryMachine, LaundryRoom, LaundryRoomAvailability } from "../../types/laundry";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

type Tab = "rooms" | "bookings";

export default function StudentLaundryPage() {
  const [tab, setTab] = useState<Tab>("rooms");
  const [rooms, setRooms] = useState<LaundryRoom[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<LaundryRoom | null>(null);
  const [availability, setAvailability] = useState<LaundryRoomAvailability | null>(null);
  const [machines, setMachines] = useState<LaundryMachine[]>([]);
  const [bookings, setBookings] = useState<LaundryBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [bookingsLoading, setBookingsLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [machineId, setMachineId] = useState<number | null>(null);
  const [bookingDate, setBookingDate] = useState("");
  const [bookingTime, setBookingTime] = useState("");
  const [bookingDuration, setBookingDuration] = useState(60);

  const loadRoomDetails = useCallback(async (room: LaundryRoom) => {
    setSelectedRoom(room);
    setMachineId(null);
    setDetailsLoading(true);
    setError(null);
    try {
      const [roomAvailability, roomMachines] = await Promise.all([
        fetchLaundryRoomAvailability(room.id),
        fetchLaundryMachines(room.id)
      ]);
      setAvailability(roomAvailability);
      setMachines(roomMachines);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load laundry room details");
    } finally {
      setDetailsLoading(false);
    }
  }, []);

  const loadRooms = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchLaundryRooms();
      setRooms(data);
      if (data.length > 0) {
        await loadRoomDetails(data[0]);
      } else {
        setSelectedRoom(null);
        setAvailability(null);
        setMachines([]);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load laundry rooms");
    } finally {
      setLoading(false);
    }
  }, [loadRoomDetails]);

  const loadBookings = useCallback(async () => {
    setBookingsLoading(true);
    setError(null);
    try {
      setBookings(await fetchLaundryBookings());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load laundry bookings");
    } finally {
      setBookingsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadRooms();
  }, [loadRooms]);

  useEffect(() => {
    if (tab === "bookings") {
      void loadBookings();
    }
  }, [loadBookings, tab]);

  async function handleBook() {
    if (!machineId || !bookingDate || !bookingTime) return;
    setSubmitting(true);
    setError(null);
    try {
      const startTime = new Date(`${bookingDate}T${bookingTime}`).toISOString();
      await createLaundryBooking(machineId, startTime, bookingDuration);
      setMachineId(null);
      setBookingDate("");
      setBookingTime("");
      if (selectedRoom) {
        await loadRoomDetails(selectedRoom);
      }
      setTab("bookings");
      await loadBookings();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create laundry booking");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(bookingId: number) {
    try {
      await cancelLaundryBooking(bookingId);
      await loadBookings();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to cancel laundry booking");
    }
  }

  return (
    <div className="screen app-screen">
      <section className="card service-hero">
        <div className="service-hero-copy">
          <span className="auth-kicker">Campus Life</span>
          <h2>Laundry booking</h2>
          <p className="muted">
            Reserve available machines, monitor room capacity, and manage your active washing slots from one place.
          </p>
        </div>
        <div className="service-hero-metrics">
          <div className="service-metric-card">
            <span>Rooms</span>
            <strong>{rooms.length}</strong>
          </div>
          <div className="service-metric-card">
            <span>Available</span>
            <strong>{availability ? availability.availableMachines : "-"}</strong>
          </div>
          <div className="service-metric-card">
            <span>Bookings</span>
            <strong>{bookings.length}</strong>
          </div>
        </div>
      </section>

      {error ? <div className="banner banner-danger">{error}</div> : null}

      <section className="card">
        <div className="service-toolbar">
          <div className="service-pill-row">
            <button className={`btn ${tab === "rooms" ? "btn-primary" : ""}`} onClick={() => setTab("rooms")}>
              Laundry rooms
            </button>
            <button className={`btn ${tab === "bookings" ? "btn-primary" : ""}`} onClick={() => setTab("bookings")}>
              My bookings
            </button>
          </div>
        </div>
      </section>

      {tab === "rooms" ? (
        <div className="service-split">
          <section className="card">
            <div className="service-section-header">
              <div>
                <h3>Rooms</h3>
                <p className="muted">Pick a laundry room to see current availability and machines.</p>
              </div>
            </div>

            {loading ? <p>Loading laundry rooms...</p> : null}
            {!loading && rooms.length === 0 ? <div className="service-empty">No laundry rooms are configured yet.</div> : null}

            <div className="service-list">
              {rooms.map((room) => (
                <button
                  key={room.id}
                  type="button"
                  className={`service-list-item${selectedRoom?.id === room.id ? " is-active" : ""}`}
                  onClick={() => void loadRoomDetails(room)}
                >
                  <div className="service-list-item-top">
                    <strong>{room.name}</strong>
                    <span className="badge badge-info">{room.totalMachines} machines</span>
                  </div>
                  <span className="muted">{room.dormBuildingId ? `Dorm building #${room.dormBuildingId}` : "General access room"}</span>
                </button>
              ))}
            </div>
          </section>

          <section className="card">
            <div className="service-section-header">
              <div>
                <h3>{selectedRoom ? selectedRoom.name : "Room details"}</h3>
                <p className="muted">Select an available machine and reserve a time slot.</p>
              </div>
            </div>

            {!selectedRoom ? (
              <div className="service-empty">Select a room to view available machines.</div>
            ) : (
              <>
                {detailsLoading ? <p>Loading room details...</p> : null}

                {availability ? (
                  <div className="service-metrics-grid">
                    <div className="service-metric-card">
                      <span>Available</span>
                      <strong>{availability.availableMachines}</strong>
                    </div>
                    <div className="service-metric-card">
                      <span>In use</span>
                      <strong>{availability.inUse}</strong>
                    </div>
                    <div className="service-metric-card">
                      <span>Out of order</span>
                      <strong>{availability.outOfOrder}</strong>
                    </div>
                    <div className="service-metric-card">
                      <span>Total</span>
                      <strong>{availability.totalMachines}</strong>
                    </div>
                  </div>
                ) : null}

                <div className="service-room-grid" style={{ marginTop: 16 }}>
                  {machines.map((machine) => {
                    const isAvailable = machine.status === "AVAILABLE";
                    const selected = machineId === machine.id;
                    return (
                      <button
                        key={machine.id}
                        type="button"
                        className={`service-room-tile${selected ? " is-selected" : ""}`}
                        disabled={!isAvailable}
                        onClick={() => isAvailable && setMachineId(machine.id)}
                      >
                        <strong>#{machine.machineNumber}</strong>
                        <span className={`badge ${isAvailable ? "badge-success" : machine.status === "IN_USE" ? "badge-warning" : "badge-danger"}`}>
                          {machine.status.replace("_", " ")}
                        </span>
                      </button>
                    );
                  })}
                </div>

                {machineId ? (
                  <div className="service-route-box">
                    <h4>Book machine #{machines.find((item) => item.id === machineId)?.machineNumber}</h4>
                    <div className="service-form-grid">
                      <div className="form-group">
                        <label>Date</label>
                        <input type="date" className="input" value={bookingDate} onChange={(e) => setBookingDate(e.target.value)} />
                      </div>
                      <div className="form-group">
                        <label>Time</label>
                        <input type="time" className="input" value={bookingTime} onChange={(e) => setBookingTime(e.target.value)} />
                      </div>
                      <div className="form-group">
                        <label>Duration</label>
                        <select className="input" value={bookingDuration} onChange={(e) => setBookingDuration(Number(e.target.value))}>
                          <option value={30}>30 min</option>
                          <option value={60}>1 hour</option>
                          <option value={90}>1.5 hours</option>
                          <option value={120}>2 hours</option>
                        </select>
                      </div>
                      <div className="form-group service-form-action">
                        <label>&nbsp;</label>
                        <button className="btn btn-primary" onClick={() => void handleBook()} disabled={submitting || !bookingDate || !bookingTime}>
                          {submitting ? "Booking..." : "Book machine"}
                        </button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="service-empty" style={{ marginTop: 16 }}>
                    Select an available machine to open the booking form.
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      ) : (
        <section className="card">
          <div className="service-section-header">
            <div>
              <h3>My bookings</h3>
              <p className="muted">Track current laundry reservations and cancel future slots if needed.</p>
            </div>
          </div>
          {bookingsLoading ? <p>Loading bookings...</p> : null}
          {!bookingsLoading && bookings.length === 0 ? <div className="service-empty">You do not have laundry bookings yet.</div> : null}
          <div className="service-stack">
            {bookings.map((booking) => (
              <div key={booking.id} className="service-inline-card service-inline-card-block">
                <div className="service-order-head">
                  <div>
                    <strong>Machine #{booking.machineNumber}</strong>
                    <div className="muted">{formatDate(booking.timeSlotStart)} to {formatDate(booking.timeSlotEnd)}</div>
                  </div>
                  <span className={`badge ${booking.status === "COMPLETED" ? "badge-success" : booking.status === "CANCELLED" ? "badge-danger" : booking.status === "IN_PROGRESS" ? "badge-warning" : "badge-info"}`}>
                    {booking.status}
                  </span>
                </div>
                <div className="service-order-footer">
                  <div className="muted">Created {formatDate(booking.createdAt)}</div>
                  {booking.status === "BOOKED" ? (
                    <button className="btn btn-sm btn-danger" onClick={() => void handleCancel(booking.id)}>
                      Cancel
                    </button>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
