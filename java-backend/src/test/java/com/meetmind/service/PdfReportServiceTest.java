package com.meetmind.service;

import com.meetmind.model.ActionItem;
import com.meetmind.model.FollowUpPlan;
import com.meetmind.model.Meeting;
import com.meetmind.model.ReviewFinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfReportServiceTest {

    private PdfReportService pdfReportService;

    @BeforeEach
    void setUp() {
        pdfReportService = new PdfReportService();
    }

    @Test
    void testGeneratePdf_WithNullFields() {
        // Meeting with null fields as loaded from older MongoDB records
        Meeting meeting = new Meeting();
        meeting.setId("test123");
        meeting.setMeetingTitle(null);
        meeting.setMeetingDate(null);
        meeting.setStartTime(null);
        meeting.setEndTime(null);
        meeting.setTimeZone(null);
        meeting.setSummary(null);
        meeting.setDecisions(null);
        meeting.setParticipants(null);

        ActionItem item = new ActionItem();
        item.setId("act1");
        item.setMeetingId("test123");
        item.setTitle(null);
        item.setDescription(null);
        item.setOwnerName(null);
        item.setOwnerRole(null);
        item.setDeadline(null);
        item.setPriority(null);
        item.setStatus(null);
        item.setSourceEvidence(null);

        byte[] pdfBytes = pdfReportService.generateFollowUpPlanPdf(
                meeting,
                List.of(item),
                null,
                null
        );

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Valid PDF signature check
        String header = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", header);
    }

    @Test
    void testGeneratePdf_MultiPageWithManyTasks() {
        Meeting meeting = new Meeting();
        meeting.setId("largeMeeting456");
        meeting.setMeetingTitle("Enterprise Architecture & Scalability Summit 2026");
        meeting.setMeetingDate("2026-10-10");
        meeting.setStartTime("09:00 AM");
        meeting.setEndTime("12:00 PM");
        meeting.setTimeZone("EST");
        meeting.setSummary("High-scale enterprise sync discussing infrastructure migrations, multi-agent LLM reasoning workflows, and STS Spring Boot microservices.");
        meeting.setDecisions(List.of(
                "Standardized on MongoDB Community Server for document persistence",
                "Approved Groq Llama 3.3 for real-time agent reasoning",
                "Enforced PDF export compliance using OpenPDF with repeated table headers"
        ));
        meeting.setParticipants(List.of("Sarah Connor", "Alex Mercer", "Priya Sharma", "David Kim", "Elena Rostova", "Marcus Vance"));

        // Generate 25 complex action items to span multiple pages
        List<ActionItem> actionItems = new ArrayList<>();
        String[] owners = {"Sarah Connor", "Alex Mercer", "Priya Sharma", "David Kim", "Unassigned"};
        String[] priorities = {"High", "Medium", "Low", "Critical"};

        for (int i = 1; i <= 25; i++) {
            ActionItem item = new ActionItem();
            item.setId("item-" + i);
            item.setMeetingId("largeMeeting456");
            item.setTaskId("TSK-ARCH-" + i);
            item.setTitle("Milestone task #" + i + ": Implement enterprise component with high throughput and end-to-end telemetry");
            item.setDescription("Detailed architectural deliverable explaining all required sub-tasks, database schemas, and integration points.");
            item.setOwnerName(owners[i % owners.length]);
            item.setOwnerRole(i % 2 == 0 ? "Staff Software Engineer" : "Lead Architect");
            item.setDeadline("2026-10-" + (10 + (i % 15)));
            item.setPriority(priorities[i % priorities.length]);
            item.setStatus(i % 3 == 0 ? "In Progress" : "Pending");
            item.setDependencies(List.of("TSK-ARCH-" + Math.max(1, i - 1)));
            item.setSourceEvidence("Explicitly stated in transcript cue at [00:" + String.format("%02d", i) + ":15]: Team approved immediate execution of this milestone deliverable.");
            actionItems.add(item);
        }

        FollowUpPlan plan = new FollowUpPlan();
        plan.setMeetingId("largeMeeting456");
        plan.setOverview("Strategic execution roadmap for enterprise deployment pilot.");
        plan.setNextMeetingDate("2026-10-24 at 10:00 AM EST");
        plan.setSteps(List.of(
                "Finalize data contracts between Spring Boot backend and Flask multi-agent orchestrator",
                "Execute load tests against MongoDB replica set",
                "Conduct security audit of Groq API key isolation"
        ));
        plan.setCheckpoints(List.of(
                "Sprint checkpoint 1 on October 15",
                "Mid-sprint checkpoint 2 on October 19",
                "Final release readiness review on October 23"
        ));
        plan.setPendingClarifications(List.of(
                "Assign designated lead for Unassigned tasks",
                "Verify production load limits with cloud DevOps team"
        ));

        List<ReviewFinding> findings = List.of(
                new ReviewFinding("largeMeeting456", "ACCURACY", "PASS", "All 3 decisions explicitly verified against notes.", "Notes line 12-18", "Confirmed"),
                new ReviewFinding("largeMeeting456", "COMPLETENESS", "FLAG", "4 tasks currently marked Unassigned pending standup.", "Action Items table", "Flagged for user review")
        );

        byte[] pdfBytes = pdfReportService.generateFollowUpPlanPdf(
                meeting,
                actionItems,
                plan,
                findings
        );

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 5000, "Multi-page PDF should be substantially larger than 5KB");
        String header = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", header);
    }
}
