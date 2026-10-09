package com.meetmind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyzeRequest {

    @NotBlank(message = "Meeting notes cannot be empty.")
    @Size(min = 10, message = "Meeting notes must be at least 10 characters long.")
    private String meetingNotes;

    private String meetingTitle;
    private String meetingDate;
    private String startTime;
    private String endTime;
    private String timeZone;
    private String sourceType = "PASTE";
    private String sourceFileName;
    private String googleMeetingId;
    private Integer durationMinutes;
    private List<String> participants = new ArrayList<>();
    private List<String> speakers = new ArrayList<>();
    private Map<String, FieldProvenanceDto> metadataProvenance = new LinkedHashMap<>();

    public AnalyzeRequest() {
    }

    public AnalyzeRequest(String meetingNotes) {
        this.meetingNotes = meetingNotes;
    }

    public String getMeetingNotes() {
        return meetingNotes;
    }

    public void setMeetingNotes(String meetingNotes) {
        this.meetingNotes = meetingNotes;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
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
        this.timeZone = timeZone;
    }

    public String getSourceType() {
        return sourceType != null ? sourceType : "PASTE";
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

    public List<String> getParticipants() {
        return participants != null ? participants : new ArrayList<>();
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants != null ? participants : new ArrayList<>();
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
