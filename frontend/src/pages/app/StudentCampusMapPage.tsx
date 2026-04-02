import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ApiError,
  fetchCampusBuildings,
  fetchCampusRoomsByBuilding,
  navigateCampus,
  searchCampusRooms
} from "../../lib/api";
import type { CampusBuilding, CampusRoom, NavigationResult } from "../../types/campus";

const BUILDING_TYPES = [
  { value: "", label: "All buildings" },
  { value: "ACADEMIC", label: "Academic" },
  { value: "LIBRARY", label: "Library" },
  { value: "LECTURE_HALL", label: "Lecture halls" },
  { value: "LAB", label: "Labs" },
  { value: "ADMIN", label: "Admin" },
  { value: "DORM", label: "Dorms" },
  { value: "CANTEEN", label: "Canteen" },
  { value: "SPORT", label: "Sport" },
  { value: "OTHER", label: "Other" }
];

function getRoomLabel(room: CampusRoom): string {
  const parts = [`${room.roomNumber}`];
  if (room.name) parts.push(room.name);
  if (room.buildingName) parts.push(room.buildingName);
  return parts.join(" - ");
}

export default function StudentCampusMapPage() {
  const [buildings, setBuildings] = useState<CampusBuilding[]>([]);
  const [rooms, setRooms] = useState<CampusRoom[]>([]);
  const [selectedBuilding, setSelectedBuilding] = useState<CampusBuilding | null>(null);
  const [buildingFilter, setBuildingFilter] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<CampusRoom[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [roomLoading, setRoomLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [fromRoomId, setFromRoomId] = useState<number | null>(null);
  const [toRoomId, setToRoomId] = useState<number | null>(null);
  const [navResult, setNavResult] = useState<NavigationResult | null>(null);
  const [navigating, setNavigating] = useState(false);

  const loadBuildings = useCallback(async (type?: string) => {
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
  }, []);

  useEffect(() => {
    void loadBuildings();
  }, [loadBuildings]);

  async function handleFilterChange(type: string) {
    setBuildingFilter(type);
    setSelectedBuilding(null);
    setRooms([]);
    setSearchResults(null);
    setNavResult(null);
    await loadBuildings(type || undefined);
  }

  async function handleSelectBuilding(building: CampusBuilding) {
    setSelectedBuilding(building);
    setSearchResults(null);
    setNavResult(null);
    setRoomLoading(true);
    setError(null);
    try {
      setRooms(await fetchCampusRoomsByBuilding(building.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load rooms");
    } finally {
      setRoomLoading(false);
    }
  }

  async function handleSearch() {
    const trimmed = searchQuery.trim();
    if (!trimmed) {
      setSearchResults(null);
      return;
    }
    setLoading(true);
    setError(null);
    setSelectedBuilding(null);
    try {
      setSearchResults(await searchCampusRooms(trimmed));
      setNavResult(null);
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
      setNavResult(await navigateCampus(fromRoomId, toRoomId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Navigation failed");
    } finally {
      setNavigating(false);
    }
  }

  const visibleRooms = searchResults ?? rooms;
  const navigationRooms = useMemo(() => {
    const map = new Map<number, CampusRoom>();
    [...rooms, ...(searchResults ?? [])].forEach((room) => map.set(room.id, room));
    return Array.from(map.values());
  }, [rooms, searchResults]);

  return (
    <div className="screen app-screen">
      <section className="card service-hero">
        <div className="service-hero-copy">
          <span className="auth-kicker">Campus Life</span>
          <h2>KBTU Campus Map</h2>
          <p className="muted">
            Browse buildings, inspect rooms, and build a route between locations using the current campus graph.
          </p>
        </div>
        <div className="service-hero-metrics">
          <div className="service-metric-card">
            <span>Buildings</span>
            <strong>{buildings.length}</strong>
          </div>
          <div className="service-metric-card">
            <span>Visible rooms</span>
            <strong>{visibleRooms.length}</strong>
          </div>
          <div className="service-metric-card">
            <span>Route ready</span>
            <strong>{navResult && navResult.totalDistanceMeters >= 0 ? "Yes" : "No"}</strong>
          </div>
        </div>
      </section>

      {error ? <div className="banner banner-danger">{error}</div> : null}

      <section className="card">
        <div className="service-toolbar">
          <input
            type="text"
            className="input"
            placeholder="Search rooms, for example 402 or Physics Lab"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                void handleSearch();
              }
            }}
          />
          <button className="btn btn-primary" onClick={() => void handleSearch()}>
            Search
          </button>
        </div>
        <div className="service-pill-row" style={{ marginTop: 12 }}>
          {BUILDING_TYPES.map((item) => (
            <button
              key={item.value}
              className={`btn btn-sm ${buildingFilter === item.value ? "btn-primary" : ""}`}
              onClick={() => void handleFilterChange(item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </section>

      <div className="service-split">
        <section className="card">
          <div className="service-section-header">
            <div>
              <h3>Buildings</h3>
              <p className="muted">Select a building to inspect its rooms.</p>
            </div>
          </div>

          {loading ? <p>Loading buildings...</p> : null}
          {!loading && buildings.length === 0 ? (
            <div className="service-empty">No buildings available yet.</div>
          ) : null}

          <div className="service-list">
            {buildings.map((building) => (
              <button
                key={building.id}
                type="button"
                className={`service-list-item${selectedBuilding?.id === building.id ? " is-active" : ""}`}
                onClick={() => void handleSelectBuilding(building)}
              >
                <div className="service-list-item-top">
                  <strong>{building.name}</strong>
                  {building.buildingType ? <span className="badge badge-info">{building.buildingType}</span> : null}
                </div>
                <span className="muted">
                  {building.code ? `Code ${building.code} - ` : ""}
                  {building.floorCount} floor{building.floorCount === 1 ? "" : "s"}
                </span>
                {building.description ? <p className="muted">{building.description}</p> : null}
              </button>
            ))}
          </div>
        </section>

        <section className="card">
          <div className="service-section-header">
            <div>
              <h3>{searchResults ? "Search results" : selectedBuilding ? selectedBuilding.name : "Rooms"}</h3>
              <p className="muted">
                {searchResults
                  ? "Use results to quickly set start and destination points."
                  : selectedBuilding
                    ? "Choose a room as the start or destination for navigation."
                    : "Search for a room or choose a building first."}
              </p>
            </div>
          </div>

          {roomLoading ? <p>Loading rooms...</p> : null}
          {!roomLoading && visibleRooms.length === 0 ? (
            <div className="service-empty">
              {searchResults ? "No matching rooms found." : "No rooms to show yet."}
            </div>
          ) : null}

          {visibleRooms.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Room</th>
                    <th>Building</th>
                    <th>Floor</th>
                    <th>Type</th>
                    <th>Capacity</th>
                    <th>Route</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleRooms.map((room) => (
                    <tr key={room.id}>
                      <td>
                        <strong>{room.roomNumber}</strong>
                        {room.name ? <div className="muted">{room.name}</div> : null}
                      </td>
                      <td>{room.buildingName || "Unknown"}</td>
                      <td>{room.floor}</td>
                      <td>{room.roomType || "-"}</td>
                      <td>{room.capacity ?? "-"}</td>
                      <td>
                        <div className="inline-actions">
                          <button className="btn btn-sm" onClick={() => setFromRoomId(room.id)}>
                            Set start
                          </button>
                          <button className="btn btn-sm btn-primary" onClick={() => setToRoomId(room.id)}>
                            Set destination
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>

      <section className="card">
        <div className="service-section-header">
          <div>
            <h3>Route builder</h3>
            <p className="muted">Choose any two known rooms and calculate the shortest accessible path.</p>
          </div>
        </div>

        <div className="service-form-grid">
          <div className="form-group">
            <label>From</label>
            <select
              className="input"
              value={fromRoomId ?? ""}
              onChange={(e) => setFromRoomId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Select a room</option>
              {navigationRooms.map((room) => (
                <option key={room.id} value={room.id}>
                  {getRoomLabel(room)}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Destination</label>
            <select
              className="input"
              value={toRoomId ?? ""}
              onChange={(e) => setToRoomId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Select a room</option>
              {navigationRooms.map((room) => (
                <option key={room.id} value={room.id}>
                  {getRoomLabel(room)}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group service-form-action">
            <label>&nbsp;</label>
            <button className="btn btn-primary" onClick={() => void handleNavigate()} disabled={navigating || !fromRoomId || !toRoomId}>
              {navigating ? "Calculating..." : "Get directions"}
            </button>
          </div>
        </div>

        {navResult ? (
          navResult.totalDistanceMeters < 0 ? (
            <div className="banner banner-danger" style={{ marginTop: 12 }}>
              No route found between the selected rooms.
            </div>
          ) : (
            <div className="service-route-box">
              <div className="banner banner-success">
                Total distance: <strong>{navResult.totalDistanceMeters.toFixed(0)} m</strong> across {navResult.edges.length} segment
                {navResult.edges.length === 1 ? "" : "s"}.
              </div>
              <div className="service-route-list">
                {navResult.edges.map((edge, index) => (
                  <div key={edge.id} className="service-route-item">
                    <strong>Segment {index + 1}</strong>
                    <span>{edge.distanceMeters.toFixed(0)} m</span>
                    <span className={`badge ${edge.accessible ? "badge-success" : "badge-warning"}`}>
                      {edge.accessible ? "Accessible" : "Limited access"}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )
        ) : null}
      </section>
    </div>
  );
}
