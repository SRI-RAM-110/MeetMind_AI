package com.meetmind.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TranscriptParseResponseDto {

    private String cleanedTranscript;
    private String format; // "VTT", "SRT", "GOOGLE_MEET_TXT", "RAW_TXT", "AUDIO_STT"
    private int speakerCount;
    private List<String> detectedSpeakers = new ArrayList<>();
    private int timestampCount;
    private List<String> transcriptTimestamps = new ArrayList<>();
    private String googleMeetingId;
    private int wordCount;
    private String suggestedTitle;
    private String suggestedDate;
    private List<String> detectedParticipants = new ArrayList<>();

    // Map of field key -> FieldProvenanceDto (e.g. title, date, participants, googleMeetingId)
    private Map<String, FieldProvenanceDto> metadataFields = new LinkedHashMap<>();

    public TranscriptParseResponseDto() {
    }

    public String getCleanedTranscript() {
        return cleanedTranscript;
    }

    public void setCleanedTranscript(String cleanedTranscript) {
        this.cleanedTranscript = cleanedTranscript;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getSpeakerCount() {
        return speakerCount;
    }

    public void setSpeakerCount(int speakerCount) {
        this.speakerCount = speakerCount;
    }

    public List<String> getDetectedSpeakers() {
        return detectedSpeakers != null ? detectedSpeakers : new ArrayList<>();
    }

    public void setDetectedSpeakers(List<String> detectedSpeakers) {
        this.detectedSpeakers = detectedSpeakers != null ? detectedSpeakers : new ArrayList<>();
    }

    public int getTimestampCount() {
        return timestampCount;
    }

    public void setTimestampCount(int timestampCount) {
        this.timestampCount = timestampCount;
    }

    public List<String> getTranscriptTimestamps() {
        return transcriptTimestamps != null ? transcriptTimestamps : new ArrayList<>();
    }

    public void setTranscriptTimestamps(List<String> transcriptTimestamps) {
        this.transcriptTimestamps = transcriptTimestamps != null ? transcriptTimestamps : new ArrayList<>();
    }

    public String getGoogleMeetingId() {
        return googleMeetingId;
    }

    public void setGoogleMeetingId(String googleMeetingId) {
        this.googleMeetingId = googleMeetingId;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public String getSuggestedTitle() {
        return suggestedTitle;
    }

    public void setSuggestedTitle(String suggestedTitle) {
        this.suggestedTitle = suggestedTitle;
    }

    public String getSuggestedDate() {
        return suggestedDate;
    }

    public void setSuggestedDate(String suggestedDate) {
        this.suggestedDate = suggestedDate;
    }

    public List<String> getDetectedParticipants() {
        return detectedParticipants != null ? detectedParticipants : new ArrayList<>();
    }

    public void setDetectedParticipants(List<String> detectedParticipants) {
        this.detectedParticipants = detectedParticipants != null ? detectedParticipants : new ArrayList<>();
    }

    public Map<String, FieldProvenanceDto> getMetadataFields() {
        return metadataFields != null ? metadataFields : new LinkedHashMap<>();
    }

    public void setMetadataFields(Map<String, FieldProvenanceDto> metadataFields) {
        this.metadataFields = metadataFields != null ? metadataFields : new LinkedHashMap<>();
    }
}
