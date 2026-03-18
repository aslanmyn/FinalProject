import { useEffect, useState } from "react";
import AssistantConsole from "../../components/AssistantConsole";
import { ApiError, askAdminAssistant, fetchAdminAnalytics, fetchAdminWorkflows } from "../../lib/api";
import type { AdminAnalyticsDashboard } from "../../types/admin";
import type { WorkflowOverview } from "../../types/common";

const SUGGESTIONS = [
  "Which sections are currently overloaded?",
  "Where is the biggest request backlog right now?",
  "Which faculties have the highest risk score?",
  "Summarize the students who need immediate attention."
];

export default function AdminAssistantPage() {
  const [analytics, setAnalytics] = useState<AdminAnalyticsDashboard | null>(null);
  const [workflows, setWorkflows] = useState<WorkflowOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [analyticsPayload, workflowsPayload] = await Promise.all([
          fetchAdminAnalytics(),
          fetchAdminWorkflows()
        ]);
        if (!cancelled) {
          setAnalytics(analyticsPayload);
          setWorkflows(workflowsPayload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load admin assistant context");
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
      title="Admin AI Assistant"
      subtitle="Ask about overloaded sections, request backlog, faculty risk, workflow queue, and operational hotspots."
      eyebrow="Read-only operations helper"
      heroTitle="Your institution-wide operations assistant"
      heroDescription="The assistant summarizes real backlog, risk, and workload data across the platform. It does not change academic, finance, or workflow records."
      placeholder="For example: Which sections are overloaded and where should the registrar intervene first?"
      suggestions={SUGGESTIONS}
      welcomeMessage="I can summarize faculty risk, workflow backlog, overloaded sections, request queues, and critical students that need admin attention."
      ask={askAdminAssistant}
      summary={
        error ? (
          <div className="assistant-summary-card">
            <span className="assistant-summary-label">Status</span>
            <strong>Could not load admin snapshot</strong>
            <p className="muted">{error}</p>
          </div>
        ) : loading || !analytics ? (
          <div className="assistant-summary-card">
            <span className="assistant-summary-label">Loading</span>
            <strong>Preparing admin context</strong>
            <p className="muted">Pulling backlog, faculty risk, and overloaded section data.</p>
          </div>
        ) : (
          <>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Critical students</span>
              <strong>{analytics.criticalStudents.length}</strong>
              <p className="muted">Students currently surfaced for urgent review.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Workflow queue</span>
              <strong>{workflows?.items.length ?? 0}</strong>
              <p className="muted">Open workflow items across requests, FX, mobility, and clearance.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Overloaded sections</span>
              <strong>{analytics.overloadedSections.length}</strong>
              <p className="muted">Sections already beyond healthy utilization threshold.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Open windows</span>
              <strong>{analytics.metrics.openWindows}</strong>
              <p className="muted">Registration windows currently active for the platform.</p>
            </div>
          </>
        )
      }
    />
  );
}
