"""
MeetMind AI - Agent Module Entrypoint
Provides MeetMindAgent backwards-compatibility wrapper backed by the
genuine 5-agent MeetingOrchestrator.
"""

from typing import Dict, Any, Optional

from orchestrator import MeetingOrchestrator, OrchestrationError
from agents.schemas import (
    TranscriptOutput,
    DecisionOutput,
    ActionItemOutput,
    FollowUpOutput,
    ReviewOutput,
    MeetingAnalysisResult
)

class MeetMindAgent:
    """
    Backwards-compatible wrapper delegating to the 5-agent MeetingOrchestrator.
    """
    def __init__(self, api_key: Optional[str] = None, model: Optional[str] = None, client: Optional[Any] = None):
        self.orchestrator = MeetingOrchestrator(api_key=api_key, model=model, client=client)
        self.api_key = self.orchestrator.api_key
        self.model = self.orchestrator.model

    @property
    def client(self):
        return self.orchestrator.client

    @client.setter
    def client(self, val):
        self.orchestrator.client = val

    @property
    def transcript_agent(self):
        return self.orchestrator.transcript_agent

    @property
    def decision_agent(self):
        return self.orchestrator.decision_agent

    @property
    def action_item_agent(self):
        return self.orchestrator.action_item_agent

    @property
    def follow_up_agent(self):
        return self.orchestrator.follow_up_agent

    @property
    def review_agent(self):
        return self.orchestrator.review_agent

    def is_live_client_ready(self) -> bool:
        return self.orchestrator.is_live_client_ready()

    def analyze_meeting(self, meeting_notes: str) -> Dict[str, Any]:
        """
        Runs the full orchestrated multi-agent pipeline.
        """
        return self.orchestrator.analyze_meeting(meeting_notes)
