"""
Unit and integration tests for MeetMind AI Multi-Agent System
Tests each of the 5 specialized agents independently with mocked Groq responses,
and tests the complete orchestrated workflow with failure paths and edge cases.
"""

import sys
import os
import json
from unittest.mock import MagicMock, patch

# Add parent directory to sys.path so tests can import agents, orchestrator, app
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import pytest
from orchestrator import MeetingOrchestrator, OrchestrationError
from agent import MeetMindAgent
from app import app
from agents.schemas import (
    TranscriptOutput,
    DecisionItem,
    DecisionOutput,
    ActionItem,
    ActionItemOutput,
    FollowUpOutput,
    ReviewFinding,
    ReviewOutput
)
from agents.transcript_agent import TranscriptAgent
from agents.decision_agent import DecisionAgent
from agents.action_item_agent import ActionItemAgent
from agents.follow_up_agent import FollowUpAgent
from agents.review_agent import ReviewAgent


SAMPLE_TRANSCRIPT = """
Project Alpha Weekly Sync - Oct 9, 2026
Attendees: Alice, Bob, Charlie, Dana

Discussion:
Alice presented the architecture for MeetMind AI. The team reviewed whether to use PostgreSQL or MongoDB.
Decision: Approved MongoDB Community Server for unstructured transcript storage.
Bob mentioned we need a clean Spring Boot backend with proper DTOs.
Action: Bob will implement the Spring Boot REST controllers and MongoDB repositories by Oct 14.
Charlie suggested adding Redis, but Alice noted we should verify memory limits first.
Unresolved Question: Will Redis caching be required before version 1.0 release?
Dana will configure Vite React with external CSS by Oct 12.
We also need to prepare integration test containers.
Next meeting: Scheduled for Oct 16 at 3:00 PM EST.
"""


def make_mock_completion(payload: dict):
    """Helper to generate a mock Groq ChatCompletion with json_object content."""
    mock_resp = MagicMock()
    mock_choice = MagicMock()
    mock_msg = MagicMock()
    mock_msg.content = json.dumps(payload)
    mock_choice.message = mock_msg
    mock_resp.choices = [mock_choice]
    return mock_resp


# ===========================================================================
# 1. INDEPENDENT AGENT TESTS WITH MOCKED GROQ RESPONSES
# ===========================================================================

def test_transcript_agent_mocked():
    """Tests Transcript Agent parses topics, participants, and summary from Groq."""
    mock_client = MagicMock()
    mock_client.chat.completions.create.return_value = make_mock_completion({
        "summary": "Team agreed to use MongoDB and deploy by mid-October.",
        "topics": ["Architecture", "Database", "UI Design"],
        "participants": ["Alice", "Bob", "Charlie", "Dana"],
        "key_discussion_points": ["MongoDB selection", "Spring Boot DTO design"]
    })

    agent = TranscriptAgent(client=mock_client)
    res = agent.run(SAMPLE_TRANSCRIPT)

    assert isinstance(res, TranscriptOutput)
    assert res.summary == "Team agreed to use MongoDB and deploy by mid-October."
    assert "Alice" in res.participants
    assert "Architecture" in res.topics
    assert len(res.key_discussion_points) == 2


def test_decision_agent_mocked():
    """Tests Decision Agent extracts explicit decisions with evidence and separates proposals."""
    mock_client = MagicMock()
    mock_client.chat.completions.create.return_value = make_mock_completion({
        "decisions": [
            {
                "decision": "Approved MongoDB Community Server",
                "evidence": "Decision: Approved MongoDB Community Server for unstructured transcript storage.",
                "consensus_type": "Approved"
            }
        ],
        "proposals_and_suggestions": ["Charlie suggested adding Redis"],
        "unresolved_questions": ["Will Redis caching be required before version 1.0 release?"]
    })

    dummy_transcript = TranscriptOutput(
        summary="Overview",
        topics=["DB"],
        participants=["Alice"],
        key_discussion_points=["Storage"]
    )

    agent = DecisionAgent(client=mock_client)
    res = agent.run(dummy_transcript, SAMPLE_TRANSCRIPT)

    assert isinstance(res, DecisionOutput)
    assert len(res.decisions) == 1
    assert res.decisions[0].decision == "Approved MongoDB Community Server"
    assert "Approved MongoDB" in res.decisions[0].evidence
    assert len(res.proposals_and_suggestions) == 1
    assert len(res.unresolved_questions) == 1


def test_action_item_agent_mocked_and_accuracy_rules():
    """Tests Action Item Agent enforces 'Unassigned' and 'Not specified' accuracy rules."""
    mock_client = MagicMock()
    mock_client.chat.completions.create.return_value = make_mock_completion({
        "action_items": [
            {
                "task": "Implement Spring Boot backend",
                "owner": "Bob",
                "deadline": "Oct 14",
                "status": "Pending",
                "evidence": "Bob will implement the Spring Boot REST controllers by Oct 14."
            },
            {
                "task": "Prepare integration test containers",
                "owner": "TBD",  # Must be converted to Unassigned
                "deadline": "None",  # Must be converted to Not specified
                "status": "",
                "evidence": "We also need to prepare integration test containers."
            }
        ]
    })

    dummy_transcript = TranscriptOutput(summary="Overview", topics=[], participants=["Bob"], key_discussion_points=[])
    agent = ActionItemAgent(client=mock_client)
    res = agent.run(dummy_transcript, SAMPLE_TRANSCRIPT)

    assert isinstance(res, ActionItemOutput)
    assert len(res.action_items) == 2
    # Item 1: Bob
    assert res.action_items[0].owner == "Bob"
    assert res.action_items[0].deadline == "Oct 14"
    # Item 2: Sanitized
    assert res.action_items[1].owner == "Unassigned"
    assert res.action_items[1].deadline == "Not specified"
    assert res.action_items[1].status == "Pending"


def test_follow_up_agent_mocked():
    """Tests Follow-Up Agent constructs post-meeting plan and next meeting date."""
    mock_client = MagicMock()
    mock_client.chat.completions.create.return_value = make_mock_completion({
        "steps": [
            "Bob completes backend controllers by Oct 14",
            "Dana configures Vite React by Oct 12"
        ],
        "unresolved_questions": ["Confirm Redis memory impact"],
        "next_meeting_date": "Oct 16 at 3:00 PM EST"
    })

    decisions = DecisionOutput(decisions=[DecisionItem(decision="Use MongoDB", evidence="")])
    actions = ActionItemOutput(action_items=[ActionItem(task="Backend", owner="Bob", deadline="Oct 14")])

    agent = FollowUpAgent(client=mock_client)
    res = agent.run(decisions, actions, SAMPLE_TRANSCRIPT)

    assert isinstance(res, FollowUpOutput)
    assert len(res.steps) == 2
    assert res.next_meeting_date == "Oct 16 at 3:00 PM EST"
    assert len(res.unresolved_questions) == 1


def test_review_agent_mocked_and_quality_audit():
    """Tests Review Agent audits outputs, records findings, and validates factual consistency."""
    mock_client = MagicMock()
    mock_client.chat.completions.create.return_value = make_mock_completion({
        "validation_passed": True,
        "findings": [
            {
                "category": "ACCURACY_VERIFIED",
                "severity": "PASS",
                "issue": "Decisions verified against transcript text.",
                "evidence_checked": "Approved MongoDB Community Server",
                "action_taken": "Validated"
            },
            {
                "category": "MISSING_OWNER",
                "severity": "INFO",
                "issue": "Task 'Prepare integration test containers' has no named assignee.",
                "evidence_checked": "We also need to prepare integration test containers.",
                "action_taken": "Labeled as 'Unassigned'"
            }
        ],
        "corrected_summary": None,
        "corrected_decisions": None,
        "corrected_action_items": None
    })

    t_out = TranscriptOutput(summary="Sum", topics=[], participants=["Alice"], key_discussion_points=[])
    d_out = DecisionOutput(decisions=[DecisionItem(decision="Use MongoDB", evidence="")])
    a_out = ActionItemOutput(action_items=[ActionItem(task="Containers", owner="Unassigned", deadline="Not specified")])
    f_out = FollowUpOutput(steps=["Step 1"], unresolved_questions=[], next_meeting_date="Oct 16")

    agent = ReviewAgent(client=mock_client)
    res = agent.run(SAMPLE_TRANSCRIPT, t_out, d_out, a_out, f_out)

    assert isinstance(res, ReviewOutput)
    assert res.validation_passed is True
    assert len(res.findings) == 2
    assert res.findings[0].severity == "PASS"
    assert res.findings[1].category == "MISSING_OWNER"


# ===========================================================================
# 2. COMPLETE ORCHESTRATED MULTI-AGENT WORKFLOW TESTS
# ===========================================================================

def test_orchestrator_mocked_full_workflow():
    """Tests the full 5-stage orchestration with mocked Groq calls for all agents."""
    mock_client = MagicMock()

    # Mock responses for stages in order:
    # 1. Transcript Agent
    resp_transcript = make_mock_completion({
        "summary": "Team aligned on core tech stack and tasks.",
        "topics": ["Architecture", "Backend", "Frontend"],
        "participants": ["Alice", "Bob", "Charlie", "Dana"],
        "key_discussion_points": ["DB selection", "Sprint timeline"]
    })
    # 2. Decision Agent (parallel stage)
    resp_decision = make_mock_completion({
        "decisions": [{"decision": "Approved MongoDB Community Server", "evidence": "Decision: Approved MongoDB"}],
        "proposals_and_suggestions": ["Charlie suggested adding Redis"],
        "unresolved_questions": ["Will Redis caching be required?"]
    })
    # 3. Action Item Agent (parallel stage)
    resp_action = make_mock_completion({
        "action_items": [
            {
                "task": "Implement Spring Boot REST controllers",
                "owner": "Bob",
                "deadline": "Oct 14",
                "status": "Pending",
                "evidence": "Bob will implement the Spring Boot REST controllers by Oct 14."
            }
        ]
    })
    # 4. Follow-Up Agent
    resp_followup = make_mock_completion({
        "steps": ["Bob implements controllers by Oct 14"],
        "unresolved_questions": ["Redis confirmation"],
        "next_meeting_date": "Oct 16 at 3:00 PM EST"
    })
    # 5. Review Agent
    resp_review = make_mock_completion({
        "validation_passed": True,
        "findings": [
            {
                "category": "ACCURACY_VERIFIED",
                "severity": "PASS",
                "issue": "All decisions grounded.",
                "evidence_checked": "Approved MongoDB",
                "action_taken": "Verified"
            }
        ],
        "corrected_summary": None,
        "corrected_decisions": None,
        "corrected_action_items": None
    })

    # The order of calls: transcript -> (decision and action item in parallel) -> follow_up -> review
    mock_client.chat.completions.create.side_effect = [
        resp_transcript,
        resp_decision,
        resp_action,
        resp_followup,
        resp_review
    ]

    orchestrator = MeetingOrchestrator(client=mock_client)
    result = orchestrator.analyze_meeting(SAMPLE_TRANSCRIPT)

    # Verify structured unified contract
    assert result["summary"] == "Team aligned on core tech stack and tasks."
    assert "Approved MongoDB Community Server" in result["decisions"]
    assert len(result["action_items"]) == 1
    assert result["action_items"][0]["owner"] == "Bob"
    assert result["action_items"][0]["deadline"] == "Oct 14"
    assert result["next_meeting_date"] == "Oct 16 at 3:00 PM EST"
    assert len(result["review_findings"]) == 1
    assert result["review_findings"][0]["severity"] == "PASS"

    # Verify execution telemetry
    assert "agent_execution" in result
    stages = [s["agent"] for s in result["agent_execution"]["stages"]]
    assert "Transcript Agent" in stages
    assert "Decision Agent" in stages
    assert "Action Item Agent" in stages
    assert "Follow-Up Agent" in stages
    assert "Review Agent" in stages


def test_orchestrator_deterministic_workflow():
    """Tests the full multi-agent workflow in offline/deterministic mode."""
    orchestrator = MeetingOrchestrator()
    result = orchestrator.analyze_meeting(SAMPLE_TRANSCRIPT)

    assert "summary" in result and len(result["summary"]) > 10
    assert len(result["decisions"]) >= 1
    assert len(result["action_items"]) >= 1
    assert result["next_meeting_date"] != ""
    assert len(result["review_findings"]) >= 1
    assert "transcript_analysis" in result
    assert "participants" in result["transcript_analysis"]
    assert "Alice" in result["transcript_analysis"]["participants"]


# ===========================================================================
# 3. EDGE CASES & ACCURACY RULES
# ===========================================================================

def test_empty_notes_validation():
    """Tests validation error on empty notes."""
    orchestrator = MeetingOrchestrator()
    with pytest.raises(ValueError, match="cannot be empty"):
        orchestrator.analyze_meeting("   ")


def test_too_brief_notes_validation():
    """Tests validation error on notes that are too brief."""
    orchestrator = MeetingOrchestrator()
    with pytest.raises(ValueError, match="too brief"):
        orchestrator.analyze_meeting("Hello sync.")


def test_missing_deadlines_and_unassigned_tasks():
    """Tests that tasks without explicit assignees or dates get 'Unassigned' and 'Not specified'."""
    notes = """
    Planning Meeting
    Discussion:
    We need to write the user documentation.
    Action item: prepare database migration script.
    """
    orchestrator = MeetingOrchestrator()
    result = orchestrator.analyze_meeting(notes)

    for item in result["action_items"]:
        if "migration script" in item["task"].lower():
            assert item["owner"] == "Unassigned"
            assert item["deadline"] == "Not specified"


def test_agent_api_failure_halts_pipeline():
    """
    Tests that if an agent encounters a fatal API or execution failure,
    the pipeline halts immediately and raises OrchestrationError without persisting.
    """
    mock_client = MagicMock()
    # Transcript agent fails with fatal error
    mock_client.chat.completions.create.side_effect = RuntimeError("Groq Connection Timeout / 503 Service Unavailable")

    orchestrator = MeetingOrchestrator(client=mock_client)
    with pytest.raises(OrchestrationError) as exc_info:
        orchestrator.analyze_meeting(SAMPLE_TRANSCRIPT)

    assert exc_info.value.stage == "Transcript Agent"


# ===========================================================================
# 4. FLASK ENDPOINT INTEGRATION TESTS
# ===========================================================================

def test_flask_health_endpoint():
    client = app.test_client()
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["status"] == "UP"
    assert "agents" in data
    assert len(data["agents"]) == 5
    assert "Transcript Agent" in data["agents"]
    assert "Review Agent" in data["agents"]


def test_flask_analyze_endpoint_empty_payload():
    client = app.test_client()
    resp = client.post("/agent/analyze", json={})
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data


def test_flask_analyze_endpoint_success():
    client = app.test_client()
    resp = client.post("/agent/analyze", json={"meeting_notes": SAMPLE_TRANSCRIPT})
    assert resp.status_code == 200
    data = resp.get_json()
    assert "summary" in data
    assert "decisions" in data
    assert "action_items" in data
    assert "follow_up_plan" in data
    assert "review_findings" in data
    assert "agent_execution" in data
    assert len(data["agent_execution"]["stages"]) == 5
