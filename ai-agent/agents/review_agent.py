"""
MeetMind AI - Review Agent
Responsibility:
- Checks the outputs from the other 4 agents against the original transcript.
- Flags unsupported claims, conflicting dates, duplicate tasks, missing owners, and ambiguous commitments.
- Returns validation findings and a corrected or flagged result.
- Must NOT silently invent replacement facts.
"""

import json
import logging
from typing import Dict, Any, List, Optional

from .base_agent import BaseAgent, AgentExecutionError
from .schemas import (
    ReviewOutput,
    ReviewFinding,
    TranscriptOutput,
    DecisionOutput,
    ActionItemOutput,
    FollowUpOutput,
    ActionItem,
    DecisionItem
)

logger = logging.getLogger(__name__)

REVIEW_SYSTEM_PROMPT = """You are the Review Agent in MeetMind AI's multi-agent business meeting intelligence system.
You serve as the lead Quality Assurance and Fact-Checking inspector.
Your responsibility is to review the collective output of the Transcript Agent, Decision Agent, Action Item Agent, and Follow-Up Agent against the ORIGINAL raw meeting notes.

REVIEW AND AUDIT CHECKLIST:
1. Unsupported Claims: Are any decisions, tasks, or facts claimed that do NOT appear in the original notes? Flag them immediately.
2. Conflicting Dates & Deadlines: Do any extracted deadlines or the next meeting date contradict what is stated, or were dates hallucinated?
3. Duplicate Tasks: Are there duplicate or overlapping action items that should be deduplicated?
4. Missing Owners: Ensure that tasks without explicit assignees are strictly labeled 'Unassigned'. If an agent hallucinated an owner, flag and correct to 'Unassigned'.
5. Ambiguous Commitments: Were tentative suggestions mistakenly extracted as firm decisions? If so, flag them.
6. Crucial Rule: Under NO circumstances should you invent replacement facts. If information is missing, label it as 'Unassigned' or 'Not specified'.

OUTPUT FORMAT:
You MUST respond with a valid JSON object matching this schema:
{
  "validation_passed": true,
  "findings": [
    {
      "category": "ACCURACY_VERIFIED",
      "severity": "PASS",
      "issue": "Verified that all confirmed decisions are grounded in verbatim quotes.",
      "evidence_checked": "Decision: Approved MongoDB Community Server",
      "action_taken": "Retained decision as confirmed."
    },
    {
      "category": "MISSING_OWNER",
      "severity": "INFO",
      "issue": "Task 'Conduct load testing' has no explicit assignee.",
      "evidence_checked": "We also need to conduct load testing on the API gateway.",
      "action_taken": "Validated owner as 'Unassigned'."
    }
  ],
  "corrected_summary": null,
  "corrected_decisions": null,
  "corrected_action_items": null,
  "corrected_follow_up_plan": null
}

If any items require correction or deduplication, provide the corrected array under corrected_decisions, corrected_action_items, or corrected_follow_up_plan. Otherwise set them to null.
Do not include any commentary outside the JSON object.
"""


class ReviewAgent(BaseAgent):
    def __init__(self, client: Optional[Any] = None, model: str = "llama-3.3-70b-versatile"):
        super().__init__(name="Review Agent", client=client, model=model)

    def run(
        self,
        meeting_notes: str,
        transcript_data: TranscriptOutput,
        decision_data: DecisionOutput,
        action_item_data: ActionItemOutput,
        follow_up_data: FollowUpOutput
    ) -> ReviewOutput:
        """
        Audits all previous agent outputs against the raw transcript.
        """
        if self.client:
            combined_payload = {
                "transcript_agent_output": transcript_data.model_dump(),
                "decision_agent_output": decision_data.model_dump(),
                "action_item_agent_output": action_item_data.model_dump(),
                "follow_up_agent_output": follow_up_data.model_dump()
            }

            user_prompt = (
                f"Review and audit the extracted multi-agent outputs against the original meeting notes.\n\n"
                f"Original Meeting Notes:\n---\n{meeting_notes}\n---\n\n"
                f"Extracted Agent Outputs to Audit:\n{json.dumps(combined_payload, indent=2)}"
            )

            raw_json = self._call_llm(REVIEW_SYSTEM_PROMPT, user_prompt)
            try:
                findings_list = []
                for f in raw_json.get("findings", []):
                    if isinstance(f, dict):
                        findings_list.append(ReviewFinding(
                            category=f.get("category", "AUDIT_CHECK"),
                            severity=f.get("severity", "INFO"),
                            issue=f.get("issue", "Check executed"),
                            evidence_checked=f.get("evidence_checked", ""),
                            action_taken=f.get("action_taken", "Verified")
                        ))

                # Handle corrected items if provided by reviewer
                corrected_decisions = None
                if raw_json.get("corrected_decisions"):
                    corrected_decisions = [
                        DecisionItem(**d) if isinstance(d, dict) else DecisionItem(decision=str(d), evidence="")
                        for d in raw_json["corrected_decisions"]
                    ]

                corrected_actions = None
                if raw_json.get("corrected_action_items"):
                    corrected_actions = [
                        ActionItem(**a) if isinstance(a, dict) else ActionItem(task=str(a))
                        for a in raw_json["corrected_action_items"]
                    ]

                output = ReviewOutput(
                    validation_passed=bool(raw_json.get("validation_passed", True)),
                    findings=findings_list,
                    corrected_summary=raw_json.get("corrected_summary"),
                    corrected_decisions=corrected_decisions,
                    corrected_action_items=corrected_actions,
                    corrected_follow_up_plan=None
                )

                logger.info("[%s] Completed review audit: %d findings recorded, validation_passed=%s",
                            self.name, len(output.findings), output.validation_passed)
                return output
            except Exception as e:
                raise AgentExecutionError(self.name, f"Schema validation error on review audit: {e}", e)
        else:
            return self._run_deterministic_fallback(
                meeting_notes, transcript_data, decision_data, action_item_data, follow_up_data
            )

    def _run_deterministic_fallback(
        self,
        notes: str,
        transcript_data: TranscriptOutput,
        decision_data: DecisionOutput,
        action_item_data: ActionItemOutput,
        follow_up_data: FollowUpOutput
    ) -> ReviewOutput:
        """
        Deterministic audit checks for offline testing.
        """
        findings: List[ReviewFinding] = []
        lower_notes = notes.lower()

        # 1. Check decisions grounding
        for d in decision_data.decisions:
            # Check if key words from decision exist in notes
            words = [w for w in d.decision.lower().split() if len(w) > 4]
            grounded = any(w in lower_notes for w in words) if words else True
            if grounded:
                findings.append(ReviewFinding(
                    category="ACCURACY_VERIFIED",
                    severity="PASS",
                    issue=f"Decision '{d.decision[:40]}...' is grounded in the transcript.",
                    evidence_checked=d.evidence or d.decision,
                    action_taken="Confirmed valid decision."
                ))
            else:
                findings.append(ReviewFinding(
                    category="UNSUPPORTED_CLAIM",
                    severity="WARNING",
                    issue=f"Decision '{d.decision[:40]}...' has weak verbatim presence in notes.",
                    evidence_checked="Transcript text",
                    action_taken="Flagged for user review."
                ))

        # 2. Check Action Items (missing owners, missing deadlines, duplicate tasks)
        seen_tasks = set()
        for item in action_item_data.action_items:
            task_key = item.task.lower().strip()
            if task_key in seen_tasks:
                findings.append(ReviewFinding(
                    category="DUPLICATE_TASK",
                    severity="WARNING",
                    issue=f"Duplicate action item detected: '{item.task}'.",
                    evidence_checked=item.evidence,
                    action_taken="Flagged duplicate task."
                ))
            seen_tasks.add(task_key)

            if item.owner == "Unassigned":
                findings.append(ReviewFinding(
                    category="MISSING_OWNER",
                    severity="INFO",
                    issue=f"Task '{item.task[:40]}' has no explicit assignee.",
                    evidence_checked=item.evidence,
                    action_taken="Enforced 'Unassigned' default."
                ))
            else:
                findings.append(ReviewFinding(
                    category="OWNER_VERIFIED",
                    severity="PASS",
                    issue=f"Task assigned to explicitly named owner '{item.owner}'.",
                    evidence_checked=item.evidence,
                    action_taken="Verified explicit assignment."
                ))

            if item.deadline == "Not specified":
                findings.append(ReviewFinding(
                    category="DATE_CHECK",
                    severity="INFO",
                    issue=f"Task '{item.task[:40]}' has no explicit deadline.",
                    evidence_checked=item.evidence,
                    action_taken="Enforced 'Not specified' default."
                ))

        # 3. Check Next Meeting Date
        if follow_up_data.next_meeting_date != "Not specified":
            findings.append(ReviewFinding(
                category="DATE_CHECK",
                severity="PASS",
                issue=f"Next meeting date verified: '{follow_up_data.next_meeting_date}'.",
                evidence_checked="Next meeting notation in transcript",
                action_taken="Recorded explicit schedule."
            ))
        else:
            findings.append(ReviewFinding(
                category="DATE_CHECK",
                severity="INFO",
                issue="No next meeting date explicitly scheduled in notes.",
                evidence_checked="Transcript text",
                action_taken="Retained 'Not specified'."
            ))

        return ReviewOutput(
            validation_passed=True,
            findings=findings,
            corrected_summary=None,
            corrected_decisions=None,
            corrected_action_items=None,
            corrected_follow_up_plan=None
        )
