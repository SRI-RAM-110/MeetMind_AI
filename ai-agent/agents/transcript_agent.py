"""
MeetMind AI - Transcript Agent
Responsibility:
- Reads and analyzes raw meeting notes.
- Identifies topics, participants when explicitly stated, and key discussion points.
- Does not invent missing information (e.g. participant names if omitted).
"""

import re
import logging
from typing import Dict, Any, Optional

from .base_agent import BaseAgent, AgentExecutionError
from .schemas import TranscriptOutput

logger = logging.getLogger(__name__)

TRANSCRIPT_SYSTEM_PROMPT = """You are the Transcript Agent in MeetMind AI's multi-agent business meeting intelligence system.
Your sole responsibility is to analyze raw meeting notes or transcripts and extract structured foundational metadata.

STRICT ACCURACY RULES:
1. Executive Summary: Provide an objective, concise executive summary (2-4 sentences) summarizing the main purpose and core outcomes of the meeting.
2. Topics: Extract a list of distinct topics or agenda items discussed.
3. Participants: Extract ONLY individuals who are explicitly stated as attendees, speakers, or participants in the text. If no participants are explicitly mentioned, return an empty list []. DO NOT guess or hallucinate participant names under any circumstances.
4. Key Discussion Points: Extract the primary discussion points faithful to the exact conversation.

OUTPUT FORMAT:
You MUST respond with a valid JSON object matching this schema:
{
  "summary": "Executive summary string",
  "topics": ["Topic 1", "Topic 2"],
  "participants": ["Name 1", "Name 2"],
  "key_discussion_points": ["Discussion point 1", "Discussion point 2"]
}
Do not include any commentary outside the JSON object.
"""


class TranscriptAgent(BaseAgent):
    def __init__(self, client: Optional[Any] = None, model: str = "llama-3.3-70b-versatile"):
        super().__init__(name="Transcript Agent", client=client, model=model)

    def run(self, meeting_notes: str) -> TranscriptOutput:
        """
        Executes transcript analysis on meeting notes.
        """
        cleaned = (meeting_notes or "").strip()
        if not cleaned:
            raise AgentExecutionError(self.name, "Meeting notes cannot be empty.")

        if self.client:
            user_prompt = f"Analyze the following meeting transcript adhering strictly to the accuracy rules:\n\n---\n{cleaned}\n---"
            raw_json = self._call_llm(TRANSCRIPT_SYSTEM_PROMPT, user_prompt)
            try:
                # Ensure participants is a list and sanitize
                participants = raw_json.get("participants") or []
                if isinstance(participants, str):
                    participants = [p.strip() for p in participants.split(",") if p.strip()]
                
                output = TranscriptOutput(
                    summary=raw_json.get("summary", "Executive summary not provided.").strip(),
                    topics=raw_json.get("topics") or [],
                    participants=participants,
                    key_discussion_points=raw_json.get("key_discussion_points") or []
                )
                logger.info("[%s] Successfully extracted summary (%d chars), %d topics, %d participants",
                            self.name, len(output.summary), len(output.topics), len(output.participants))
                return output
            except Exception as e:
                raise AgentExecutionError(self.name, f"Schema validation error on output: {e}", e)
        else:
            return self._run_deterministic_fallback(cleaned)

    def _run_deterministic_fallback(self, notes: str) -> TranscriptOutput:
        """
        Rule-based deterministic extraction for testing and offline environments.
        """
        lines = [line.strip() for line in notes.splitlines() if line.strip()]
        
        # 1. Participants extraction (look for Attendees:, Present:, etc.)
        participants = []
        for line in lines:
            lower = line.lower()
            if any(lower.startswith(k) for k in ["attendees:", "present:", "participants:", "attendees -", "present -"]):
                parts = re.split(r"[:\-]", line, maxsplit=1)
                if len(parts) > 1:
                    names = [n.strip() for n in parts[1].split(",") if n.strip()]
                    participants.extend(names)
                break
        
        # 2. Topics extraction
        topics = []
        for line in lines:
            lower = line.lower()
            if lower.startswith("topic:") or lower.startswith("agenda:"):
                parts = line.split(":", 1)
                if len(parts) > 1:
                    topics.append(parts[1].strip())
            elif any(w in lower for w in ["database", "backend", "frontend", "architecture", "security", "budget", "deployment"]):
                if len(topics) < 4:
                    topics.append(line[:60].strip())

        if not topics:
            topics = ["Business and Technical Discussion"]

        # 3. Key discussion points
        key_points = []
        for line in lines:
            if not any(line.lower().startswith(p) for p in ["attendees:", "decision:", "action:", "next meeting:"]):
                if len(line) > 20 and len(key_points) < 5:
                    key_points.append(line)

        summary = f"Meeting discussing {', '.join(topics[:2])}. Focused on key project deliverables and operational alignment."
        if lines:
            summary = f"{lines[0]} - {summary}"

        return TranscriptOutput(
            summary=summary,
            topics=topics,
            participants=participants,
            key_discussion_points=key_points
        )
