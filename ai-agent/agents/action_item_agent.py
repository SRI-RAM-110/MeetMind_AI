"""
MeetMind AI - Action Item Agent
Responsibility:
- Extracts tasks, deliverables, and commitments from meeting text.
- Returns task, owner, deadline, status, and supporting verbatim evidence.
- Uses 'Unassigned' for missing or ambiguous owners.
- Uses 'Not specified' for missing deadlines.
- Never invents assignments or dates.
- Status defaults to 'Pending'.
"""

import re
import logging
from typing import Dict, Any, List, Optional

from .base_agent import BaseAgent, AgentExecutionError
from .schemas import ActionItemOutput, ActionItem, TranscriptOutput

logger = logging.getLogger(__name__)

ACTION_ITEM_SYSTEM_PROMPT = """You are the Action Item Agent in MeetMind AI's multi-agent business meeting intelligence system.
Your sole responsibility is to extract concrete action items, commitments, deliverables, and responsibilities.

CRITICAL ACCURACY RULES (MANDATORY):
1. Task / Title / Description:
   - Provide a concise task title and actionable description.
   - Include a stable task_id like "TASK-01", "TASK-02", etc.
2. Owner & Role:
   - Must be the explicitly named individual or team in the text.
   - If no specific owner is named, you MUST set "owner": "Unassigned" and "needs_confirmation": true.
   - Extract owner_role (e.g., "Backend Engineer", "Frontend Developer", "Product Lead") ONLY IF stated or strongly implied by task context; otherwise null.
   - NEVER invent or guess a person's real name.
3. Deadline:
   - Must be the explicit date, day, or timeframe stated in the notes.
   - If no deadline is stated, set "deadline": "Not specified" and "needs_confirmation": true.
   - If relative deadline like "by Friday" is ambiguous, retain original wording and set "needs_confirmation": true.
4. Priority & Status:
   - Priority: "High", "Medium", or "Low" based on urgency stated in text. Default: "Medium".
   - Status: Default status is "Pending". Only use "Completed" or "In Progress" if explicitly stated as such.
5. Dependencies:
   - List any prerequisite tasks or prerequisites mentioned in the discussion (e.g. ["TASK-01"]). Default: [].
6. Evidence & Timestamps:
   - Include verbatim evidence sentence or phrase from transcript.
   - If transcript has timestamps (e.g. "[10:14]"), extract into "source_timestamp", else null.
   - Confidence: estimated confidence score between 0.5 and 1.0.

OUTPUT FORMAT:
You MUST respond with a valid JSON object matching this schema:
{
  "action_items": [
    {
      "task_id": "TASK-01",
      "task": "Implement Spring Boot REST controllers and MongoDB repositories",
      "title": "Build Spring Boot Backend & Mongo Repositories",
      "description": "Implement the Spring Boot REST controllers and MongoDB repositories",
      "owner": "Bob",
      "owner_role": "Backend Engineer",
      "deadline": "Oct 14",
      "priority": "High",
      "status": "Pending",
      "dependencies": [],
      "evidence": "Bob will implement the Spring Boot REST controllers and MongoDB repositories by Oct 14.",
      "source_timestamp": null,
      "confidence": 0.95,
      "needs_confirmation": false
    }
  ]
}
Do not include any commentary outside the JSON object.
"""


class ActionItemAgent(BaseAgent):
    def __init__(self, client: Optional[Any] = None, model: str = "llama-3.3-70b-versatile"):
        super().__init__(name="Action Item Agent", client=client, model=model)

    def run(self, transcript_data: TranscriptOutput, meeting_notes: str) -> ActionItemOutput:
        """
        Executes action item extraction and sanitization against accuracy rules.
        """
        if not meeting_notes or not meeting_notes.strip():
            raise AgentExecutionError(self.name, "Meeting notes cannot be empty.")

        if self.client:
            user_prompt = (
                f"Extract all action items from the meeting transcript following the accuracy rules strictly.\n\n"
                f"Transcript Participants: {', '.join(transcript_data.participants) if transcript_data.participants else 'None explicitly stated'}\n\n"
                f"Raw Meeting Notes:\n---\n{meeting_notes}\n---"
            )
            raw_json = self._call_llm(ACTION_ITEM_SYSTEM_PROMPT, user_prompt)
            try:
                raw_items = raw_json.get("action_items", [])
                validated_items = self._sanitize_and_validate_items(raw_items)
                logger.info("[%s] Extracted %d validated action items", self.name, len(validated_items))
                return ActionItemOutput(action_items=validated_items)
            except Exception as e:
                raise AgentExecutionError(self.name, f"Schema validation error on action items: {e}", e)
        else:
            return self._run_deterministic_fallback(meeting_notes)

    def _sanitize_and_validate_items(self, raw_items: List[Any]) -> List[ActionItem]:
        """
        Post-processes extracted action items to strictly guarantee compliance
        with the Unassigned, Not specified, and Evidence rules.
        """
        validated: List[ActionItem] = []
        for idx, raw in enumerate(raw_items, 1):
            if not isinstance(raw, dict):
                continue

            task = str(raw.get("task", "")).strip()
            if not task:
                continue

            task_id = str(raw.get("task_id") or f"TASK-{idx:02d}").strip()
            title = str(raw.get("title") or (task[:60] + "..." if len(task) > 60 else task)).strip()
            description = str(raw.get("description") or task).strip()

            # Owner sanitization
            owner = str(raw.get("owner", "")).strip()
            needs_confirmation = bool(raw.get("needs_confirmation", False))
            if not owner or owner.lower() in ["", "none", "tbd", "unassigned", "unknown", "n/a", "no owner", "null"]:
                owner = "Unassigned"
                needs_confirmation = True

            owner_role = raw.get("owner_role")
            if owner_role and str(owner_role).strip().lower() in ["none", "null", "unknown", "n/a"]:
                owner_role = None

            # Deadline sanitization
            deadline = str(raw.get("deadline", "")).strip()
            if not deadline or deadline.lower() in ["", "none", "tbd", "not specified", "unknown", "n/a", "no deadline", "null"]:
                deadline = "Not specified"
                needs_confirmation = True

            # Priority
            priority = str(raw.get("priority", "Medium")).strip()
            if priority.capitalize() in ["High", "Medium", "Low"]:
                priority = priority.capitalize()
            else:
                priority = "Medium"

            # Status
            status = str(raw.get("status", "Pending")).strip() or "Pending"

            # Dependencies
            raw_deps = raw.get("dependencies", [])
            dependencies = [str(d).strip() for d in raw_deps if str(d).strip()] if isinstance(raw_deps, list) else []

            # Evidence
            evidence = str(raw.get("evidence", "")).strip()
            source_timestamp = raw.get("source_timestamp")
            if source_timestamp and str(source_timestamp).strip().lower() in ["none", "null"]:
                source_timestamp = None

            confidence = 1.0
            try:
                if "confidence" in raw and raw["confidence"] is not None:
                    confidence = float(raw["confidence"])
            except (ValueError, TypeError):
                confidence = 1.0

            validated.append(ActionItem(
                task_id=task_id,
                task=task,
                title=title,
                description=description,
                owner=owner,
                owner_role=str(owner_role).strip() if owner_role else None,
                deadline=deadline,
                priority=priority,
                status=status,
                dependencies=dependencies,
                evidence=evidence,
                source_timestamp=str(source_timestamp).strip() if source_timestamp else None,
                confidence=confidence,
                needs_confirmation=needs_confirmation
            ))

        return validated

    def _run_deterministic_fallback(self, notes: str) -> ActionItemOutput:
        """
        Deterministic extraction for offline testing.
        """
        lines = [line.strip() for line in notes.splitlines() if line.strip()]
        items = []

        idx = 1
        for line in lines:
            lower = line.lower()
            if any(lower.startswith(k) for k in ["action:", "action item:", "todo:", "task:"]) or " will " in lower:
                # Remove prefix
                cleaned_line = re.sub(r"^(action|action item|todo|task):\s*", "", line, flags=re.IGNORECASE).strip()
                
                # Check for owner pattern: e.g. "Bob will ..." or "Dana will ..."
                owner = "Unassigned"
                needs_conf = False
                owner_match = re.match(r"^([A-Z][a-z]+)\s+will\b", cleaned_line)
                if owner_match:
                    owner = owner_match.group(1)
                else:
                    needs_conf = True
                
                # Check for deadline pattern: e.g. "by Oct 14", "by Friday", "by 2026-10-14"
                deadline = "Not specified"
                deadline_match = re.search(r"\bby\s+([A-Za-z0-9\s,\-]+?)(?:\.|$)", cleaned_line, flags=re.IGNORECASE)
                if deadline_match:
                    deadline = deadline_match.group(1).strip()
                else:
                    needs_conf = True

                task_id = f"TASK-{idx:02d}"
                idx += 1
                task = cleaned_line
                items.append(ActionItem(
                    task_id=task_id,
                    task=task,
                    title=task[:60] + "..." if len(task) > 60 else task,
                    description=task,
                    owner=owner,
                    owner_role=None,
                    deadline=deadline,
                    priority="High" if "urgent" in lower or "critical" in lower else "Medium",
                    status="Pending",
                    dependencies=[],
                    evidence=line,
                    source_timestamp=None,
                    confidence=0.9 if owner != "Unassigned" else 0.7,
                    needs_confirmation=needs_conf
                ))

        return ActionItemOutput(action_items=items)
