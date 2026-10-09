"""
MeetMind AI - Decision Agent
Responsibility:
- Extracts explicit decisions confirmed by the team.
- Separates confirmed decisions from suggestions, proposals, and unresolved questions.
- Provides supporting evidence (direct quotes or verbatim excerpts) from the transcript.
"""

import json
import logging
from typing import Dict, Any, List, Optional

from .base_agent import BaseAgent, AgentExecutionError
from .schemas import DecisionOutput, DecisionItem, TranscriptOutput

logger = logging.getLogger(__name__)

DECISION_SYSTEM_PROMPT = """You are the Decision Agent in MeetMind AI's multi-agent business meeting intelligence system.
Your sole responsibility is to extract explicit, confirmed decisions and distinguish them from mere suggestions or undecided questions.

STRICT ACCURACY RULES:
1. Confirmed Decisions: Extract ONLY items that represent an explicit agreement, consensus, or sign-off by the participants.
2. Evidence Requirement: Every confirmed decision MUST include direct supporting evidence (exact quote or near-verbatim excerpt from the transcript).
3. Distinguish Proposals and Suggestions: If a participant proposed, suggested, or pitched an idea but it was NOT explicitly agreed upon or approved, you MUST place it in 'proposals_and_suggestions', NEVER in 'decisions'.
4. Unresolved Questions: If an issue was left open, questioned, or postponed for future review, place it in 'unresolved_questions'.
5. Do Not Invent: Never fabricate decisions or assume consensus that is not stated in the text.

OUTPUT FORMAT:
You MUST respond with a valid JSON object matching this schema:
{
  "decisions": [
    {
      "decision": "Approved MongoDB Community Server for primary document storage",
      "evidence": "Decision: Approved MongoDB Community Server for unstructured transcript storage.",
      "consensus_type": "Approved"
    }
  ],
  "proposals_and_suggestions": [
    "Charlie suggested adding Redis caching"
  ],
  "unresolved_questions": [
    "Will Redis caching be required before version 1.0 release?"
  ]
}
Do not include any commentary outside the JSON object.
"""


class DecisionAgent(BaseAgent):
    def __init__(self, client: Optional[Any] = None, model: str = "llama-3.3-70b-versatile"):
        super().__init__(name="Decision Agent", client=client, model=model)

    def run(self, transcript_data: TranscriptOutput, meeting_notes: str) -> DecisionOutput:
        """
        Executes decision extraction given foundational transcript metadata and raw notes.
        """
        if not meeting_notes or not meeting_notes.strip():
            raise AgentExecutionError(self.name, "Meeting notes cannot be empty.")

        if self.client:
            user_prompt = (
                f"Analyze the meeting notes for explicit confirmed decisions.\n\n"
                f"Transcript Summary Context: {transcript_data.summary}\n"
                f"Discussion Topics: {', '.join(transcript_data.topics)}\n\n"
                f"Raw Meeting Notes:\n---\n{meeting_notes}\n---"
            )
            raw_json = self._call_llm(DECISION_SYSTEM_PROMPT, user_prompt)
            try:
                decisions_list = []
                for item in raw_json.get("decisions", []):
                    if isinstance(item, dict):
                        decisions_list.append(DecisionItem(
                            decision=item.get("decision", "").strip(),
                            evidence=item.get("evidence", "").strip(),
                            consensus_type=item.get("consensus_type", "Confirmed")
                        ))
                    elif isinstance(item, str):
                        decisions_list.append(DecisionItem(decision=item.strip(), evidence=""))

                output = DecisionOutput(
                    decisions=decisions_list,
                    proposals_and_suggestions=raw_json.get("proposals_and_suggestions", []),
                    unresolved_questions=raw_json.get("unresolved_questions", [])
                )
                logger.info("[%s] Extracted %d decisions, %d proposals, %d unresolved questions",
                            self.name, len(output.decisions), len(output.proposals_and_suggestions), len(output.unresolved_questions))
                return output
            except Exception as e:
                raise AgentExecutionError(self.name, f"Schema validation error on decisions: {e}", e)
        else:
            return self._run_deterministic_fallback(meeting_notes)

    def _run_deterministic_fallback(self, notes: str) -> DecisionOutput:
        """
        Deterministic extraction for offline testing.
        """
        lines = [line.strip() for line in notes.splitlines() if line.strip()]
        decisions = []
        proposals = []
        questions = []

        for line in lines:
            lower = line.lower()
            if any(lower.startswith(k) for k in ["decision:", "decided:", "approved:", "agreed:"]):
                parts = line.split(":", 1)
                desc = parts[1].strip() if len(parts) > 1 else line
                decisions.append(DecisionItem(
                    decision=desc,
                    evidence=line,
                    consensus_type="Approved"
                ))
            elif any(k in lower for k in ["suggested", "proposed", "recommended", "tentative"]):
                proposals.append(line)
            elif "?" in line or "unresolved" in lower or "open question" in lower:
                questions.append(line)

        # Fallback if no explicit decision keyword found
        if not decisions:
            for line in lines:
                if any(w in line.lower() for w in ["will use", "selected", "chose", "finalized"]):
                    decisions.append(DecisionItem(decision=line, evidence=line, consensus_type="Agreed"))
                    break

        return DecisionOutput(
            decisions=decisions,
            proposals_and_suggestions=proposals,
            unresolved_questions=questions
        )
