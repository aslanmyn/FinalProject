import { useEffect, useState } from "react";
import {
  ApiError,
  fetchCampusBuildings,
  fetchCampusRoomsByBuilding,
  searchCampusRooms,
  navigateCampus
} from "../../lib/api";
import type { CampusBuilding, CampusRoom, NavigationResult } from "../../types/campus";

const BUILDING_TYPES = [
  { value: "", label: "All Buildings" },
  { value: "ACADEMIC", label: "Academic" },
  { value: "LIBRARY", label: "Libraries" },
  { value: "LECTURE_HALL", label: "Lecture Halls" },
  { value: "LAB", label: "Labs" },
  { value: "ADMIN", label: "Admin" },
  { value: "DORM", label: "Dorms" },
  { value: "CANTEEN", label: "Canteen" },
  { value: "SPORT", label: "Sport" },
  { value: "OTHER", label: "Other" }
];

export default function StudentCampusMapPage() {
  const [buildings, setBuildings] = useState<CampusBuilding[]>([]);
  const [rooms, setRooms] = useState<CampusRoom[]>([]);
  const [selectedBuilding, setSelectedBuilding] = useState<CampusBuilding | null>(null);
  const [buildingFilter, setBuildingFilter] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<CampusRoom[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Navigation
  const [fromRoomId, setFromRoomId] = useState<number | null>(null);
  const [toRoomId, setToRoomId] = useState<number | null>(null);
  const [navResult, setNavResult] = useState<NavigationResult | null>(null);
  const [navigating, setNavigating] = useState(false);

  async function loadBuildings(type?: string) {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchCampusBuildings(type || undefined);
      setBuildings(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load buildings");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadBuildings();
  }, []);

  async function handleFilterChange(type: string) {
    setBuildingFilter(type);
    setSelectedBuilding(null);
    setRooms([]);
    await loadBuildings(type || undefined);
  }

  async function handleSelectBuilding(building: CampusBuilding) {
    setSelectedBuilding(building);
    setSearchResults(null);
    try {
      const rms = await fetchCampusRoomsByBuilding(building.id);
      setRooms(rms);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load rooms");
    }
  }

  async function handleSearch() {
    if (!searchQuery.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const results = await searchCampusRooms(searchQuery);
      setSearchResults(results);
      setSelectedBuilding(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Search failed");
    } finally {
      setLoading(false);
    }
  }

  async function handleNavigate() {
    if (!fromRoomId || !toRoomId) return;
    setNavigating(true);
    setError(null);
    try {
      const result = await navigateCampus(fromRoomId, toRoomId);
      setNavResult(result);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Navigation failed");
    } finally {
      setNavigating(false);
    }
  }

  // Collect all known rooms for navigation dropdowns
  const allKnownRooms = searchResults || rooms;

  return (
    <div className="screen app-screen">
      <header className="topbar">
        <h2>KBTU Campus Map</h2>
      </header>

      {error ? <div className="banner banner-danger">{error}</div> : null}

      {/* Search bar */}
      <section className="card">
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <input
            type="text"
            className="input"
            placeholder="Find Room (e.g., Room 402)"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") void handleSearch(); }}
            style={{ flex: 1 }}
          />
          <button className="btn btn-primary" onClick={handleSearch}>Search</button>
        </div>
      </section>

      {/* Building type filter */}
      <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap", marginBottom: "1rem" }}>
        {BUILDING_TYPES.map((bt) => (
          <button
            key={bt.value}
            className={`btn btn-sm ${buildingFilter === bt.value ? "btn-primary" : ""}`}
            onClick={() => handleFilterChange(bt.value)}
          >
            {bt.label}
          </button>
        ))}
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
        {/* Buildings list */}
        <section className="card">
          <h3>Buildings</h3>
          {loading ? <p>Loading...</p> : null}
          {!loading && buildings.length === 0 ? <p className="muted">No buildings found.</p> : null}
          <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
            {buildings.map((b) => (
              <div
                key={b.id}
                onClick={() => handleSelectBuilding(b)}
                style={{
                  padding: "0.75rem",
                  borderRadius: 8,
                  border: `1px solid ${selectedBuilding?.id === b.id ? "var(--accent)" : "var(--border)"}`,
                  background: selectedBuilding?.id === b.id ? "var(--accent-subtle)" : "var(--card)",
                  cursor: "pointer"
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <strong>{b.name}</strong>
                  {b.buildingType ? (
                    <span className="badge badge-info" style={{ fontSize: "0.7rem" }}>{b.buildingType}</span>
                  ) : null}
                </div>
                {b.code ? <span className="muted" style={{ fontSize: "0.8rem" }}>Code: {b.code}</span> : null}
                {b.description ? <p className="muted" style={{ fontSize: "0.8rem", margin: "0.25rem 0 0" }}>{b.description}</p> : null}
                <span className="muted" style={{ fontSize: "0.8rem" }}>{b.floorCount} floor{b.floorCount > 1 ? "s" : ""}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Rooms / Search results */}
        <section className="card">
          <h3>
            {searchResults ? "Search Results" : selectedBuilding ? `Rooms — ${selectedBuilding.name}` : "Rooms"}
          </h3>

          {/* Search results */}
          {searchResults ? (
            searchResults.length === 0 ? <p className="muted">No rooms found.</p> : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr><th>Room</th><th>Building</th><th>Floor</th><th>Type</th><th>Actions</th></tr>
                  </thead>
                  <tbody>
                    {searchResults.map((r) => (
                      <tr key={r.id}>
                        <td><strong>{r.roomNumber}</strong>{r.name ? ` — ${r.name}` : ""}</td>
                        <td>{r.buildingName}</td>
                        <td>{r.floor}</td>
                        <td>{r.roomType || "—"}</td>
                        <td>
                          <button className="btn btn-sm" onClick={() => setToRoomId(r.id)}>Navigate here</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          ) : null}

          {/* Building rooms */}
          {!searchResults && selectedBuilding ? (
            rooms.length === 0 ? <p className="muted">No rooms in this building.</p> : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr><th>Room</th><th>Floor</th><th>Type</th><th>Name</th><th>Capacity</th><th>Actions</th></tr>
                  </thead>
                  <tbody>
                    {rooms.map((r) => (
                      <tr key={r.id}>
                        <td><strong>{r.roomNumber}</strong></td>
                        <td>{r.floor}</td>
                        <td>{r.roomType || "—"}</td>
                        <td>{r.name || "—"}</td>
                        <td>{r.capacity ?? "—"}</td>
                        <td>
                          <button className="btn btn-sm" onClick={() => setToRoomId(r.id)}>Navigate here</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          ) : null}

          {!searchResults && !selectedBuilding ? (
            <p className="muted">Select a building or search for a room.</p>
          ) : null}
        </section>
      </div>

      {/* Navigation */}
      <section className="card" style={{ marginTop: "1rem" }}>
        <h3>Navigate Between Rooms</h3>
        <div style={{ display: "flex", gap: "0.75rem", alignItems: "flex-end", flexWrap: "wrap" }}>
          <div className="form-group" style={{ flex: 1, minWidth: 150 }}>
            <label>From Room ID</label>
            <input type="number" className="input" value={fromRoomId ?? ""} onChange={(e) => setFromRoomId(e.target.value ? Number(e.target.value) : null)} placeholder="Room ID" />
          </div>
          <div className="form-group" style={{ flex: 1, minWidth: 150 }}>
            <label>To Room ID</label>
            <input type="number" className="input" value={toRoomId ?? ""} onChange={(e) => setToRoomId(e.target.value ? Number(e.target.value) : null)} placeholder="Room ID" />
          </div>
          <button className="btn btn-primary" onClick={handleNavigate} disabled={navigating || !fromRoomId || !toRoomId} style={{ marginBottom: "0.5rem" }}>
            {navigating ? "Calculating..." : "Get Directions"}
          </button>
        </div>

        {navResult ? (
          <div style={{ marginTop: "1rem" }}>
            {navResult.totalDistanceMeters < 0 ? (
              <div className="banner banner-danger">No route found between these rooms.</div>
            ) : (
              <div className="banner banner-success">
                Route found! Total distance: <strong>{navResult.totalDistanceMeters.toFixed(0)}m</strong> ({navResult.edges.length} segments)
              </div>
            )}
          </div>
        ) : null}
      </section>
    </div>
  );
}
