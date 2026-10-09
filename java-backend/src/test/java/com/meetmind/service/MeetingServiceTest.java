package com.meetmind.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.dto.AiAgentResponse;
import com.meetmind.dto.MeetingResponseDto;
import com.meetmind.exception.MeetingNotFoundException;
import com.meetmind.model.ActionItem;
import com.meetmind.model.FollowUpPlan;
import com.meetmind.model.Meeting;
import com.meetmind.model.ReviewFinding;
import com.meetmind.repository.ActionItemRepository;
import com.meetmind.repository.FollowUpPlanRepository;
import com.meetmind.repository.MeetingRepository;
import com.meetmind.repository.ReviewFindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private ActionItemRepository actionItemRepository;

    @Mock
    private FollowUpPlanRepository followUpPlanRepository;

    @Mock
    private ReviewFindingRepository reviewFindingRepository;

    @Mock
    private AiAgentClientService aiAgentClientService;

    @Mock
    private PdfReportService pdfReportService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        meetingService = new MeetingService(
                meetingRepository,
                actionItemRepository,
                followUpPlanRepository,
                reviewFindingRepository,
                aiAgentClientService,
                objectMapper,
                pdfReportService
        );
    }

    @Test
    void testAnalyzeAndSaveMeeting_NewMeeting() {
        String notes = "Discussed Q4 roadmap and agreed to deploy the new auth service by Nov 1.";

        when(meetingRepository.findByNotesHash(anyString())).thenReturn(Optional.empty());

        AiAgentResponse agentResponse = new AiAgentResponse();
        agentResponse.setSummary("Roadmap discussion for Q4.");
        agentResponse.setDecisions(List.of("Deploy auth service"));
        agentResponse.setNextMeetingDate("Nov 5");

        AiAgentResponse.RawActionItem item = new AiAgentResponse.RawActionItem(
                "Deploy auth service", "Alice", "Nov 1", "Pending", "Agreed to deploy by Nov 1"
        );
        agentResponse.setActionItems(List.of(item));

        AiAgentResponse.RawFollowUpPlan plan = new AiAgentResponse.RawFollowUpPlan();
        plan.setSteps(List.of("Run security audit"));
        agentResponse.setFollowUpPlan(plan);

        AiAgentResponse.RawReviewFinding rawFinding = new AiAgentResponse.RawReviewFinding();
        rawFinding.setCategory("ACCURACY_VERIFIED");
        rawFinding.setSeverity("PASS");
        rawFinding.setIssue("All decisions verified");
        rawFinding.setEvidenceChecked("Deploy auth service");
        rawFinding.setActionTaken("Confirmed");
        agentResponse.setReviewFindings(List.of(rawFinding));

        when(aiAgentClientService.analyzeMeeting(notes)).thenReturn(agentResponse);

        Meeting savedMeeting = new Meeting(notes, "Roadmap discussion for Q4.", List.of("Deploy auth service"), "COMPLETED", "Nov 5", "hash");
        savedMeeting.setId("meeting123");
        when(meetingRepository.save(any(Meeting.class))).thenReturn(savedMeeting);

        ActionItem savedActionItem = new ActionItem("meeting123", "Deploy auth service", "Alice", "Nov 1", "Pending", "evidence");
        savedActionItem.setId("action123");
        when(actionItemRepository.save(any(ActionItem.class))).thenReturn(savedActionItem);

        FollowUpPlan savedPlan = new FollowUpPlan("meeting123", List.of("Run security audit"), List.of(), "Nov 5");
        savedPlan.setId("plan123");
        when(followUpPlanRepository.save(any(FollowUpPlan.class))).thenReturn(savedPlan);

        ReviewFinding savedFinding = new ReviewFinding("meeting123", "ACCURACY_VERIFIED", "PASS", "All decisions verified", "Deploy auth service", "Confirmed");
        savedFinding.setId("finding123");
        when(reviewFindingRepository.save(any(ReviewFinding.class))).thenReturn(savedFinding);

        MeetingResponseDto result = meetingService.analyzeAndSaveMeeting(notes);

        assertNotNull(result);
        assertEquals("meeting123", result.getId());
        assertEquals("Roadmap discussion for Q4.", result.getSummary());
        assertEquals(1, result.getActionItems().size());
        assertEquals("Alice", result.getActionItems().get(0).getOwner());
        assertNotNull(result.getFollowUpPlan());
        assertEquals(1, result.getReviewFindings().size());
        assertEquals("PASS", result.getReviewFindings().get(0).getSeverity());

        verify(meetingRepository, times(1)).save(any(Meeting.class));
        verify(actionItemRepository, times(1)).save(any(ActionItem.class));
        verify(followUpPlanRepository, times(1)).save(any(FollowUpPlan.class));
        verify(reviewFindingRepository, times(1)).save(any(ReviewFinding.class));
    }

    @Test
    void testAnalyzeAndSaveMeeting_DuplicateNotesAvoidsDuplicateSave() {
        String notes = "Repeated notes to verify duplicate prevention logic.";

        Meeting existing = new Meeting(notes, "Existing summary", List.of(), "COMPLETED", "None", "hash");
        existing.setId("existingId");

        when(meetingRepository.findByNotesHash(anyString())).thenReturn(Optional.of(existing));
        when(actionItemRepository.findByMeetingId("existingId")).thenReturn(List.of());
        when(followUpPlanRepository.findByMeetingId("existingId")).thenReturn(Optional.empty());
        when(reviewFindingRepository.findByMeetingId("existingId")).thenReturn(List.of());

        MeetingResponseDto result = meetingService.analyzeAndSaveMeeting(notes);

        assertEquals("existingId", result.getId());
        verify(aiAgentClientService, never()).analyzeMeeting(anyString());
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void testGetMeetingById_NotFound() {
        when(meetingRepository.findById("unknownId")).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> meetingService.getMeetingById("unknownId"));
    }

    @Test
    void testGenerateActionItemsCsv() {
        Meeting meeting = new Meeting("notes", "sum", List.of(), "COMPLETED", "None", "hash");
        meeting.setId("m1");
        when(meetingRepository.findById("m1")).thenReturn(Optional.of(meeting));

        ActionItem item = new ActionItem("m1", "Review code, with comma", "John", "Oct 15", "Pending", "\"Quote\" evidence");
        when(actionItemRepository.findByMeetingId("m1")).thenReturn(List.of(item));

        String csv = meetingService.generateActionItemsCsv("m1");
        assertTrue(csv.contains("Task,Owner,Deadline,Status,Evidence"));
        assertTrue(csv.contains("\"Review code, with comma\""));
        assertTrue(csv.contains("\"John\""));
        assertTrue(csv.contains("\"\"\"Quote\"\" evidence\""));
    }

    @Test
    void testGenerateMeetingReportPdf_Success() {
        Meeting meeting = new Meeting("notes", "sum", List.of(), "COMPLETED", "None", "hash");
        meeting.setId("m1");
        when(meetingRepository.findById("m1")).thenReturn(Optional.of(meeting));
        when(actionItemRepository.findByMeetingId("m1")).thenReturn(List.of());
        when(followUpPlanRepository.findByMeetingId("m1")).thenReturn(Optional.empty());
        when(reviewFindingRepository.findByMeetingId("m1")).thenReturn(List.of());

        byte[] expectedPdf = "%PDF-1.4 Mock Bytes".getBytes();
        when(pdfReportService.generateFollowUpPlanPdf(eq(meeting), anyList(), isNull(), anyList()))
                .thenReturn(expectedPdf);

        byte[] actualPdf = meetingService.generateMeetingReportPdf("m1");
        assertNotNull(actualPdf);
        assertArrayEquals(expectedPdf, actualPdf);
        verify(pdfReportService, times(1)).generateFollowUpPlanPdf(eq(meeting), anyList(), isNull(), anyList());
    }

    @Test
    void testGenerateMeetingReportPdf_NotFound() {
        when(meetingRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(MeetingNotFoundException.class, () -> meetingService.generateMeetingReportPdf("nonexistent"));
        verify(pdfReportService, never()).generateFollowUpPlanPdf(any(), any(), any(), any());
    }
}
