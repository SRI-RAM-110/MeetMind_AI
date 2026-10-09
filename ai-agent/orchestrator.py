"""
MeetMind AI - Central Multi-Agent Orchestrator
Coordinates the 5 specialized agents in dependency order with parallel execution
where independent (Decision Agent and Action Item Agent).
"""

import os
import time
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, Any, Optional, List

from dotenv import load_dotenv

from agents.schemas import (
    TranscriptOutput,
    DecisionOutput,
    ActionItemOutput,
    FollowUpOutput,
    ReviewOutput,
    MeetingAnalysisResult,
    AgentExecutionStage
)
from agents.base_agent import AgentExecutionError
from agents.transcript_agent import TranscriptAgent
from agents.decision_agent import DecisionAgent
from agents.action_item_agent import ActionItemAgent
from agents.follow_up_agent import FollowUpAgent
from agents.review_agent import ReviewAgent

load_dotenv()

logger = logging.getLogger(__name__)

DEFAULT_MODEL = "llama-3.3-70b-versatile"


class OrchestrationError(Exception):
    """Raised when the multi-agent pipeline fails or fails validation."""
    def __init__(self, message: str, stage: str, details: Optional[Any] = None):
        super().__init__(f"Orchestration failed at stage '{stage}': {message}")
        self.stage = stage
        self.details = details


class MeetingOrchestrator:
    """
    Central orchestrator coordinating:
    1. Transcript Agent
    2. Decision Agent & Action Item Agent (in parallel)
    3. Follow-Up Agent
    4. Review Agent
    5. Final schema validation
    """
    def __init__(self, api_key: Optional[str] = None, model: Optional[str] = None, client: Optional[Any] = None):
        self.api_key = api_key or os.getenv("GROQ_API_KEY", "")
        self.model = model or os.getenv("GROQ_MODEL", DEFAULT_MODEL)
        self._client = client

        if self._client is None and self.api_key and self.api_key.strip() and self.api_key not in ["replace_me", "your_groq_api_key_here"]:
            try:
                from groq import Groq
                self._client = Groq(api_key=self.api_key.strip())
                logger.info("MeetingOrchestrator initialized shared Groq client with model: %s", self.model)
            except Exception as e:
                logger.warning("Could not initialize Groq client in orchestrator: %s", e)

        # Instantiate 5 specialized agents
        self.transcript_agent = TranscriptAgent(client=self._client, model=self.model)
        self.decision_agent = DecisionAgent(client=self._client, model=self.model)
        self.action_item_agent = ActionItemAgent(client=self._client, model=self.model)
        self.follow_up_agent = FollowUpAgent(client=self._client, model=self.model)
        self.review_agent = ReviewAgent(client=self._client, model=self.model)

    @property
    def client(self):
        return self._client

    @client.setter
    def client(self, val):
        self._client = val
        self.transcript_agent.client = val
        self.decision_agent.client = val
        self.action_item_agent.client = val
        self.follow_up_agent.client = val
        self.review_agent.client = val

    def is_live_client_ready(self) -> bool:
        return self.client is not None

    def analyze_meeting(self, meeting_notes: str) -> Dict[str, Any]:
        """
        Executes the orchestrated multi-agent pipeline against meeting notes.
        Raises OrchestrationError if any required stage fails or if validation fails.
        """
        cleaned_notes = (meeting_notes or "").strip()
        if not cleaned_notes:
            raise ValueError("Meeting notes cannot be empty or whitespace only.")

        if len(cleaned_notes) < 15:
            raise ValueError("Meeting notes are too brief for meaningful business analysis (minimum 15 characters).")

        pipeline_start_t = time.time()
        stage_telemetry: List[AgentExecutionStage] = []

        logger.info(">>> Starting MeetMind Multi-Agent Orchestration Pipeline (notes: %d chars)", len(cleaned_notes))

        # =====================================================================
        # STAGE 1: Transcript Agent
        # =====================================================================
        s1_start = time.time()
        try:
            logger.info("[Pipeline] Stage 1/5: Running Transcript Agent...")
            transcript_data: TranscriptOutput = self.transcript_agent.run(cleaned_notes)
            s1_duration = int((time.time() - s1_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Transcript Agent",
                status="COMPLETED",
                duration_ms=s1_duration,
                details=f"Extracted {len(transcript_data.topics)} topics, {len(transcript_data.participants)} participants"
            ))
        except Exception as e:
            s1_duration = int((time.time() - s1_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Transcript Agent",
                status="FAILED",
                duration_ms=s1_duration,
                details=str(e)
            ))
            logger.error("[Pipeline] Stage 1 (Transcript Agent) failed: %s", e, exc_info=True)
            raise OrchestrationError(str(e), stage="Transcript Agent")

        # =====================================================================
        # STAGE 2: Parallel execution: Decision Agent & Action Item Agent
        # =====================================================================
        s2_start = time.time()
        decision_data: Optional[DecisionOutput] = None
        action_item_data: Optional[ActionItemOutput] = None

        logger.info("[Pipeline] Stage 2/5: Running Decision Agent & Action Item Agent in parallel...")
        try:
            with ThreadPoolExecutor(max_workers=2, thread_name_prefix="AgentStage2") as executor:
                future_decision = executor.submit(self.decision_agent.run, transcript_data, cleaned_notes)
                future_actions = executor.submit(self.action_item_agent.run, transcript_data, cleaned_notes)

                # Wait for both
                decision_data = future_decision.result()
                action_item_data = future_actions.result()

            s2_duration = int((time.time() - s2_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Decision Agent",
                status="COMPLETED",
                duration_ms=s2_duration,
                details=f"Extracted {len(decision_data.decisions)} decisions"
            ))
            stage_telemetry.append(AgentExecutionStage(
                agent="Action Item Agent",
                status="COMPLETED",
                duration_ms=s2_duration,
                details=f"Extracted {len(action_item_data.action_items)} action items"
            ))
        except Exception as e:
            s2_duration = int((time.time() - s2_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Decision & Action Item Stage",
                status="FAILED",
                duration_ms=s2_duration,
                details=str(e)
            ))
            logger.error("[Pipeline] Stage 2 (Decision / Action Item) failed: %s", e, exc_info=True)
            raise OrchestrationError(str(e), stage="Decision & Action Item Extraction")

        # =====================================================================
        # STAGE 3: Follow-Up Agent
        # =====================================================================
        s3_start = time.time()
        try:
            logger.info("[Pipeline] Stage 3/5: Running Follow-Up Agent...")
            follow_up_data: FollowUpOutput = self.follow_up_agent.run(
                decision_data=decision_data,
                action_item_data=action_item_data,
                meeting_notes=cleaned_notes,
                transcript_data=transcript_data
            )
            s3_duration = int((time.time() - s3_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Follow-Up Agent",
                status="COMPLETED",
                duration_ms=s3_duration,
                details=f"Formulated {len(follow_up_data.steps)} steps, next meeting: {follow_up_data.next_meeting_date}"
            ))
        except Exception as e:
            s3_duration = int((time.time() - s3_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Follow-Up Agent",
                status="FAILED",
                duration_ms=s3_duration,
                details=str(e)
            ))
            logger.error("[Pipeline] Stage 3 (Follow-Up Agent) failed: %s", e, exc_info=True)
            raise OrchestrationError(str(e), stage="Follow-Up Agent")

        # =====================================================================
        # STAGE 4: Review Agent
        # =====================================================================
        s4_start = time.time()
        try:
            logger.info("[Pipeline] Stage 4/5: Running Review Agent...")
            review_data: ReviewOutput = self.review_agent.run(
                meeting_notes=cleaned_notes,
                transcript_data=transcript_data,
                decision_data=decision_data,
                action_item_data=action_item_data,
                follow_up_data=follow_up_data
            )
            s4_duration = int((time.time() - s4_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Review Agent",
                status="COMPLETED",
                duration_ms=s4_duration,
                details=f"Audit completed: {len(review_data.findings)} findings recorded (passed={review_data.validation_passed})"
            ))
        except Exception as e:
            s4_duration = int((time.time() - s4_start) * 1000)
            stage_telemetry.append(AgentExecutionStage(
                agent="Review Agent",
                status="FAILED",
                duration_ms=s4_duration,
                details=str(e)
            ))
            logger.error("[Pipeline] Stage 4 (Review Agent) failed: %s", e, exc_info=True)
            raise OrchestrationError(str(e), stage="Review Agent")

        # =====================================================================
        # STAGE 5: Synthesis and Final Schema Validation
        # =====================================================================
        total_duration = int((time.time() - pipeline_start_t) * 1000)

        # Apply any corrections suggested by Review Agent (without silently inventing facts)
        final_summary = review_data.corrected_summary or transcript_data.summary
        
        # Decisions list: if review agent corrected them, use corrected; else decision_data
        active_decisions_objs = review_data.corrected_decisions or decision_data.decisions
        decisions_strings = [d.decision for d in active_decisions_objs if d.decision]

        # Action items: if review agent corrected them, use corrected; else action_item_data
        active_action_items_objs = review_data.corrected_action_items or action_item_data.action_items
        action_items_list = [item.model_dump() for item in active_action_items_objs]

        # Follow-up plan:
        follow_up_plan_dict = {
            "overview": follow_up_data.overview,
            "steps": follow_up_data.steps,
            "checkpoints": follow_up_data.checkpoints,
            "upcoming_review_meetings": follow_up_data.upcoming_review_meetings,
            "pending_clarifications": follow_up_data.pending_clarifications,
            "unresolved_questions": follow_up_data.unresolved_questions,
            "next_meeting_date": follow_up_data.next_meeting_date
        }

        # Review findings
        review_findings_list = [finding.model_dump() for finding in review_data.findings]

        # Transcript analysis
        transcript_analysis_dict = {
            "topics": transcript_data.topics,
            "participants": transcript_data.participants,
            "key_discussion_points": transcript_data.key_discussion_points
        }

        # Execution telemetry
        agent_execution_dict = {
            "total_duration_ms": total_duration,
            "model": self.model,
            "stages": [stage.model_dump() for stage in stage_telemetry]
        }

        final_result = {
            "summary": final_summary,
            "decisions": decisions_strings,
            "action_items": action_items_list,
            "follow_up_plan": follow_up_plan_dict,
            "next_meeting_date": follow_up_data.next_meeting_date,
            "unresolved_questions": follow_up_data.unresolved_questions,
            "review_findings": review_findings_list,
            "transcript_analysis": transcript_analysis_dict,
            "agent_execution": agent_execution_dict
        }

        # Strict Pydantic validation before return
        validated_schema = MeetingAnalysisResult(**final_result)
        logger.info("<<< MeetMind Multi-Agent Orchestration finished successfully in %dms", total_duration)

        return validated_schema.model_dump()
