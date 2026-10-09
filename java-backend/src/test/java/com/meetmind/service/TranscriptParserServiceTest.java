package com.meetmind.service;

import com.meetmind.dto.FieldProvenanceDto;
import com.meetmind.dto.TranscriptParseResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TranscriptParserServiceTest {

    private TranscriptParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new TranscriptParserService();
    }

    @Test
    void testParseGoogleMeetTranscript_WithHeaderAndSpeakerCues() {
        String gmContent = """
                Transcript
                Meeting name: Q4 Architecture Review
                Date: October 10, 2026
                Meeting ID: meet.google.com/abc-defg-hij
                Time: 10:00 AM - 10:45 AM EDT
                Attendees: Sarah Connor, Alex Mercer, Priya Sharma

                Sarah Connor (10:00 AM): Welcome everyone to the architecture review.
                Alex Mercer (10:05 AM): Thanks Sarah. I have prepared the database schema diagram.
                Priya Sharma (10:15 AM): The Spring Boot backend is ready for integration.
                """;

        TranscriptParseResponseDto result = parserService.parseTranscript(gmContent, "meeting_transcript.txt");

        assertEquals("GOOGLE_MEET_TXT", result.getFormat());
        assertEquals("Q4 Architecture Review", result.getSuggestedTitle());
        assertEquals("October 10, 2026", result.getSuggestedDate());
        assertEquals("abc-defg-hij", result.getGoogleMeetingId());

        // Participants from header
        assertTrue(result.getDetectedParticipants().contains("Sarah Connor"));
        assertTrue(result.getDetectedParticipants().contains("Alex Mercer"));
        assertTrue(result.getDetectedParticipants().contains("Priya Sharma"));

        // Speakers from cues
        assertTrue(result.getDetectedSpeakers().contains("Sarah Connor"));
        assertTrue(result.getDetectedSpeakers().contains("Alex Mercer"));
        assertTrue(result.getDetectedSpeakers().contains("Priya Sharma"));

        // Provenance checks
        FieldProvenanceDto titleProv = result.getMetadataFields().get("title");
        assertNotNull(titleProv);
        assertEquals("VERIFIED", titleProv.getStatus());
        assertEquals("SOURCE_METADATA", titleProv.getSource());

        FieldProvenanceDto dateProv = result.getMetadataFields().get("date");
        assertNotNull(dateProv);
        assertEquals("VERIFIED", dateProv.getStatus());

        // Timing fields should not be extracted as metadata
        assertFalse(result.getMetadataFields().containsKey("startTime"));
        assertFalse(result.getMetadataFields().containsKey("endTime"));
        assertFalse(result.getMetadataFields().containsKey("durationMinutes"));
    }

    @Test
    void testParseWebVTT_ExtractVoiceTagsAndPreservesContent() {
        String vttContent = """
                WEBVTT
                NOTE This is a sprint planning recording

                00:00:01.000 --> 00:00:05.000
                <v Alice Smith>Good morning team, let's start.</v>

                00:00:06.000 --> 00:00:15.000
                <v Bob Jones>I have finished the user interface styling.</v>

                00:00:16.000 --> 00:30:00.000
                <v Alice Smith>Excellent work Bob.</v>
                """;

        TranscriptParseResponseDto result = parserService.parseTranscript(vttContent, "sprint_sync.vtt");

        assertEquals("VTT", result.getFormat());
        assertEquals(2, result.getSpeakerCount());
        assertTrue(result.getDetectedSpeakers().contains("Alice Smith"));
        assertTrue(result.getDetectedSpeakers().contains("Bob Jones"));

        // Date is not invented
        FieldProvenanceDto dateProv = result.getMetadataFields().get("date");
        assertNotNull(dateProv);
        assertEquals("NOT_AVAILABLE", dateProv.getStatus());
        assertNull(result.getSuggestedDate());

        // Timing fields not present
        assertFalse(result.getMetadataFields().containsKey("startTime"));
        assertFalse(result.getMetadataFields().containsKey("durationMinutes"));
    }

    @Test
    void testParseSRT_ExtractCuesAndSpeakers() {
        String srtContent = """
                1
                00:00:01,000 --> 00:00:04,500
                David Kim: Reviewing the frontend requirements today.

                2
                00:00:05,000 --> 00:15:00,000
                Elena Rostova: I will coordinate the deployment timeline.
                """;

        TranscriptParseResponseDto result = parserService.parseTranscript(srtContent, "demo.srt");

        assertEquals("SRT", result.getFormat());
        assertEquals(2, result.getSpeakerCount());
        assertTrue(result.getDetectedSpeakers().contains("David Kim"));
        assertTrue(result.getDetectedSpeakers().contains("Elena Rostova"));
    }

    @Test
    void testMissingMeetingDate_DoesNotInventValues() {
        String plainContent = """
                Project Roadmap Sync
                Discussed the feature rollout and resolved database scaling questions.
                Alice: We should deploy by Friday.
                Bob: Sounds good.
                """;

        TranscriptParseResponseDto result = parserService.parseTranscript(plainContent, "notes.txt");

        // Should NOT invent today's date
        assertNull(result.getSuggestedDate());
        FieldProvenanceDto dateProv = result.getMetadataFields().get("date");
        assertEquals("NOT_AVAILABLE", dateProv.getStatus());

        // Title inferred from first line
        assertEquals("Project Roadmap Sync", result.getSuggestedTitle());
        FieldProvenanceDto titleProv = result.getMetadataFields().get("title");
        assertEquals("INFERRED", titleProv.getStatus());
    }

    @Test
    void testExtractExplicitInlineDate() {
        String plainContent = """
                Meeting held on 2026-10-15 in conference room 3.
                Sarah: We decided to approve the budget.
                """;

        TranscriptParseResponseDto result = parserService.parseTranscript(plainContent, "notes.txt");

        assertEquals("2026-10-15", result.getSuggestedDate());

        FieldProvenanceDto dateProv = result.getMetadataFields().get("date");
        assertEquals("INFERRED", dateProv.getStatus());
        assertEquals("TRANSCRIPT_TEXT", dateProv.getSource());
    }
}
