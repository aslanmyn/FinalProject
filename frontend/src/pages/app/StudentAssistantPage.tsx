import { useEffect, useState } from "react";
import AssistantConsole from "../../components/AssistantConsole";
import {
  ApiError,
  askStudentAssistant,
  fetchStudentPlanner,
  fetchStudentRiskDashboard,
  fetchStudentScheduleDemo,
  fetchStudentWorkflows
} from "../../lib/api";
import type {
  StudentAssistantScheduleRecommendation,
  StudentPlannerDashboard,
  StudentRiskDashboard
} from "../../types/student";
import type { WorkflowOverview } from "../../types/common";

const SUGGESTIONS = [
  "Am I at risk because of attendance right now?",
  "How much do I need on the final to get a B in my weakest course?",
  "Why is my current GPA lower than expected?",
  "Which courses should I focus on first this semester?",
  "Make my next semester schedule after 12 and avoid Friday if possible."
];

function formatRiskLevel(value: string): string {
  return value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatDay(day: string): string {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

function renderScheduleRecommendation(reply: { scheduleRecommendation?: unknown }) {
  const recommendation = reply.scheduleRecommendation as StudentAssistantScheduleRecommendation | null | undefined;
  if (!recommendation) {
    return null;
  }

  const dayOrder = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

  return (
    <div className="assistant-schedule-card">
      <div className="assistant-schedule-header">
        <div>
          <strong>{recommendation.semesterName || "Schedule recommendation"}</strong>
          <p className="muted">{recommendation.summary || "AI-generated next semester suggestion"}</p>
        </div>
        <span className={`badge ${recommendation.feasible ? "" : "badge-neutral"}`}>
          {recommendation.partial ? "Partial plan" : recommendation.feasible ? "Feasible" : "Not feasible"}
        </span>
      </div>

      {recommendation.selectedSections.length > 0 ? (
        <div className="assistant-schedule-section-list">
          {recommendation.selectedSections.map((section) => (
            <article key={`${section.courseCode}-${section.sectionId}`} className="assistant-schedule-section">
              <div className="assistant-schedule-section-head">
                <strong>{section.courseCode}</strong>
                <span>Section #{section.sectionId}</span>
              </div>
              <p>{section.courseName || "Course"}</p>
              <p className="muted">{section.teacherName || "Teacher TBA"}</p>
              <div className="assistant-schedule-time-list">
                {section.meetingTimes.map((slot, index) => (
                  <span key={`${section.sectionId}-${slot.dayOfWeek}-${index}`} className="assistant-schedule-time-chip">
                    {formatDay(slot.dayOfWeek)} {slot.startTime.slice(0, 5)}-{slot.endTime.slice(0, 5)} {slot.room || ""}
                  </span>
                ))}
              </div>
            </article>
          ))}
        </div>
      ) : null}

      <div className="assistant-schedule-grid">
        {dayOrder.map((day) => {
          const items = recommendation.visualSchedule?.[day] ?? [];
          return (
            <div key={day} className="assistant-schedule-column">
              <span className="assistant-schedule-day">{formatDay(day)}</span>
              {items.length === 0 ? (
                <div className="assistant-schedule-empty">No classes</div>
              ) : (
                items.map((item, index) => (
                  <div key={`${day}-${item.courseCode}-${index}`} className="assistant-schedule-slot">
                    <strong>{item.courseCode}</strong>
                    <span>{item.startTime.slice(0, 5)}-{item.endTime.slice(0, 5)}</span>
                    <span>{item.room || "Room TBA"}</span>
                  </div>
                ))
              )}
            </div>
          );
        })}
      </div>

      {recommendation.warnings.length > 0 ? (
        <div className="assistant-schedule-notes">
          <span className="assistant-summary-label">Warnings</span>
          <ul>
            {recommendation.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}

async function askWithDemoFallback(message: string) {
  const normalizedMessage = message.toLowerCase();
  const isScheduleRequest =
    normalizedMessage.includes("schedule") ||
    normalizedMessage.includes("расписание") ||
    normalizedMessage.includes("demo schedule") ||
    normalizedMessage.includes("демо");

  try {
    const reply = await askStudentAssistant(message);
    // If the AI returned a quota error for a schedule request, fall back to demo
    if (isScheduleRequest && !reply.scheduleRecommendation && reply.model === "gemini-quota-limit") {
      try {
        return await fetchStudentScheduleDemo();
      } catch {
        return reply;
      }
    }
    return reply;
  } catch (err) {
    // If the real API failed and it was a schedule request, try demo
    if (isScheduleRequest) {
      return fetchStudentScheduleDemo();
    }
    throw err;
  }
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
      heroDescription="The assistant reads your current portal data and explains results in plain language. It can also ask AI to draft a conflict-aware next semester schedule from available section times."
      placeholder="For example: make my next semester schedule after 12 and avoid Friday if possible."
      suggestions={SUGGESTIONS}
      welcomeMessage="I can help with attendance risk, attestation totals, final score planning, GPA questions, your active workflows, and draft next semester schedule ideas."
      ask={askWithDemoFallback}
      renderAssistantExtra={renderScheduleRecommendation}
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
