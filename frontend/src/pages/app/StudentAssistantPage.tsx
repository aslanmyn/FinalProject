import { useMemo, useState } from "react";
import { ApiError, askStudentAssistant } from "../../lib/api";

type ConversationItem = {
  id: string;
  role: "user" | "assistant";
  text: string;
  meta?: string;
};

const SUGGESTIONS = [
  "У меня есть риск по посещаемости?",
  "Сколько мне нужно набрать на final по Calculus II, чтобы получить 80?",
  "Кратко разберись по моему текущему семестру.",
  "Какие у меня ближайшие экзамены?"
];

export default function StudentAssistantPage() {
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conversation, setConversation] = useState<ConversationItem[]>([
    {
      id: "welcome",
      role: "assistant",
      text: "Я могу помочь по посещаемости, аттестациям, нужному баллу на final, GPA и ближайшим экзаменам. Спроси простым языком.",
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
      const reply = await askStudentAssistant(message);
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
          <h2>Student AI Assistant</h2>
          <p className="muted">Ask about attendance, grades, exams, and what score you still need on the final.</p>
        </div>
      </header>

      <section className="card assistant-hero-card">
        <div className="assistant-hero">
          <div>
            <span className="assistant-eyebrow">Read-only academic helper</span>
            <h3>Your data-aware study assistant</h3>
            <p className="muted">
              The assistant uses your current portal data and explains results in plain language. It does not change grades,
              attendance, or registrations.
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
              placeholder="Например: сколько мне нужно на final по CSCI2107, чтобы получить 80?"
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
