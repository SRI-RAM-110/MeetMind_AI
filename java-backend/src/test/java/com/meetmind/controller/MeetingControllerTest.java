package com.meetmind.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.dto.AnalyzeRequest;
import com.meetmind.dto.MeetingResponseDto;
import com.meetmind.exception.GlobalExceptionHandler;
import com.meetmind.exception.MeetingNotFoundException;
import com.meetmind.model.Meeting;
import com.meetmind.service.MeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class MeetingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MeetingService meetingService;

    @Mock
    private com.meetmind.service.TranscriptParserService transcriptParserService;

    @Mock
    private com.meetmind.service.AiAgentClientService aiAgentClientService;

    @InjectMocks
    private MeetingController meetingController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(meetingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testAnalyzeMeeting_Success() throws Exception {
        Meeting meeting = new Meeting("Notes", "Executive Summary", List.of("Decision 1"), "COMPLETED", "2026-10-16", "hash123");
        meeting.setId("mockMeetingId123");

        MeetingResponseDto mockResponse = new MeetingResponseDto(meeting, List.of(), null);
        when(meetingService.analyzeAndSaveMeeting(any(AnalyzeRequest.class))).thenReturn(mockResponse);

        AnalyzeRequest request = new AnalyzeRequest("This is a valid meeting notes text with sufficient length.");

        mockMvc.perform(post("/api/meetings/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("mockMeetingId123"))
                .andExpect(jsonPath("$.summary").value("Executive Summary"))
                .andExpect(jsonPath("$.decisions[0]").value("Decision 1"));
    }

    @Test
    void testAnalyzeMeeting_ValidationError_EmptyNotes() throws Exception {
        AnalyzeRequest request = new AnalyzeRequest("");

        mockMvc.perform(post("/api/meetings/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void testGetMeetingById_Success() throws Exception {
        Meeting meeting = new Meeting("Notes", "Summary text", List.of(), "COMPLETED", "2026-10-20", "hash456");
        meeting.setId("id789");

        MeetingResponseDto mockResponse = new MeetingResponseDto(meeting, List.of(), null);
        when(meetingService.getMeetingById("id789")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/meetings/id789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id789"))
                .andExpect(jsonPath("$.summary").value("Summary text"));
    }

    @Test
    void testGetMeetingById_NotFound() throws Exception {
        when(meetingService.getMeetingById("nonexistent")).thenThrow(new MeetingNotFoundException("Meeting not found with ID: nonexistent"));

        mockMvc.perform(get("/api/meetings/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testDownloadActionItemsCsv() throws Exception {
        when(meetingService.generateActionItemsCsv("id789")).thenReturn("Task,Owner,Deadline,Status,Evidence\n\"Deploy API\",\"Bob\",\"Oct 14\",\"Pending\",\"Bob will deploy\"");

        mockMvc.perform(get("/api/meetings/id789/actions.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"action_items.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"));
    }

    @Test
    void testDownloadFollowUpPlanJson() throws Exception {
        when(meetingService.generateFollowUpPlanJson("id789")).thenReturn("{\"steps\":[\"Step 1\"]}");

        mockMvc.perform(get("/api/meetings/id789/follow-up.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"follow_up_plan.json\""))
                .andExpect(content().contentType("application/json; charset=UTF-8"));
    }

    @Test
    void testDownloadFollowUpPlanPdf_Success() throws Exception {
        byte[] mockPdfBytes = "%PDF-1.4 Mock Binary Content for Follow-Up Plan".getBytes();
        Meeting meeting = new Meeting("Notes", "Summary", List.of(), "COMPLETED", "2026-10-20", "hash");
        meeting.setId("id789");
        meeting.setMeetingTitle("Sprint Planning");
        meeting.setMeetingDate("2026-10-10");

        MeetingResponseDto mockDto = new MeetingResponseDto(meeting, List.of(), null);
        when(meetingService.getMeetingById("id789")).thenReturn(mockDto);
        when(meetingService.generateMeetingReportPdf("id789")).thenReturn(mockPdfBytes);

        mockMvc.perform(get("/api/meetings/id789/follow-up-plan/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"MeetMind_FollowUpPlan_Sprint_Planning_2026-10-10.pdf\""))
                .andExpect(content().bytes(mockPdfBytes));
    }

    @Test
    void testDownloadFollowUpPlanPdf_NotFound() throws Exception {
        when(meetingService.getMeetingById("nonexistent")).thenThrow(new MeetingNotFoundException("Meeting not found with ID: nonexistent"));

        mockMvc.perform(get("/api/meetings/nonexistent/follow-up-plan/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
