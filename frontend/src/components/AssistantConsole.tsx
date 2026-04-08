import { ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { ApiError } from "../lib/api";

type AssistantReply = {
  answer: string;
  model: string;
  generatedAt: string;
  scheduleRecommendation?: unknown;
};

type ConversationItem = {
  id: string;
  role: "user" | "assistant";
  text: string;
  meta?: string;
  payload?: AssistantReply;
};

type AssistantConsoleProps = {
  title: string;
  subtitle?: string;
  eyebrow?: string;
  heroTitle?: string;
  heroDescription?: string;
  placeholder: string;
  suggestions?: string[];
  welcomeMessage: string;
  ask: (message: string) => Promise<AssistantReply>;
  summary?: ReactNode;
  renderAssistantExtra?: (reply: AssistantReply) => ReactNode;
};

function formatMeta(reply: AssistantReply): string {
  return `${reply.model} | ${reply.generatedAt.slice(0, 19).replace("T", " ")}`;
}

export default function AssistantConsole({
  title,
  subtitle,
  eyebrow,
  heroTitle,
  heroDescription,
  placeholder,
  suggestions = [],
  welcomeMessage,
  ask,
  summary,
  renderAssistantExtra
}: AssistantConsoleProps) {
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const [conversation, setConversation] = useState<ConversationItem[]>([
    {
      id: "welcome",
      role: "assistant",
      text: welcomeMessage,
      meta: "KBTU AI Assistant"
    }
  ]);

  const canSend = useMemo(() => draft.trim().length > 0 && !sending, [draft, sending]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [conversation, sending]);

  async function handleSend(customMessage?: string) {
    const message = (customMessage ?? draft).trim();
    if (!message || sending) {
      return;
    }

    setConversation((prev) => [
      ...prev,
      {
        id: `u-${Date.now()}`,
        role: "user",
        text: message
      }
    ]);
    setDraft("");
    setSending(true);
    setError(null);

    try {
      const reply = await ask(message);
      setConversation((prev) => [
        ...prev,
        {
          id: `a-${Date.now()}`,
          role: "assistant",
          text: reply.answer,
          meta: formatMeta(reply),
          payload: reply
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
          <h2>{title}</h2>
          {subtitle ? <p className="muted">{subtitle}</p> : null}
        </div>
      </header>

      {(eyebrow || heroTitle || heroDescription || suggestions.length > 0 || summary) ? (
        <section className="card assistant-hero-card">
          {(eyebrow || heroTitle || heroDescription || suggestions.length > 0) ? (
            <div className="assistant-hero">
              {(eyebrow || heroTitle || heroDescription) ? (
                <div>
                  {eyebrow ? <span className="assistant-eyebrow">{eyebrow}</span> : null}
                  {heroTitle ? <h3>{heroTitle}</h3> : null}
                  {heroDescription ? <p className="muted">{heroDescription}</p> : null}
                </div>
              ) : null}
              {suggestions.length > 0 ? (
                <div className="assistant-suggestion-list">
                  {suggestions.map((prompt) => (
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
              ) : null}
            </div>
          ) : null}
          {summary ? <div className="assistant-summary-grid">{summary}</div> : null}
        </section>
      ) : null}

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
              {item.role === "assistant" && item.payload && renderAssistantExtra ? (
                <div className="assistant-message-extra">{renderAssistantExtra(item.payload)}</div>
              ) : null}
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
          <div ref={messagesEndRef} />
        </div>

        <div className="assistant-composer">
          <label className="assistant-composer-label">
            <span>Your question</span>
            <textarea
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder={placeholder}
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
