import { useEffect, useRef, useState, useCallback } from "react";
import {
  fetchChatRooms,
  fetchChatMessages,
  fetchChatMembers,
  getOrCreateSectionRoom,
  getOrCreateDirectRoom,
  createGroupRoom,
  searchChatUsers,
  fetchStudentEnrollments,
  fetchTeacherSections,
} from "../../lib/api";
import { getUserRole } from "../../lib/auth";
import { connectStomp, disconnectStomp, subscribeTo, sendStompMessage } from "../../lib/ws";
import type { ChatRoom, ChatMessageItem, ChatMember, ChatUserResult } from "../../types/chat";

export default function ChatPage() {
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [activeRoomId, setActiveRoomId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessageItem[]>([]);
  const [members, setMembers] = useState<ChatMember[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [showMembers, setShowMembers] = useState(false);
  const [showNewChat, setShowNewChat] = useState(false);
  const [roomSearch, setRoomSearch] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const unsubRef = useRef<(() => void) | null>(null);
  const myUserId = useRef<number | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("kbtu_access_token");
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        myUserId.current = Number(payload.sub);
      } catch { /* ignore */ }
    }
  }, []);

  const loadRooms = useCallback(async () => {
    try {
      const data = await fetchChatRooms();
      setRooms(data);
    } catch { /* ignore */ } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadRooms(); }, [loadRooms]);

  useEffect(() => {
    connectStomp(() => setConnected(true));
    return () => { disconnectStomp(); setConnected(false); };
  }, []);

  useEffect(() => {
    if (!activeRoomId || !connected) return;
    unsubRef.current?.();
    unsubRef.current = subscribeTo(`/topic/chat/${activeRoomId}`, (msg) => {
      try {
        const incoming: ChatMessageItem = JSON.parse(msg.body);
        setMessages((prev) => [...prev, incoming]);
      } catch { /* ignore */ }
    });
    return () => { unsubRef.current?.(); unsubRef.current = null; };
  }, [activeRoomId, connected]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function openRoom(roomId: number) {
    setActiveRoomId(roomId);
    setMessages([]);
    setShowMembers(false);
    setShowNewChat(false);
    try {
      const data = await fetchChatMessages(roomId, 0, 100);
      setMessages(data.items.reverse());
      const m = await fetchChatMembers(roomId);
      setMembers(m);
    } catch { /* ignore */ }
  }

  function handleSend() {
    if (!input.trim() || !activeRoomId) return;
    sendStompMessage(`/app/chat/${activeRoomId}`, { content: input.trim() });
    setInput("");
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSend(); }
  }

  function formatTime(iso: string) {
    const d = new Date(iso);
    const now = new Date();
    const isToday = d.toDateString() === now.toDateString();
    if (isToday) return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    return d.toLocaleDateString([], { month: "short", day: "numeric" }) + " " +
      d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }

  function getInitials(name: string) {
    return name.split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2);
  }

  function getRoleColor(role: string) {
    if (role === "PROFESSOR") return "var(--chat-professor)";
    if (role === "ADMIN") return "var(--chat-admin)";
    return "var(--chat-student)";
  }

  function getRoomIcon(room: ChatRoom) {
    if (room.type === "SECTION") return "G";
    if (room.type === "GROUP") return "#";
    return getInitials(room.name || "DM");
  }

  function getRoomTypeLabel(room: ChatRoom) {
    if (room.type === "SECTION") return "Section Chat";
    if (room.type === "GROUP") return "Group";
    return "Direct";
  }

  const activeRoom = rooms.find((r) => r.id === activeRoomId);
  const filteredRooms = rooms.filter((r) =>
    !roomSearch || (r.name || "").toLowerCase().includes(roomSearch.toLowerCase())
  );

  return (
    <div className="chat-layout">
      {/* Sidebar */}
      <div className="chat-sidebar">
        <div className="chat-sidebar-header">
          <h3>Messages</h3>
          <button
            className="chat-new-btn"
            onClick={() => setShowNewChat(!showNewChat)}
            title="New chat"
          >+</button>
        </div>

        {/* Room search */}
        <div className="chat-search-wrap">
          <input
            className="chat-search-input"
            type="text"
            placeholder="Search conversations..."
            value={roomSearch}
            onChange={(e) => setRoomSearch(e.target.value)}
          />
        </div>

        {showNewChat && (
          <NewChatPanel
            onCreated={(room) => {
              setShowNewChat(false);
              setRooms((prev) => {
                if (prev.some((r) => r.id === room.id)) return prev;
                return [room, ...prev];
              });
              openRoom(room.id);
            }}
          />
        )}

        <div className="chat-room-list">
          {loading && <p className="chat-empty">Loading...</p>}
          {!loading && filteredRooms.length === 0 && (
            <p className="chat-empty">
              {roomSearch ? "No matching conversations." : "No chats yet. Create one with the + button above."}
            </p>
          )}
          {filteredRooms.map((room) => (
            <button
              key={room.id}
              className={`chat-room-item${room.id === activeRoomId ? " active" : ""}`}
              onClick={() => openRoom(room.id)}
            >
              <span className={`chat-room-avatar ${room.type === "SECTION" ? "section" : room.type === "GROUP" ? "group" : "direct"}`}>
                {getRoomIcon(room)}
              </span>
              <div className="chat-room-info">
                <span className="chat-room-name">{room.name || "Direct Message"}</span>
                <span className="chat-room-type">{getRoomTypeLabel(room)}</span>
              </div>
            </button>
          ))}
        </div>

        <div className="chat-connection-status">
          <span className={`status-dot${connected ? " online" : ""}`} />
          {connected ? "Connected" : "Connecting..."}
        </div>
      </div>

      {/* Main chat area */}
      <div className="chat-main">
        {!activeRoomId ? (
          <div className="chat-placeholder">
            <div className="chat-placeholder-icon">💬</div>
            <h3>Select a conversation</h3>
            <p>Choose a chat from the sidebar or create a new one to start messaging.</p>
          </div>
        ) : (
          <>
            <div className="chat-header">
              <div className="chat-header-info">
                <h3>{activeRoom?.name || "Chat"}</h3>
                <span className="chat-header-meta">{members.length} members</span>
              </div>
              <button className="chat-members-toggle" onClick={() => setShowMembers(!showMembers)}>
                {showMembers ? "Hide" : "Members"}
              </button>
            </div>

            <div className="chat-body-wrapper">
              <div className="chat-messages">
                {messages.length === 0 && <p className="chat-empty">No messages yet. Say hello!</p>}
                {messages.map((msg, idx) => {
                  const isMe = msg.senderId === myUserId.current;
                  const showAvatar = idx === 0 || messages[idx - 1].senderId !== msg.senderId;
                  return (
                    <div key={msg.id} className={`chat-msg${isMe ? " me" : ""}`}>
                      {!isMe && showAvatar && (
                        <span className="chat-msg-avatar" style={{ background: getRoleColor(msg.senderRole) }}>
                          {getInitials(msg.senderName)}
                        </span>
                      )}
                      {!isMe && !showAvatar && <span className="chat-msg-avatar-spacer" />}
                      <div className="chat-msg-bubble-wrap">
                        {!isMe && showAvatar && (
                          <span className="chat-msg-sender" style={{ color: getRoleColor(msg.senderRole) }}>
                            {msg.senderName}
                            {msg.senderRole === "PROFESSOR" && <span className="chat-role-badge prof">Prof</span>}
                          </span>
                        )}
                        <div className={`chat-msg-bubble${isMe ? " me" : ""}`}>
                          {msg.content}
                          <span className="chat-msg-time">{formatTime(msg.createdAt)}</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
                <div ref={messagesEndRef} />
              </div>

              {showMembers && (
                <div className="chat-members-panel">
                  <h4>Members</h4>
                  {members.map((m) => (
                    <div key={m.id} className="chat-member-item">
                      <span className="chat-member-avatar" style={{ background: getRoleColor(m.role) }}>
                        {getInitials(m.name)}
                      </span>
                      <div>
                        <div className="chat-member-name">{m.name}</div>
                        <div className="chat-member-role">{m.role}</div>
                      </div>
                      {m.id !== myUserId.current && (
                        <button
                          className="chat-dm-btn"
                          onClick={async () => {
                            const room = await getOrCreateDirectRoom(m.id);
                            setRooms((prev) => {
                              if (prev.some((r) => r.id === room.id)) return prev;
                              return [room, ...prev];
                            });
                            openRoom(room.id);
                          }}
                          title="Direct message"
                        >DM</button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="chat-input-bar">
              <textarea
                className="chat-input"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Type a message..."
                rows={1}
              />
              <button className="chat-send-btn" onClick={handleSend} disabled={!input.trim() || !connected}>
                Send
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// ─── New Chat Panel ───────────────────────────────────────────────────────────

type Mode = "menu" | "section" | "dm" | "group";

function NewChatPanel({ onCreated }: { onCreated: (room: ChatRoom) => void }) {
  const [mode, setMode] = useState<Mode>("menu");

  return (
    <div className="chat-new-panel">
      {mode === "menu" && (
        <>
          <p className="chat-new-label">Start a new chat</p>
          <button className="chat-section-btn" onClick={() => setMode("section")}>📚 Section Chat</button>
          <button className="chat-section-btn" onClick={() => setMode("dm")}>💬 Direct Message</button>
          <button className="chat-section-btn" onClick={() => setMode("group")}>👥 Create Group</button>
        </>
      )}
      {mode === "section" && <SectionPanel onCreated={onCreated} onBack={() => setMode("menu")} />}
      {mode === "dm" && <UserPickerPanel onCreated={onCreated} onBack={() => setMode("menu")} />}
      {mode === "group" && <GroupCreatePanel onCreated={onCreated} onBack={() => setMode("menu")} />}
    </div>
  );
}

// ─── Section Panel ────────────────────────────────────────────────────────────

function SectionPanel({ onCreated, onBack }: { onCreated: (room: ChatRoom) => void; onBack: () => void }) {
  const [sections, setSections] = useState<{ id: number; name: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const role = getUserRole();

  useEffect(() => {
    async function load() {
      try {
        if (role === "STUDENT") {
          const enrollments = await fetchStudentEnrollments();
          setSections(enrollments.map((e) => ({ id: e.sectionId, name: `${e.subjectCode} — ${e.subjectName}` })));
        } else if (role === "PROFESSOR") {
          const teacherSections = await fetchTeacherSections();
          setSections(teacherSections.map((s) => ({ id: s.id, name: `${s.subjectCode} — ${s.subjectName}` })));
        }
      } catch { /* ignore */ } finally { setLoading(false); }
    }
    load();
  }, [role]);

  async function handleCreate(sectionId: number) {
    try {
      const room = await getOrCreateSectionRoom(sectionId);
      onCreated(room);
    } catch (err) {
      alert(err instanceof Error ? err.message : "Failed to create room");
    }
  }

  return (
    <>
      <div className="chat-new-back" onClick={onBack}>← Back</div>
      <p className="chat-new-label">Open section chat</p>
      {loading && <p className="chat-empty">Loading sections...</p>}
      {!loading && sections.length === 0 && <p className="chat-empty">No sections available</p>}
      {sections.map((s) => (
        <button key={s.id} className="chat-section-btn" onClick={() => handleCreate(s.id)}>{s.name}</button>
      ))}
    </>
  );
}

// ─── User Picker Panel (DM) ───────────────────────────────────────────────────

function UserPickerPanel({ onCreated, onBack }: { onCreated: (room: ChatRoom) => void; onBack: () => void }) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<ChatUserResult[]>([]);
  const [searching, setSearching] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setSearching(true);
      try {
        const data = await searchChatUsers(q);
        setResults(data);
      } catch { /* ignore */ } finally { setSearching(false); }
    }, 300);
  }, [q]);

  async function handleDm(userId: number) {
    try {
      const room = await getOrCreateDirectRoom(userId);
      onCreated(room);
    } catch (err) {
      alert(err instanceof Error ? err.message : "Failed to open DM");
    }
  }

  return (
    <>
      <div className="chat-new-back" onClick={onBack}>← Back</div>
      <p className="chat-new-label">Direct message</p>
      <input
        className="chat-search-input"
        type="text"
        placeholder="Search by name..."
        value={q}
        onChange={(e) => setQ(e.target.value)}
        autoFocus
      />
      {searching && <p className="chat-empty">Searching...</p>}
      <div className="chat-user-results">
        {results.map((u) => (
          <button key={u.id} className="chat-user-result-btn" onClick={() => handleDm(u.id)}>
            <span className={`chat-user-avatar ${u.role === "PROFESSOR" ? "prof" : "student"}`}>
              {u.name.split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2)}
            </span>
            <div className="chat-user-info">
              <span className="chat-user-name">{u.name}</span>
              <span className="chat-user-role">{u.role}</span>
            </div>
          </button>
        ))}
        {!searching && q && results.length === 0 && <p className="chat-empty">No users found.</p>}
        {!searching && !q && results.length === 0 && <p className="chat-empty">Type a name to search.</p>}
      </div>
    </>
  );
}

// ─── Group Create Panel ───────────────────────────────────────────────────────

function GroupCreatePanel({ onCreated, onBack }: { onCreated: (room: ChatRoom) => void; onBack: () => void }) {
  const [groupName, setGroupName] = useState("");
  const [q, setQ] = useState("");
  const [results, setResults] = useState<ChatUserResult[]>([]);
  const [selected, setSelected] = useState<ChatUserResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [creating, setCreating] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      if (!q) { setResults([]); return; }
      setSearching(true);
      try {
        const data = await searchChatUsers(q);
        setResults(data.filter((u) => !selected.some((s) => s.id === u.id)));
      } catch { /* ignore */ } finally { setSearching(false); }
    }, 300);
  }, [q, selected]);

  function toggleUser(u: ChatUserResult) {
    setSelected((prev) =>
      prev.some((s) => s.id === u.id) ? prev.filter((s) => s.id !== u.id) : [...prev, u]
    );
    setQ("");
  }

  async function handleCreate() {
    if (!groupName.trim() || selected.length === 0) return;
    setCreating(true);
    try {
      const room = await createGroupRoom(groupName.trim(), selected.map((u) => u.id));
      onCreated(room);
    } catch (err) {
      alert(err instanceof Error ? err.message : "Failed to create group");
    } finally { setCreating(false); }
  }

  return (
    <>
      <div className="chat-new-back" onClick={onBack}>← Back</div>
      <p className="chat-new-label">Create group chat</p>

      <input
        className="chat-search-input"
        type="text"
        placeholder="Group name..."
        value={groupName}
        onChange={(e) => setGroupName(e.target.value)}
      />

      {selected.length > 0 && (
        <div className="chat-selected-members">
          {selected.map((u) => (
            <span key={u.id} className="chat-selected-chip" onClick={() => toggleUser(u)}>
              {u.name.split(" ")[0]} ×
            </span>
          ))}
        </div>
      )}

      <input
        className="chat-search-input"
        type="text"
        placeholder="Add members by name..."
        value={q}
        onChange={(e) => setQ(e.target.value)}
      />

      {searching && <p className="chat-empty">Searching...</p>}
      <div className="chat-user-results">
        {results.map((u) => (
          <button key={u.id} className="chat-user-result-btn" onClick={() => toggleUser(u)}>
            <span className={`chat-user-avatar ${u.role === "PROFESSOR" ? "prof" : "student"}`}>
              {u.name.split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2)}
            </span>
            <div className="chat-user-info">
              <span className="chat-user-name">{u.name}</span>
              <span className="chat-user-role">{u.role}</span>
            </div>
          </button>
        ))}
        {!searching && q && results.length === 0 && <p className="chat-empty">No users found.</p>}
      </div>

      <button
        className="chat-create-group-btn"
        onClick={handleCreate}
        disabled={!groupName.trim() || selected.length === 0 || creating}
      >
        {creating ? "Creating..." : `Create Group (${selected.length + 1} members)`}
      </button>
    </>
  );
}
