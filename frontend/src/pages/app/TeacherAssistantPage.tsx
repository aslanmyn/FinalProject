import { useEffect, useState } from "react";
import AssistantConsole from "../../components/AssistantConsole";
import { ApiError, askTeacherAssistant, fetchTeacherRiskDashboard } from "../../lib/api";
import type { TeacherRiskDashboard } from "../../types/teacher";

const SUGGESTIONS = [
  "Which students are at risk right now?",
  "Which sections have the weakest attendance?",
  "What grades or finals are still unpublished?",
  "Summarize the sections that need my attention first."
];

function formatRiskLevel(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

export default function TeacherAssistantPage() {
  const [dashboard, setDashboard] = useState<TeacherRiskDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const payload = await fetchTeacherRiskDashboard();
        if (!cancelled) {
          setDashboard(payload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load teacher assistant context");
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
    <AssistantConsole
      title="Teacher AI Assistant"
      subtitle="Ask about section risk, attendance, grade publication, workload, and students who need attention."
      eyebrow="Read-only teaching helper"
      heroTitle="Your data-aware teaching assistant"
      heroDescription="The assistant reviews your live section data and helps you spot risk signals quickly. It does not change grades, attendance, notes, or publications."
      placeholder="For example: which students in my current sections need intervention first and why?"
      suggestions={SUGGESTIONS}
      welcomeMessage="I can summarize section health, highlight students at risk, point out attendance issues, and show what still needs publication."
      ask={askTeacherAssistant}
      summary={
        error ? (
          <div className="assistant-summary-card">
            <span className="assistant-summary-label">Status</span>
            <strong>Could not load teaching snapshot</strong>
            <p className="muted">{error}</p>
          </div>
        ) : loading || !dashboard ? (
          <div className="assistant-summary-card">
            <span className="assistant-summary-label">Loading</span>
            <strong>Preparing teaching context</strong>
            <p className="muted">Pulling section risk, pending grade changes, and publication backlog.</p>
          </div>
        ) : (
          <>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Sections needing attention</span>
              <strong>{dashboard.sectionsNeedingAttention}</strong>
              <p className="muted">Sections currently outside a stable risk band.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Students at risk</span>
              <strong>{dashboard.atRiskStudents}</strong>
              <p className="muted">Students currently surfaced by the risk model.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Pending grade changes</span>
              <strong>{dashboard.pendingGradeChanges}</strong>
              <p className="muted">Requests still waiting for review or action.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Unpublished finals</span>
              <strong>{dashboard.unpublishedFinals}</strong>
              <p className="muted">Final results still not visible to students.</p>
            </div>
          </>
        )
      }
    />
  );
}
