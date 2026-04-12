import AssistantConsole from "../../components/AssistantConsole";
import {
  askStudentAssistant,
  fetchStudentScheduleDemo
} from "../../lib/api";
import type { StudentAssistantScheduleRecommendation } from "../../types/student";

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
        <span className={`badge ${recommendation.partial ? "badge-neutral" : recommendation.feasible ? "" : "badge-neutral"}`}>
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
    normalizedMessage.includes("распис") ||
    normalizedMessage.includes("demo schedule") ||
    normalizedMessage.includes("демо");

  try {
    const reply = await askStudentAssistant(message);
    if (isScheduleRequest && !reply.scheduleRecommendation && reply.model === "gemini-quota-limit") {
      try {
        return await fetchStudentScheduleDemo();
      } catch {
        return reply;
      }
    }
    return reply;
  } catch (err) {
    if (isScheduleRequest) {
      return fetchStudentScheduleDemo();
    }
    throw err;
  }
}

export default function StudentAssistantPage() {
  return (
    <AssistantConsole
      title="AI Assistant"
      placeholder="Ask me anything..."
      welcomeMessage="I can help with attendance risk, attestation totals, final score planning, GPA questions, your active workflows, and draft next semester schedule ideas."
      ask={askWithDemoFallback}
      renderAssistantExtra={renderScheduleRecommendation}
    />
  );
}
