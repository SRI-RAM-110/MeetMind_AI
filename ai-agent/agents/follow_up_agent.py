"""
MeetMind AI - Follow-Up Agent
Responsibility:
- Builds a structured post-meeting follow-up plan from validated decisions and action items.
- Identifies unresolved questions and confirmations needed before the next milestone.
- Uses ONLY stated meeting dates and deadlines.
- Sets next_meeting_date to 'Not specified' if omitted.
"""

import re
import logging
from typing import Dict, Any, List, Optional

from .base_agent import BaseAgent, AgentExecutionError
from .schemas import FollowUpOutput, DecisionOutput, ActionItemOutput, TranscriptOutput

logger = logging.getLogger(__name__)

FOLLOW_UP_SYSTEM_PROMPT = """You are the Follow-Up Agent in MeetMind AI's multi-agent business meeting intelligence system.
Your sole responsibility is to synthesize a structured post-meeting follow-up plan based on validated decisions, action items, and open discussion items.

STRICT ACCURACY RULES:
1. Overview: Provide a concise executive overview of the follow-up roadmap.
2. Steps: Create a sequential, practical series of next steps based directly on the validated decisions and action items.
3. Checkpoints: Suggest reasonable AI-recommended checkpoints based on confirmed tasks (clearly intended as AI guidance).
4. Upcoming Review Meetings: Extract any explicitly confirmed future review meetings. If none, return [].
5. Pending Clarifications: Identify missing information that prevents execution or scheduling (e.g. unassigned tasks, missing deadlines).
6. Unresolved Questions: Include any unanswered questions or pending approvals left open in discussion.
7. Next Meeting Date:
   - Extract the next meeting date/time ONLY IF explicitly stated in the transcript text.
   - If no next meeting is stated or scheduled, you MUST set "next_meeting_date": "Not specified".
   - NEVER invent, assume, or fabricate a future meeting date.

OUTPUT FORMAT:
You MUST respond with a valid JSON object matching this schema:
{
  "overview": "Execution roadmap for MeetMind AI MVP launch across backend and frontend deliverables.",
  "steps": [
    "Bob implements Spring Boot backend by Oct 14",
    "Dana prepares Vite React UI by Oct 12",
    "Team reviews implementation and integration tests"
  ],
  "checkpoints": [
    "Mid-week sync on Oct 11 to check backend schema definitions",
    "Pre-release integration check on Oct 15"
  ],
  "upcoming_review_meetings": [
    "Oct 16 at 3:00 PM EST - Architecture Review Sync"
  ],
  "pending_clarifications": [
    "Task 'Integration test containers' has no assigned owner"
  ],
  "unresolved_questions": [
    "Confirm whether Redis caching will be needed before v1.0"
  ],
  "next_meeting_date": "Oct 16 at 3:00 PM EST"
}
Do not include any commentary outside the JSON object.
"""


class FollowUpAgent(BaseAgent):
    def __init__(self, client: Optional[Any] = None, model: str = "llama-3.3-70b-versatile"):
        super().__init__(name="Follow-Up Agent", client=client, model=model)

    def run(
        self,
        decision_data: DecisionOutput,
        action_item_data: ActionItemOutput,
        meeting_notes: str,
        transcript_data: Optional[TranscriptOutput] = None
    ) -> FollowUpOutput:
        """
        Synthesizes the follow-up plan from decisions and action items.
        """
        if self.client:
            decisions_summary = "\n".join([f"- {d.decision} (Evidence: {d.evidence})" for d in decision_data.decisions]) or "No explicit decisions"
            actions_summary = "\n".join([f"- {a.owner}: {a.task} [Due: {a.deadline}, Priority: {a.priority}]" for a in action_item_data.action_items]) or "No action items"
            unresolved_input = "\n".join([f"- {q}" for q in decision_data.unresolved_questions]) or "None"

            user_prompt = (
                f"Construct the post-meeting follow-up plan.\n\n"
                f"Validated Decisions:\n{decisions_summary}\n\n"
                f"Validated Action Items:\n{actions_summary}\n\n"
                f"Open Questions Identified So Far:\n{unresolved_input}\n\n"
                f"Original Meeting Notes:\n---\n{meeting_notes}\n---"
            )

            raw_json = self._call_llm(FOLLOW_UP_SYSTEM_PROMPT, user_prompt)
            try:
                next_date = str(raw_json.get("next_meeting_date", "Not specified")).strip()
                if not next_date or next_date.lower() in ["none", "null", "n/a", "tbd", "unknown"]:
                    next_date = "Not specified"

                overview = raw_json.get("overview")
                steps = raw_json.get("steps") or []
                checkpoints = raw_json.get("checkpoints") or []
                upcoming_reviews = raw_json.get("upcoming_review_meetings") or []
                pending = raw_json.get("pending_clarifications") or []
                unresolved = raw_json.get("unresolved_questions") or []

                output = FollowUpOutput(
                    overview=str(overview).strip() if overview else None,
                    steps=[str(s).strip() for s in steps if str(s).strip()],
                    checkpoints=[str(c).strip() for c in checkpoints if str(c).strip()],
                    upcoming_review_meetings=[str(r).strip() for r in upcoming_reviews if str(r).strip()],
                    pending_clarifications=[str(p).strip() for p in pending if str(p).strip()],
                    unresolved_questions=[str(u).strip() for u in unresolved if str(u).strip()],
                    next_meeting_date=next_date
                )
                logger.info("[%s] Built follow-up plan with %d steps, %d checkpoints, next meeting: %s",
                            self.name, len(output.steps), len(output.checkpoints), output.next_meeting_date)
                return output
            except Exception as e:
                raise AgentExecutionError(self.name, f"Schema validation error on follow-up plan: {e}", e)
        else:
            return self._run_deterministic_fallback(decision_data, action_item_data, meeting_notes)

    def _run_deterministic_fallback(
        self,
        decision_data: DecisionOutput,
        action_item_data: ActionItemOutput,
        notes: str
    ) -> FollowUpOutput:
        """
        Deterministic follow-up plan builder for offline testing.
        """
        steps = []
        pending_clarifications = []
        for item in action_item_data.action_items:
            owner_label = f"[{item.owner}]" if item.owner != "Unassigned" else "[Unassigned Team]"
            deadline_label = f" (by {item.deadline})" if item.deadline != "Not specified" else ""
            steps.append(f"{owner_label} {item.task}{deadline_label}")
            if item.owner == "Unassigned":
                pending_clarifications.append(f"Task '{item.task}' requires an assigned owner.")
            if item.deadline == "Not specified":
                pending_clarifications.append(f"Task '{item.task}' requires a confirmed deadline.")

        if not steps and decision_data.decisions:
            for d in decision_data.decisions:
                steps.append(f"Execute agreed decision: {d.decision}")

        # Check for next meeting date pattern
        next_meeting_date = "Not specified"
        upcoming_reviews = []
        for line in notes.splitlines():
            lower = line.lower().strip()
            if any(lower.startswith(k) for k in ["next meeting:", "next sync:", "follow up meeting:"]):
                parts = re.split(r"[:\-]", line, maxsplit=1)
                if len(parts) > 1:
                    next_meeting_date = parts[1].strip()
                    upcoming_reviews.append(line.strip())
                break

        checkpoints = []
        if steps:
            checkpoints.append("Initial progress checkpoint within 48-72 hours")
            checkpoints.append("Final deliverable verification checkpoint prior to next milestone")

        unresolved = list(decision_data.unresolved_questions)

        return FollowUpOutput(
            overview="Structured post-meeting execution roadmap for agreed deliverables.",
            steps=steps,
            checkpoints=checkpoints,
            upcoming_review_meetings=upcoming_reviews,
            pending_clarifications=pending_clarifications,
            unresolved_questions=unresolved,
            next_meeting_date=next_meeting_date
        )
