package com.meetmind.dto;

import com.meetmind.model.FollowUpPlan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FollowUpPlanDto {

    private String id;
    private String meetingId;
    private String overview = "Post-meeting execution and follow-up plan.";
    private List<String> steps = new ArrayList<>();
    private List<String> checkpoints = new ArrayList<>();
    private List<String> upcomingReviewMeetings = new ArrayList<>();
    private List<String> pendingClarifications = new ArrayList<>();
    private List<String> unresolvedQuestions = new ArrayList<>();
    private String nextMeetingDate = "Not specified";
    private Instant createdAt;

    public FollowUpPlanDto() {
    }

    public FollowUpPlanDto(FollowUpPlan entity) {
        if (entity != null) {
            this.id = entity.getId();
            this.meetingId = entity.getMeetingId();
            this.overview = entity.getOverview();
            this.steps = entity.getSteps() != null ? entity.getSteps() : new ArrayList<>();
            this.checkpoints = entity.getCheckpoints() != null ? entity.getCheckpoints() : new ArrayList<>();
            this.upcomingReviewMeetings = entity.getUpcomingReviewMeetings() != null ? entity.getUpcomingReviewMeetings() : new ArrayList<>();
            this.pendingClarifications = entity.getPendingClarifications() != null ? entity.getPendingClarifications() : new ArrayList<>();
            this.unresolvedQuestions = this.pendingClarifications;
            this.nextMeetingDate = entity.getNextMeetingDate();
            this.createdAt = entity.getCreatedAt();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public List<String> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<String> checkpoints) {
        this.checkpoints = checkpoints;
    }

    public List<String> getUpcomingReviewMeetings() {
        return upcomingReviewMeetings;
    }

    public void setUpcomingReviewMeetings(List<String> upcomingReviewMeetings) {
        this.upcomingReviewMeetings = upcomingReviewMeetings;
    }

    public List<String> getPendingClarifications() {
        return pendingClarifications != null ? pendingClarifications : unresolvedQuestions;
    }

    public void setPendingClarifications(List<String> pendingClarifications) {
        this.pendingClarifications = pendingClarifications;
        this.unresolvedQuestions = pendingClarifications;
    }

    public List<String> getUnresolvedQuestions() {
        return getPendingClarifications();
    }

    public void setUnresolvedQuestions(List<String> unresolvedQuestions) {
        setPendingClarifications(unresolvedQuestions);
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
}
