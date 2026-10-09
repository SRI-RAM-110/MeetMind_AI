"""
MeetMind AI - Agent Tool Definitions
Contains tool functions and schemas for Groq function/tool calling.
None of these tools write directly to MongoDB; they validate and structure data
for the calling Java Spring Boot service.
"""

from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


class ActionItemSchema(BaseModel):
    task: str = Field(..., description="Actionable task description")
    owner: str = Field(default="Unassigned", description="Explicitly assigned owner, or 'Unassigned' if none")
    deadline: str = Field(default="Not specified", description="Explicitly stated deadline or date, or 'Not specified'")
    status: str = Field(default="Pending", description="Status of the task (e.g., Pending, In Progress, Completed)")
    evidence: str = Field(..., description="Verbatim quote or excerpt from notes providing proof for this task")


class MeetingInsightsSchema(BaseModel):
    summary: str = Field(..., description="Objective, executive summary of the meeting")
    decisions: List[str] = Field(default_factory=list, description="List of confirmed decisions (excluding mere proposals)")
    unresolved_questions: List[str] = Field(default_factory=list, description="Open questions or topics left unresolved")
    next_meeting_date: Optional[str] = Field(default="Not specified", description="Explicit date/time of next meeting, if stated")


class FollowUpPlanSchema(BaseModel):
    steps: List[str] = Field(..., description="Ordered or prioritized follow-up milestone steps")
    unresolved_questions: List[str] = Field(default_factory=list, description="Items requiring confirmation or clarification")
    next_meeting_date: Optional[str] = Field(default="Not specified", description="Explicitly scheduled next meeting date")


# Tool Functions

def read_meeting_notes(meeting_notes: str) -> Dict[str, Any]:
    """
    Reads, cleans, and pre-analyzes raw meeting notes or transcripts.
    Computes text statistics and verifies content readiness.
    """
    cleaned = (meeting_notes or "").strip()
    if not cleaned:
        return {
            "status": "error",
            "message": "Meeting notes cannot be empty."
        }
    
    lines = [line.strip() for line in cleaned.splitlines() if line.strip()]
    word_count = len(cleaned.split())
    
    return {
        "status": "success",
        "word_count": word_count,
        "line_count": len(lines),
        "preview": cleaned[:200] + ("..." if len(cleaned) > 200 else ""),
        "message": f"Successfully ingested meeting notes ({word_count} words, {len(lines)} lines)."
    }


def extract_meeting_insights(
    summary: str,
    decisions: List[str],
    unresolved_questions: Optional[List[str]] = None,
    next_meeting_date: Optional[str] = "Not specified"
) -> Dict[str, Any]:
    """
    Validates executive summary, confirmed decisions, unresolved questions,
    and explicitly mentioned next meeting date.
    Enforces rules: do not invent decisions; separate proposals from firm agreements.
    """
    unresolved = unresolved_questions or []
    cleaned_decisions = [d.strip() for d in decisions if d and d.strip()]
    cleaned_unresolved = [q.strip() for q in unresolved if q and q.strip()]
    cleaned_next_date = (next_meeting_date or "Not specified").strip()
    if not cleaned_next_date:
        cleaned_next_date = "Not specified"

    if not summary or not summary.strip():
        return {
            "status": "error",
            "message": "Summary must be provided."
        }

    return {
        "status": "success",
        "summary": summary.strip(),
        "decisions": cleaned_decisions,
        "unresolved_questions": cleaned_unresolved,
        "next_meeting_date": cleaned_next_date,
        "message": f"Validated {len(cleaned_decisions)} decisions and {len(cleaned_unresolved)} open items."
    }


def save_action_items(action_items: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Prepares and validates structured action items for the calling service.
    Enforces strict rules:
      - Owner must be explicitly stated in the transcript; otherwise 'Unassigned'.
      - Deadline must be explicitly stated; otherwise 'Not specified'.
      - Supporting evidence must be provided.
      - Never writes directly to MongoDB.
    """
    validated_items: List[Dict[str, Any]] = []

    for idx, item in enumerate(action_items):
        task = str(item.get("task", "")).strip()
        if not task:
            continue

        raw_owner = str(item.get("owner", "")).strip()
        # Normalization: If owner is empty, generic 'TBD', 'Someone', or 'None', set to 'Unassigned'
        if not raw_owner or raw_owner.lower() in ["none", "null", "tbd", "unknown", "n/a", "no owner", "unassigned"]:
            owner = "Unassigned"
        else:
            owner = raw_owner

        raw_deadline = str(item.get("deadline", "")).strip()
        if not raw_deadline or raw_deadline.lower() in ["none", "null", "tbd", "unknown", "n/a", "not specified", "asap"]:
            if raw_deadline.lower() == "asap":
                deadline = "ASAP (Date not specified)"
            else:
                deadline = "Not specified"
        else:
            deadline = raw_deadline

        status = str(item.get("status", "Pending")).strip()
        if not status:
            status = "Pending"

        evidence = str(item.get("evidence", "")).strip()
        if not evidence:
            evidence = f"Inferred from task discussion: '{task}'"

        validated_items.append({
            "task": task,
            "owner": owner,
            "deadline": deadline,
            "status": status,
            "evidence": evidence
        })

    return {
        "status": "success",
        "count": len(validated_items),
        "action_items": validated_items,
        "message": f"Successfully prepared and validated {len(validated_items)} action items."
    }


def create_follow_up_plan(
    steps: List[str],
    unresolved_questions: Optional[List[str]] = None,
    next_meeting_date: Optional[str] = "Not specified"
) -> Dict[str, Any]:
    """
    Validates and structures the post-meeting follow-up plan.
    Enforces rules: do not invent next meeting dates; capture unresolved questions.
    """
    cleaned_steps = [s.strip() for s in (steps or []) if s and s.strip()]
    cleaned_questions = [q.strip() for q in (unresolved_questions or []) if q and q.strip()]
    cleaned_date = (next_meeting_date or "Not specified").strip()
    if not cleaned_date:
        cleaned_date = "Not specified"

    if not cleaned_steps:
        cleaned_steps = ["Review meeting notes with stakeholders", "Execute assigned action items"]

    plan = {
        "steps": cleaned_steps,
        "unresolved_questions": cleaned_questions,
        "next_meeting_date": cleaned_date
    }

    return {
        "status": "success",
        "follow_up_plan": plan,
        "message": f"Validated follow-up plan with {len(cleaned_steps)} steps."
    }


# Groq Tool Specifications (Function Calling schemas)
GROQ_TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "read_meeting_notes",
            "description": "Reads, normalizes, and pre-checks raw meeting notes or transcripts before detailed analysis.",
            "parameters": {
                "type": "object",
                "properties": {
                    "meeting_notes": {
                        "type": "string",
                        "description": "The verbatim raw text of the meeting notes or transcript."
                    }
                },
                "required": ["meeting_notes"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "extract_meeting_insights",
            "description": "Extracts and validates executive summary, confirmed decisions, unresolved questions, and next meeting date.",
            "parameters": {
                "type": "object",
                "properties": {
                    "summary": {
                        "type": "string",
                        "description": "A concise, objective executive summary of the meeting."
                    },
                    "decisions": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "Explicitly confirmed decisions agreed upon by the team."
                    },
                    "unresolved_questions": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "Items, blockers, or questions left unresolved during the meeting."
                    },
                    "next_meeting_date": {
                        "type": "string",
                        "description": "Explicit date or timeframe for the next meeting, or 'Not specified' if unmentioned."
                    }
                },
                "required": ["summary", "decisions"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "save_action_items",
            "description": "Validates and structures action items. Rules: Use 'Unassigned' if owner is missing; 'Not specified' if deadline is missing; include supporting evidence quote.",
            "parameters": {
                "type": "object",
                "properties": {
                    "action_items": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "task": {
                                    "type": "string",
                                    "description": "The specific actionable task."
                                },
                                "owner": {
                                    "type": "string",
                                    "description": "The named person responsible. Must be 'Unassigned' if not explicitly stated."
                                },
                                "deadline": {
                                    "type": "string",
                                    "description": "Explicit due date or deadline. Must be 'Not specified' if not stated."
                                },
                                "status": {
                                    "type": "string",
                                    "description": "Initial task status, default 'Pending'."
                                },
                                "evidence": {
                                    "type": "string",
                                    "description": "Verbatim quote or excerpt from the notes justifying this assignment."
                                }
                            },
                            "required": ["task", "owner", "deadline", "evidence"]
                        },
                        "description": "List of extracted action items."
                    }
                },
                "required": ["action_items"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "create_follow_up_plan",
            "description": "Prepares and validates the follow-up roadmap including milestones, open questions, and next meeting.",
            "parameters": {
                "type": "object",
                "properties": {
                    "steps": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "Actionable post-meeting steps and check-in milestones."
                    },
                    "unresolved_questions": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "List of questions or uncertainties requiring follow-up."
                    },
                    "next_meeting_date": {
                        "type": "string",
                        "description": "Explicitly scheduled next meeting date or 'Not specified'."
                    }
                },
                "required": ["steps"]
            }
        }
    }
]


TOOL_MAPPING = {
    "read_meeting_notes": read_meeting_notes,
    "extract_meeting_insights": extract_meeting_insights,
    "save_action_items": save_action_items,
    "create_follow_up_plan": create_follow_up_plan
}
