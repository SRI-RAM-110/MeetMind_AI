package com.meetmind.model;

import com.meetmind.dto.FieldProvenanceDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "meetings")
public class Meeting {

    @Id
    private String id;

    private String meetingTitle = "Business Meeting";
    private String meetingDate; // YYYY-MM-DD
    private String startTime;   // HH:mm or 12-hour
    private String endTime;     // HH:mm or 12-hour
    private String timeZone = "UTC";
    private String sourceType = "PASTE"; // PASTE, UPLOAD_TRANSCRIPT, GOOGLE_MEET, AUDIO_VIDEO
    private String sourceFileName;
    private String googleMeetingId;
    private Integer durationMinutes;

    private String originalNotes;
    private String transcriptText;
    private String summary;
    private List<String> decisions = new ArrayList<>();
    private List<String> participants = new ArrayList<>();
    private List<String> speakers = new ArrayList<>();
    private Map<String, FieldProvenanceDto> metadataProvenance = new LinkedHashMap<>();
    private String status = "COMPLETED"; // "COMPLETED", "FAILED"

    @Indexed
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    private String nextMeetingDate;

    @Indexed
    private String notesHash;

    public Meeting() {
    }

    public Meeting(String originalNotes, String summary, List<String> decisions, String status, String nextMeetingDate, String notesHash) {
        this.originalNotes = originalNotes;
        this.transcriptText = originalNotes;
        this.summary = summary;
        this.decisions = decisions != null ? decisions : new ArrayList<>();
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.nextMeetingDate = nextMeetingDate;
        this.notesHash = notesHash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = (meetingTitle != null && !meetingTitle.isBlank()) ? meetingTitle : "Business Meeting";
    }

    public String getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(String meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = (timeZone != null && !timeZone.isBlank()) ? timeZone : "UTC";
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getGoogleMeetingId() {
        return googleMeetingId;
    }

    public void setGoogleMeetingId(String googleMeetingId) {
        this.googleMeetingId = googleMeetingId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getOriginalNotes() {
        return originalNotes;
    }

    public void setOriginalNotes(String originalNotes) {
        this.originalNotes = originalNotes;
        if (this.transcriptText == null) {
            this.transcriptText = originalNotes;
        }
    }

    public String getTranscriptText() {
        return transcriptText != null ? transcriptText : originalNotes;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getDecisions() {
        return decisions != null ? decisions : new ArrayList<>();
    }

    public void setDecisions(List<String> decisions) {
        this.decisions = decisions != null ? decisions : new ArrayList<>();
    }

    public List<String> getParticipants() {
        return participants != null ? participants : new ArrayList<>();
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants != null ? participants : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNextMeetingDate() {
        return nextMeetingDate;
    }

    public void setNextMeetingDate(String nextMeetingDate) {
        this.nextMeetingDate = nextMeetingDate;
    }

    public String getNotesHash() {
        return notesHash;
    }

    public void setNotesHash(String notesHash) {
        this.notesHash = notesHash;
    }

    public List<String> getSpeakers() {
        return speakers != null ? speakers : new ArrayList<>();
    }

    public void setSpeakers(List<String> speakers) {
        this.speakers = speakers != null ? speakers : new ArrayList<>();
    }

    public Map<String, FieldProvenanceDto> getMetadataProvenance() {
        return metadataProvenance != null ? metadataProvenance : new LinkedHashMap<>();
    }

    public void setMetadataProvenance(Map<String, FieldProvenanceDto> metadataProvenance) {
        this.metadataProvenance = metadataProvenance != null ? metadataProvenance : new LinkedHashMap<>();
    }
}
