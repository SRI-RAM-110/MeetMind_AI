package com.meetmind.dto;

import com.meetmind.model.Meeting;

import java.time.Instant;

public class MeetingSummaryDto {

    private String id;
    private String meetingTitle;
    private String meetingDate;
    private String startTime;
    private String endTime;
    private String timeZone;
    private String sourceType;
    private String sourceFileName;
    private Integer durationMinutes;

    private String summary;
    private String status;
    private String nextMeetingDate;
    private int actionItemCount;
    private int decisionCount;
    private Instant createdAt;

    public MeetingSummaryDto() {
    }

    public MeetingSummaryDto(Meeting meeting, int actionItemCount) {
        if (meeting != null) {
            this.id = meeting.getId();
            this.meetingTitle = meeting.getMeetingTitle();
            this.meetingDate = meeting.getMeetingDate();
            this.startTime = meeting.getStartTime();
            this.endTime = meeting.getEndTime();
            this.timeZone = meeting.getTimeZone();
            this.sourceType = meeting.getSourceType();
            this.sourceFileName = meeting.getSourceFileName();
            this.durationMinutes = meeting.getDurationMinutes();
            this.summary = meeting.getSummary();
            this.status = meeting.getStatus();
            this.nextMeetingDate = meeting.getNextMeetingDate();
            this.decisionCount = meeting.getDecisions() != null ? meeting.getDecisions().size() : 0;
            this.createdAt = meeting.getCreatedAt();
        }
        this.actionItemCount = actionItemCount;
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

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public int getActionItemCount() {
        return actionItemCount;
    }

    public void setActionItemCount(int actionItemCount) {
        this.actionItemCount = actionItemCount;
    }

    public int getDecisionCount() {
        return decisionCount;
    }

    public void setDecisionCount(int decisionCount) {
        this.decisionCount = decisionCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
