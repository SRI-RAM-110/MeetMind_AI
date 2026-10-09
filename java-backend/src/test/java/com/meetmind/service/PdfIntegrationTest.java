package com.meetmind.service;

import com.meetmind.controller.MeetingController;
import com.meetmind.model.ActionItem;
import com.meetmind.model.FollowUpPlan;
import com.meetmind.model.Meeting;
import com.meetmind.repository.ActionItemRepository;
import com.meetmind.repository.FollowUpPlanRepository;
import com.meetmind.repository.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PdfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ActionItemRepository actionItemRepository;

    @Autowired
    private FollowUpPlanRepository followUpPlanRepository;

    @Test
    void testDownloadPdfEndpoint_WithRealisticData() throws Exception {
        Meeting meeting = new Meeting();
        meeting.setMeetingTitle("Sprint Planning Q4");
        meeting.setMeetingDate("2026-10-10");
        meeting.setStartTime("10:00 AM");
        meeting.setEndTime("11:00 AM");
        meeting.setTimeZone("EST");
        meeting.setSummary("Reviewed roadmap deliverables and assigned core milestones.");
        meeting.setDecisions(List.of("Approved MongoDB Community Server", "Approved Python Flask LLM service"));
        meeting.setParticipants(List.of("Sarah Connor", "Alex Mercer", "Priya Sharma", "David Kim"));
        meeting = meetingRepository.save(meeting);

        ActionItem item1 = new ActionItem();
        item1.setMeetingId(meeting.getId());
        item1.setTaskId("TSK-1");
        item1.setTitle("Implement Spring Boot repositories");
        item1.setOwnerName("Priya Sharma");
        item1.setOwnerRole("Backend Lead");
        item1.setDeadline("2026-10-16");
        item1.setPriority("High");
        item1.setStatus("Pending");
        actionItemRepository.save(item1);

        FollowUpPlan plan = new FollowUpPlan();
        plan.setMeetingId(meeting.getId());
        plan.setOverview("Roadmap execution for Q4 pilot");
        plan.setNextMeetingDate("2026-10-23");
        plan.setSteps(List.of("Deploy backend", "Validate endpoints"));
        followUpPlanRepository.save(plan);

        byte[] responseBytes = mockMvc.perform(get("/api/meetings/" + meeting.getId() + "/follow-up-plan/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("MeetMind_FollowUpPlan_Sprint_Planning_Q4")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Verify valid PDF header
        String pdfHeader = new String(responseBytes, 0, 4);
        assertEquals("%PDF", pdfHeader);
    }
}
