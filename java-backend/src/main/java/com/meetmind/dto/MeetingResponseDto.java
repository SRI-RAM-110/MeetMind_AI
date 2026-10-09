package com.meetmind.dto;

import com.meetmind.model.Meeting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MeetingResponseDto {

    private String id;
    private String meetingTitle;
    private String meetingDate;
    private String startTime;
    private String endTime;
    private String timeZone;
    private String sourceType;
    private String sourceFileName;
    private String googleMeetingId;
    private Integer durationMinutes;

    private String originalNotes;
    private String transcriptText;
    private String summary;
    private List<String> decisions = new ArrayList<>();
    private List<String> participants = new ArrayList<>();
    private List<String> speakers = new ArrayList<>();
    private Map<String, FieldProvenanceDto> metadataProvenance = new java.util.LinkedHashMap<>();
    private String status;
    private String nextMeetingDate;
    private Instant createdAt;
    private Instant updatedAt;

    private List<ActionItemDto> actionItems = new ArrayList<>();
    private FollowUpPlanDto followUpPlan;
    private List<ReviewFindingDto> reviewFindings = new ArrayList<>();
    private TranscriptAnalysisDto transcriptAnalysis;
    private Map<String, Object> agentExecution;

    public MeetingResponseDto() {
    }

    public MeetingResponseDto(Meeting meeting, List<ActionItemDto> actionItems, FollowUpPlanDto followUpPlan) {
        this(meeting, actionItems, followUpPlan, new ArrayList<>(), null, null);
    }

    public MeetingResponseDto(Meeting meeting,
                              List<ActionItemDto> actionItems,
                              FollowUpPlanDto followUpPlan,
                              List<ReviewFindingDto> reviewFindings,
                              TranscriptAnalysisDto transcriptAnalysis,
                              Map<String, Object> agentExecution) {
        if (meeting != null) {
            this.id = meeting.getId();
            this.meetingTitle = meeting.getMeetingTitle();
            this.meetingDate = meeting.getMeetingDate();
            this.startTime = meeting.getStartTime();
            this.endTime = meeting.getEndTime();
            this.timeZone = meeting.getTimeZone();
            this.sourceType = meeting.getSourceType();
            this.sourceFileName = meeting.getSourceFileName();
            this.googleMeetingId = meeting.getGoogleMeetingId();
            this.durationMinutes = meeting.getDurationMinutes();
            this.originalNotes = meeting.getOriginalNotes();
            this.transcriptText = meeting.getTranscriptText();
            this.summary = meeting.getSummary();
            this.decisions = meeting.getDecisions() != null ? meeting.getDecisions() : new ArrayList<>();
            this.participants = meeting.getParticipants() != null ? meeting.getParticipants() : new ArrayList<>();
            this.speakers = meeting.getSpeakers() != null ? meeting.getSpeakers() : new ArrayList<>();
            this.metadataProvenance = meeting.getMetadataProvenance() != null ? meeting.getMetadataProvenance() : new java.util.LinkedHashMap<>();
            this.status = meeting.getStatus();
            this.nextMeetingDate = meeting.getNextMeetingDate();
            this.createdAt = meeting.getCreatedAt();
            this.updatedAt = meeting.getUpdatedAt();
        }
        this.actionItems = actionItems != null ? actionItems : new ArrayList<>();
        this.followUpPlan = followUpPlan;
        this.reviewFindings = reviewFindings != null ? reviewFindings : new ArrayList<>();
        this.transcriptAnalysis = transcriptAnalysis;
        this.agentExecution = agentExecution;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMeetingTitle() {
        return meetingTitle != null ? meetingTitle : "Business Meeting";
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
        return decisions;
    }

    public void setDecisions(List<String> decisions) {
        this.decisions = decisions;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNextMeetingDate() {
        return nextMeetingDate;
    }

    public void setNextMeetingDate(String nextMeetingDate) {
        this.nextMeetingDate = nextMeetingDate;
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

    public List<ActionItemDto> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<ActionItemDto> actionItems) {
        this.actionItems = actionItems;
    }

    public FollowUpPlanDto getFollowUpPlan() {
        return followUpPlan;
    }

    public void setFollowUpPlan(FollowUpPlanDto followUpPlan) {
        this.followUpPlan = followUpPlan;
    }

    public List<ReviewFindingDto> getReviewFindings() {
        return reviewFindings;
    }

    public void setReviewFindings(List<ReviewFindingDto> reviewFindings) {
        this.reviewFindings = reviewFindings;
    }

    public TranscriptAnalysisDto getTranscriptAnalysis() {
        return transcriptAnalysis;
    }

    public void setTranscriptAnalysis(TranscriptAnalysisDto transcriptAnalysis) {
        this.transcriptAnalysis = transcriptAnalysis;
    }

    public Map<String, Object> getAgentExecution() {
        return agentExecution;
    }

    public void setAgentExecution(Map<String, Object> agentExecution) {
        this.agentExecution = agentExecution;
    }

    public List<String> getSpeakers() {
        return speakers != null ? speakers : new ArrayList<>();
    }

    public void setSpeakers(List<String> speakers) {
        this.speakers = speakers != null ? speakers : new ArrayList<>();
    }

    public Map<String, FieldProvenanceDto> getMetadataProvenance() {
        return metadataProvenance != null ? metadataProvenance : new java.util.LinkedHashMap<>();
    }

    public void setMetadataProvenance(Map<String, FieldProvenanceDto> metadataProvenance) {
        this.metadataProvenance = metadataProvenance != null ? metadataProvenance : new java.util.LinkedHashMap<>();
    }
}
