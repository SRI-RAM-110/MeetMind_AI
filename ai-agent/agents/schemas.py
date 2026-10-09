"""
MeetMind AI - Agent Schemas
Defines structured input/output contracts for all five specialized agents.
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# 1. Transcript Agent Schemas
# ---------------------------------------------------------------------------
class TranscriptOutput(BaseModel):
    summary: str = Field(..., description="Executive summary of the meeting")
    topics: List[str] = Field(default_factory=list, description="Topics discussed")
    participants: List[str] = Field(default_factory=list, description="Explicitly named attendees or participants")
    key_discussion_points: List[str] = Field(default_factory=list, description="Key discussion points")


# ---------------------------------------------------------------------------
# 2. Decision Agent Schemas
# ---------------------------------------------------------------------------
class DecisionItem(BaseModel):
    decision: str = Field(..., description="Explicit, agreed-upon decision")
    evidence: str = Field(default="", description="Supporting direct evidence/quote from the transcript")
    consensus_type: Optional[str] = Field(default="Confirmed", description="Type of consensus (e.g. Confirmed, Approved)")


class DecisionOutput(BaseModel):
    decisions: List[DecisionItem] = Field(default_factory=list, description="List of confirmed decisions")
    proposals_and_suggestions: List[str] = Field(default_factory=list, description="Suggestions or proposals not confirmed")
    unresolved_questions: List[str] = Field(default_factory=list, description="Unanswered questions left open")


# ---------------------------------------------------------------------------
# 3. Action Item Agent Schemas
# ---------------------------------------------------------------------------
class ActionItem(BaseModel):
    task: str = Field(..., description="Actionable deliverable description")
    task_id: Optional[str] = Field(default=None, description="Stable unique task identifier (e.g. TASK-01)")
    title: Optional[str] = Field(default=None, description="Short task title")
    description: Optional[str] = Field(default=None, description="Detailed task description")
    owner: str = Field(default="Unassigned", description="Explicit assignee or 'Unassigned' if not named")
    owner_role: Optional[str] = Field(default=None, description="Task-specific role or responsibility of the assignee")
    deadline: str = Field(default="Not specified", description="Explicit deadline or 'Not specified' if missing")
    priority: str = Field(default="Medium", description="Task priority (High, Medium, Low)")
    status: str = Field(default="Pending", description="Task status (default 'Pending')")
    dependencies: List[str] = Field(default_factory=list, description="List of prerequisite tasks or dependencies")
    evidence: str = Field(default="", description="Verbatim evidence phrase from the transcript")
    source_timestamp: Optional[str] = Field(default=None, description="Timestamp or time code from transcript")
    confidence: Optional[float] = Field(default=1.0, description="Extraction confidence score (0.0 to 1.0)")
    needs_confirmation: bool = Field(default=False, description="Whether this task needs human confirmation")


class ActionItemOutput(BaseModel):
    action_items: List[ActionItem] = Field(default_factory=list, description="List of validated action items")


# ---------------------------------------------------------------------------
# 4. Follow-Up Agent Schemas
# ---------------------------------------------------------------------------
class FollowUpOutput(BaseModel):
    overview: Optional[str] = Field(default=None, description="High-level follow-up execution summary")
    steps: List[str] = Field(default_factory=list, description="Chronological follow-up steps")
    checkpoints: List[str] = Field(default_factory=list, description="AI-recommended progress review checkpoints")
    upcoming_review_meetings: List[str] = Field(default_factory=list, description="Explicitly confirmed review meetings")
    pending_clarifications: List[str] = Field(default_factory=list, description="Details or assumptions needing clarification")
    unresolved_questions: List[str] = Field(default_factory=list, description="Unresolved questions and confirmations needed")
    next_meeting_date: str = Field(default="Not specified", description="Stated next meeting date or 'Not specified'")


# ---------------------------------------------------------------------------
# 5. Review Agent Schemas
# ---------------------------------------------------------------------------
class ReviewFinding(BaseModel):
    category: str = Field(..., description="Category of finding (e.g., UNSUPPORTED_CLAIM, MISSING_OWNER, DATE_CHECK, ACCURACY_VERIFIED)")
    severity: str = Field(default="INFO", description="FLAG, WARNING, INFO, or PASS")
    issue: str = Field(..., description="Description of the check or flagged issue")
    evidence_checked: str = Field(default="", description="Transcript quote or element checked")
    action_taken: str = Field(default="", description="Corrective action taken or verification note")


class ReviewOutput(BaseModel):
    validation_passed: bool = Field(default=True, description="True if meeting outputs are grounded and factual")
    findings: List[ReviewFinding] = Field(default_factory=list, description="Detailed validation findings")
    corrected_summary: Optional[str] = Field(default=None, description="Corrected or validated summary")
    corrected_decisions: Optional[List[DecisionItem]] = Field(default=None, description="Validated decisions")
    corrected_action_items: Optional[List[ActionItem]] = Field(default=None, description="Validated action items")
    corrected_follow_up_plan: Optional[FollowUpOutput] = Field(default=None, description="Validated follow-up plan")


# ---------------------------------------------------------------------------
# Orchestrator Telemetry & Final Result Schema
# ---------------------------------------------------------------------------
class AgentExecutionStage(BaseModel):
    agent: str
    status: str  # "COMPLETED", "FAILED", "SKIPPED"
    duration_ms: int
    details: Optional[str] = None


class MeetingAnalysisResult(BaseModel):
    summary: str
    decisions: List[str]
    action_items: List[Dict[str, Any]]
    follow_up_plan: Dict[str, Any]
    next_meeting_date: str
    unresolved_questions: List[str]
    review_findings: List[Dict[str, Any]]
    transcript_analysis: Dict[str, Any]
    agent_execution: Dict[str, Any]
