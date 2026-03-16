import { useMemo, useState } from "react";
import { ApiError, askTeacherAssistant } from "../../lib/api";

type ConversationItem = {
  id: string;
  role: "user" | "assistant";
  text: string;
  meta?: string;
};

const SUGGESTIONS = [
  "Which students are at risk right now?",
  "Which sections have the weakest attendance?",
  "What grades or finals are still unpublished?",
  "Summarize my current teaching workload."
];

export default function TeacherAssistantPage() {
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conversation, setConversation] = useState<ConversationItem[]>([
    {
      id: "welcome",
      role: "assistant",
      text: "I can help summarize your sections, highlight students at risk, point out attendance issues, and show what still needs publication.",
      meta: "KBTU AI Assistant"
    }
  ]);

  const canSend = useMemo(() => draft.trim().length > 0 && !sending, [draft, sending]);

  async function handleSend(customMessage?: string) {
    const message = (customMessage ?? draft).trim();
    if (!message || sending) return;

    const userItem: ConversationItem = {
      id: `u-${Date.now()}`,
      role: "user",
      text: message
    };

    setConversation((prev) => [...prev, userItem]);
    setDraft("");
    setSending(true);
    setError(null);

    try {
      const reply = await askTeacherAssistant(message);
      setConversation((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: "assistant",
          text: reply.answer,
          meta: `${reply.model} • ${reply.generatedAt.slice(0, 19).replace("T", " ")}`
        }
      ]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to get assistant reply");
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="screen app-screen assistant-page">
      <header className="topbar">
        <div>
          <h2>Teacher AI Assistant</h2>
          <p className="muted">Ask about section risk, attendance, grade publication, and workload across your courses.</p>
        </div>
      </header>

      <section className="card assistant-hero-card">
        <div className="assistant-hero">
          <div>
            <span className="assistant-eyebrow">Read-only teaching helper</span>
            <h3>Your data-aware teaching assistant</h3>
            <p className="muted">
              The assistant reviews your actual section data and helps you spot problems faster. It does not change grades,
              attendance, notes, or publications.
            </p>
          </div>
          <div className="assistant-suggestion-list">
            {SUGGESTIONS.map((prompt) => (
              <button
                key={prompt}
                type="button"
                className="assistant-suggestion"
                onClick={() => void handleSend(prompt)}
                disabled={sending}
              >
                {prompt}
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="card assistant-chat-card">
        <div className="assistant-messages">
          {conversation.map((item) => (
            <article
              key={item.id}
              className={`assistant-message ${item.role === "user" ? "assistant-message-user" : "assistant-message-ai"}`}
            >
              <div className="assistant-message-head">
                <strong>{item.role === "user" ? "You" : "Assistant"}</strong>
                {item.meta ? <span>{item.meta}</span> : null}
              </div>
              <div className="assistant-message-body">
                {item.text.split("\n").map((line, index) => (
                  <p key={`${item.id}-${index}`}>{line}</p>
                ))}
              </div>
            </article>
          ))}

          {sending ? (
            <article className="assistant-message assistant-message-ai">
              <div className="assistant-message-head">
                <strong>Assistant</strong>
                <span>Generating answer...</span>
              </div>
              <div className="assistant-typing">
                <span />
                <span />
                <span />
              </div>
            </article>
          ) : null}
        </div>

        <div className="assistant-composer">
          <label className="assistant-composer-label">
            <span>Your question</span>
            <textarea
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="For example: Which students in my current sections need attention first?"
              rows={4}
              maxLength={2000}
            />
          </label>
          <div className="assistant-composer-footer">
            <small className="muted">{draft.length} / 2000</small>
            <button type="button" onClick={() => void handleSend()} disabled={!canSend}>
              {sending ? "Thinking..." : "Ask assistant"}
            </button>
          </div>
          {error ? <p className="error">{error}</p> : null}
        </div>
      </section>
    </div>
  );
}
