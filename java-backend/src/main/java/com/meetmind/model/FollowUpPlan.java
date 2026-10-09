package com.meetmind.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "follow_up_plans")
public class FollowUpPlan {

    @Id
    private String id;

    @Indexed(unique = true)
    private String meetingId;

    private String overview = "Post-meeting execution and follow-up plan.";
    private List<String> steps = new ArrayList<>();
    private List<String> checkpoints = new ArrayList<>(); // AI recommended review points
    private List<String> upcomingReviewMeetings = new ArrayList<>();
    private List<String> pendingClarifications = new ArrayList<>();
    private List<String> unresolvedQuestions = new ArrayList<>();
    private String nextMeetingDate = "Not specified";

    private Instant createdAt = Instant.now();

    public FollowUpPlan() {
    }

    public FollowUpPlan(String meetingId, List<String> steps, List<String> unresolvedQuestions, String nextMeetingDate) {
        this.meetingId = meetingId;
        this.steps = steps != null ? steps : new ArrayList<>();
        this.unresolvedQuestions = unresolvedQuestions != null ? unresolvedQuestions : new ArrayList<>();
        this.pendingClarifications = this.unresolvedQuestions;
        this.nextMeetingDate = (nextMeetingDate == null || nextMeetingDate.isBlank()) ? "Not specified" : nextMeetingDate;
        this.createdAt = Instant.now();
    }

    public FollowUpPlan(String meetingId, String overview, List<String> steps,
                        List<String> checkpoints, List<String> upcomingReviewMeetings,
                        List<String> pendingClarifications, String nextMeetingDate) {
        this.meetingId = meetingId;
        this.overview = (overview != null && !overview.isBlank()) ? overview : "Post-meeting execution and follow-up plan.";
        this.steps = steps != null ? steps : new ArrayList<>();
        this.checkpoints = checkpoints != null ? checkpoints : new ArrayList<>();
        this.upcomingReviewMeetings = upcomingReviewMeetings != null ? upcomingReviewMeetings : new ArrayList<>();
        this.pendingClarifications = pendingClarifications != null ? pendingClarifications : new ArrayList<>();
        this.unresolvedQuestions = this.pendingClarifications;
        this.nextMeetingDate = (nextMeetingDate == null || nextMeetingDate.isBlank()) ? "Not specified" : nextMeetingDate;
        this.createdAt = Instant.now();
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
        return steps != null ? steps : new ArrayList<>();
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public List<String> getCheckpoints() {
        return checkpoints != null ? checkpoints : new ArrayList<>();
    }

    public void setCheckpoints(List<String> checkpoints) {
        this.checkpoints = checkpoints;
    }

    public List<String> getUpcomingReviewMeetings() {
        return upcomingReviewMeetings != null ? upcomingReviewMeetings : new ArrayList<>();
    }

    public void setUpcomingReviewMeetings(List<String> upcomingReviewMeetings) {
        this.upcomingReviewMeetings = upcomingReviewMeetings;
    }

    public List<String> getPendingClarifications() {
        return pendingClarifications != null ? pendingClarifications : (unresolvedQuestions != null ? unresolvedQuestions : new ArrayList<>());
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
