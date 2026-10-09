"""
Unit tests for agent tools in tools.py
"""

import pytest
from tools import (
    read_meeting_notes,
    extract_meeting_insights,
    save_action_items,
    create_follow_up_plan
)


def test_read_meeting_notes_valid():
    text = "Team sync on Sprint 4. Discussed database migration and UI redesign."
    res = read_meeting_notes(text)
    assert res["status"] == "success"
    assert res["word_count"] > 0
    assert "ingested" in res["message"]


def test_read_meeting_notes_empty():
    res = read_meeting_notes("   ")
    assert res["status"] == "error"
    assert "cannot be empty" in res["message"]


def test_extract_meeting_insights_valid():
    res = extract_meeting_insights(
        summary="The team agreed to migrate the database by next Friday.",
        decisions=["Use MongoDB for document storage", "Adopt React 19"],
        unresolved_questions=["Who will handle production indexing?"],
        next_meeting_date="2026-10-16"
    )
    assert res["status"] == "success"
    assert len(res["decisions"]) == 2
    assert res["next_meeting_date"] == "2026-10-16"
    assert len(res["unresolved_questions"]) == 1


def test_extract_meeting_insights_empty_summary():
    res = extract_meeting_insights(summary="", decisions=["Some decision"])
    assert res["status"] == "error"


def test_save_action_items_accuracy_rules():
    raw_items = [
        {
            "task": "Build the MongoDB schema",
            "owner": "Sarah",
            "deadline": "2026-10-15",
            "status": "Pending",
            "evidence": "Sarah said: 'I will build the MongoDB schema by Oct 15.'"
        },
        {
            "task": "Update the documentation",
            "owner": "TBD",  # Must be converted to Unassigned
            "deadline": "",   # Must be converted to Not specified
            "status": "",
            "evidence": ""
        }
    ]

    res = save_action_items(raw_items)
    assert res["status"] == "success"
    items = res["action_items"]
    assert len(items) == 2

    # Item 1 checks
    assert items[0]["owner"] == "Sarah"
    assert items[0]["deadline"] == "2026-10-15"
    assert items[0]["status"] == "Pending"
    assert "Sarah said" in items[0]["evidence"]

    # Item 2 checks (enforcing accuracy rules)
    assert items[1]["owner"] == "Unassigned"
    assert items[1]["deadline"] == "Not specified"
    assert items[1]["status"] == "Pending"
    assert items[1]["evidence"] != ""


def test_create_follow_up_plan():
    res = create_follow_up_plan(
        steps=["Review PRs", "Deploy to staging"],
        unresolved_questions=["Budget approval"],
        next_meeting_date="Monday 10 AM"
    )
    assert res["status"] == "success"
    plan = res["follow_up_plan"]
    assert len(plan["steps"]) == 2
    assert plan["next_meeting_date"] == "Monday 10 AM"
    assert len(plan["unresolved_questions"]) == 1
