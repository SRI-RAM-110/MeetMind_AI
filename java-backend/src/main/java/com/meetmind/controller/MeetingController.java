package com.meetmind.controller;

import com.meetmind.dto.*;
import com.meetmind.service.AiAgentClientService;
import com.meetmind.service.MeetingService;
import com.meetmind.service.TranscriptParserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final TranscriptParserService transcriptParserService;
    private final AiAgentClientService aiAgentClientService;

    public MeetingController(MeetingService meetingService,
                             TranscriptParserService transcriptParserService,
                             AiAgentClientService aiAgentClientService) {
        this.meetingService = meetingService;
        this.transcriptParserService = transcriptParserService;
        this.aiAgentClientService = aiAgentClientService;
    }

    /**
     * POST /api/meetings/parse-transcript
     * Parses uploaded transcript file (.txt, .vtt, .srt) or raw transcript text.
     * Extracts speaker labels, timestamps, duration, and clean text.
     */
    @PostMapping(value = "/parse-transcript", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<TranscriptParseResponseDto> parseTranscriptFile(
            @RequestParam("file") MultipartFile file) {
        TranscriptParseResponseDto response = transcriptParserService.parseFile(file);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/meetings/parse-transcript-text
     * Parses raw pasted text or transcript format.
     */
    @PostMapping(value = "/parse-transcript-text", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<TranscriptParseResponseDto> parseTranscriptText(
            @RequestBody java.util.Map<String, String> payload) {
        String rawText = payload.getOrDefault("text", "");
        TranscriptParseResponseDto response = transcriptParserService.parseText(rawText);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/meetings/transcribe
     * Accepts meeting audio or video file (WAV, MP3, M4A, MP4) and transcribes using Groq Whisper API.
     */
    @PostMapping(value = "/transcribe", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<TranscriptionResponseDto> transcribeAudio(
            @RequestParam("file") MultipartFile file) {
        TranscriptionResponseDto response = aiAgentClientService.transcribeAudio(file);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/meetings/analyze
     * Validates meeting notes and metadata, invokes Python AI agent, persists in MongoDB, returns structured response.
     */
    @PostMapping("/analyze")
    public ResponseEntity<MeetingResponseDto> analyzeMeeting(@Valid @RequestBody AnalyzeRequest request) {
        MeetingResponseDto response = meetingService.analyzeAndSaveMeeting(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/meetings
     * Returns history of meetings sorted by creation time descending.
     */
    @GetMapping
    public ResponseEntity<List<MeetingSummaryDto>> getAllMeetings() {
        List<MeetingSummaryDto> meetings = meetingService.getAllMeetings();
        return ResponseEntity.ok(meetings);
    }

    /**
     * GET /api/meetings/{id}
     * Returns full meeting details including action items and follow-up plan.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponseDto> getMeetingById(@PathVariable("id") String id) {
        MeetingResponseDto response = meetingService.getMeetingById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/meetings/{id}/follow-up-plan/pdf
     * Downloads professional 7-section PDF report of the meeting follow-up plan.
     */
    @GetMapping("/{id}/follow-up-plan/pdf")
    public ResponseEntity<byte[]> downloadFollowUpPlanPdf(@PathVariable("id") String id) {
        MeetingResponseDto meetingDto = meetingService.getMeetingById(id);
        byte[] pdfBytes = meetingService.generateMeetingReportPdf(id);

        String title = meetingDto.getMeetingTitle();
        String date = meetingDto.getMeetingDate();
        String safeTitle = (title != null && !title.isBlank()) ? title.replaceAll("[^a-zA-Z0-9_.-]", "_") : "Meeting";
        String safeDate = (date != null && !date.isBlank()) ? date.replaceAll("[^a-zA-Z0-9_.-]", "_") : id;
        String filename = "MeetMind_FollowUpPlan_" + safeTitle + "_" + safeDate + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    /**
     * GET /api/meetings/{id}/actions.csv
     * Downloads action items formatted as CSV.
     */
    @GetMapping("/{id}/actions.csv")
    public ResponseEntity<byte[]> downloadActionItemsCsv(@PathVariable("id") String id) {
        String csvContent = meetingService.generateActionItemsCsv(id);
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"action_items.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /**
     * GET /api/meetings/{id}/follow-up.json
     * Downloads follow-up plan formatted as pretty JSON.
     */
    @GetMapping("/{id}/follow-up.json")
    public ResponseEntity<byte[]> downloadFollowUpPlanJson(@PathVariable("id") String id) {
        String jsonContent = meetingService.generateFollowUpPlanJson(id);
        byte[] bytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"follow_up_plan.json\"")
                .contentType(MediaType.parseMediaType("application/json; charset=UTF-8"))
                .body(bytes);
    }
}
