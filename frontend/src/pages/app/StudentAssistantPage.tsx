import { useEffect, useState } from "react";
import AssistantConsole from "../../components/AssistantConsole";
import {
  ApiError,
  askStudentAssistant,
  fetchStudentPlanner,
  fetchStudentRiskDashboard,
  fetchStudentWorkflows
} from "../../lib/api";
import type { StudentPlannerDashboard, StudentRiskDashboard } from "../../types/student";
import type { WorkflowOverview } from "../../types/common";

const SUGGESTIONS = [
  "Am I at risk because of attendance right now?",
  "How much do I need on the final to get a B in my weakest course?",
  "Why is my current GPA lower than expected?",
  "Which courses should I focus on first this semester?"
];

function formatRiskLevel(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

export default function StudentAssistantPage() {
  const [risk, setRisk] = useState<StudentRiskDashboard | null>(null);
  const [planner, setPlanner] = useState<StudentPlannerDashboard | null>(null);
  const [workflows, setWorkflows] = useState<WorkflowOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [riskPayload, plannerPayload, workflowsPayload] = await Promise.all([
          fetchStudentRiskDashboard(),
          fetchStudentPlanner(),
          fetchStudentWorkflows()
        ]);
        if (!cancelled) {
          setRisk(riskPayload);
          setPlanner(plannerPayload);
          setWorkflows(workflowsPayload);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Failed to load student assistant context");
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
      title="Student AI Assistant"
      subtitle="Ask about attendance, grades, GPA, workflows, and what score you still need on the final."
      eyebrow="Read-only academic helper"
      heroTitle="Your data-aware study assistant"
      heroDescription="The assistant reads your current portal data and explains results in plain language. It does not change grades, attendance, or registrations."
      placeholder="For example: how much do I need on the final in Calculus II to finish the course with a B?"
      suggestions={SUGGESTIONS}
      welcomeMessage="I can help with attendance risk, attestation totals, final score planning, GPA questions, and your active workflows."
      ask={askStudentAssistant}
      summary={
        error ? (
          <div className="assistant-summary-card">
            <span className="assistant-summary-label">Status</span>
            <strong>Could not load student snapshot</strong>
            <p className="muted">{error}</p>
          </div>
        ) : loading || !risk || !planner ? (
          <div className="assistant-summary-card">
            <span className="assistant-summary-label">Loading</span>
            <strong>Preparing academic context</strong>
            <p className="muted">Pulling your attendance, GPA, planner, and workflow data.</p>
          </div>
        ) : (
          <>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Overall risk</span>
              <strong>{formatRiskLevel(risk.level)}</strong>
              <p className="muted">{risk.riskScore.toFixed(1)} / 100 academic risk score.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Attendance</span>
              <strong>{risk.attendanceRate.toFixed(1)}%</strong>
              <p className="muted">Across all tracked courses in the current view.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Published GPA</span>
              <strong>{risk.publishedGpa.toFixed(2)}</strong>
              <p className="muted">{planner.courses.length} courses available in the planner.</p>
            </div>
            <div className="assistant-summary-card">
              <span className="assistant-summary-label">Open workflows</span>
              <strong>{workflows?.items.length ?? 0}</strong>
              <p className="muted">Requests, FX, mobility, clearance, and registration actions.</p>
            </div>
          </>
        )
      }
    />
  );
}
