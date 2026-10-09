package com.meetmind.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAgentResponse {

    private String summary;
    private List<String> decisions = new ArrayList<>();

    @JsonProperty("action_items")
    private List<RawActionItem> actionItems = new ArrayList<>();

    @JsonProperty("follow_up_plan")
    private RawFollowUpPlan followUpPlan;

    @JsonProperty("next_meeting_date")
    private String nextMeetingDate;

    @JsonProperty("unresolved_questions")
    private List<String> unresolvedQuestions = new ArrayList<>();

    @JsonProperty("review_findings")
    private List<RawReviewFinding> reviewFindings = new ArrayList<>();

    @JsonProperty("transcript_analysis")
    private RawTranscriptAnalysis transcriptAnalysis;

    @JsonProperty("agent_execution")
    private Map<String, Object> agentExecution;

    public AiAgentResponse() {
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
        this.decisions = decisions;
    }

    public List<RawActionItem> getActionItems() {
        return actionItems != null ? actionItems : new ArrayList<>();
    }

    public void setActionItems(List<RawActionItem> actionItems) {
        this.actionItems = actionItems;
    }

    public RawFollowUpPlan getFollowUpPlan() {
        return followUpPlan;
    }

    public void setFollowUpPlan(RawFollowUpPlan followUpPlan) {
        this.followUpPlan = followUpPlan;
    }

    public String getNextMeetingDate() {
        return nextMeetingDate;
    }

    public void setNextMeetingDate(String nextMeetingDate) {
        this.nextMeetingDate = nextMeetingDate;
    }

    public List<String> getUnresolvedQuestions() {
        return unresolvedQuestions != null ? unresolvedQuestions : new ArrayList<>();
    }

    public void setUnresolvedQuestions(List<String> unresolvedQuestions) {
        this.unresolvedQuestions = unresolvedQuestions;
    }

    public List<RawReviewFinding> getReviewFindings() {
        return reviewFindings != null ? reviewFindings : new ArrayList<>();
    }

    public void setReviewFindings(List<RawReviewFinding> reviewFindings) {
        this.reviewFindings = reviewFindings;
    }

    public RawTranscriptAnalysis getTranscriptAnalysis() {
        return transcriptAnalysis;
    }

    public void setTranscriptAnalysis(RawTranscriptAnalysis transcriptAnalysis) {
        this.transcriptAnalysis = transcriptAnalysis;
    }

    public Map<String, Object> getAgentExecution() {
        return agentExecution;
    }

    public void setAgentExecution(Map<String, Object> agentExecution) {
        this.agentExecution = agentExecution;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawActionItem {
        @JsonProperty("task_id")
        private String taskId;
        private String title;
        private String description;
        private String task;
        private String owner;
        @JsonProperty("owner_role")
        private String ownerRole;
        private String deadline;
        private String priority = "Medium";
        private String status = "Pending";
        private List<String> dependencies = new ArrayList<>();
        private String evidence;
        @JsonProperty("source_timestamp")
        private String sourceTimestamp;
        private Double confidence = 1.0;
        @JsonProperty("needs_confirmation")
        private Boolean needsConfirmation = false;

        public RawActionItem() {
        }

        public RawActionItem(String task, String owner, String deadline, String status, String evidence) {
            this.task = task;
            this.owner = owner;
            this.deadline = deadline;
            this.status = status;
            this.evidence = evidence;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTask() {
            return task;
        }

        public void setTask(String task) {
            this.task = task;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getOwnerRole() {
            return ownerRole;
        }

        public void setOwnerRole(String ownerRole) {
            this.ownerRole = ownerRole;
        }

        public String getDeadline() {
            return deadline;
        }

        public void setDeadline(String deadline) {
            this.deadline = deadline;
        }

        public String getPriority() {
            return priority != null ? priority : "Medium";
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getDependencies() {
            return dependencies != null ? dependencies : new ArrayList<>();
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }

        public String getEvidence() {
            return evidence;
        }

        public void setEvidence(String evidence) {
            this.evidence = evidence;
        }

        public String getSourceTimestamp() {
            return sourceTimestamp;
        }

        public void setSourceTimestamp(String sourceTimestamp) {
            this.sourceTimestamp = sourceTimestamp;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public Boolean getNeedsConfirmation() {
            return needsConfirmation != null ? needsConfirmation : false;
        }

        public void setNeedsConfirmation(Boolean needsConfirmation) {
            this.needsConfirmation = needsConfirmation;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawFollowUpPlan {
        private String overview;
        private List<String> steps = new ArrayList<>();
        private List<String> checkpoints = new ArrayList<>();

        @JsonProperty("upcoming_review_meetings")
        private List<String> upcomingReviewMeetings = new ArrayList<>();

        @JsonProperty("pending_clarifications")
        private List<String> pendingClarifications = new ArrayList<>();

        @JsonProperty("unresolved_questions")
        private List<String> unresolvedQuestions = new ArrayList<>();

        @JsonProperty("next_meeting_date")
        private String nextMeetingDate;

        public RawFollowUpPlan() {
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
            return pendingClarifications != null ? pendingClarifications : new ArrayList<>();
        }

        public void setPendingClarifications(List<String> pendingClarifications) {
            this.pendingClarifications = pendingClarifications;
        }

        public List<String> getUnresolvedQuestions() {
            return unresolvedQuestions != null ? unresolvedQuestions : new ArrayList<>();
        }

        public void setUnresolvedQuestions(List<String> unresolvedQuestions) {
            this.unresolvedQuestions = unresolvedQuestions;
        }

        public String getNextMeetingDate() {
            return nextMeetingDate;
        }

        public void setNextMeetingDate(String nextMeetingDate) {
            this.nextMeetingDate = nextMeetingDate;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawReviewFinding {
        private String category;
        private String severity;
        private String issue;

        @JsonProperty("evidence_checked")
        private String evidenceChecked;

        @JsonProperty("action_taken")
        private String actionTaken;

        public RawReviewFinding() {
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getIssue() {
            return issue;
        }

        public void setIssue(String issue) {
            this.issue = issue;
        }

        public String getEvidenceChecked() {
            return evidenceChecked;
        }

        public void setEvidenceChecked(String evidenceChecked) {
            this.evidenceChecked = evidenceChecked;
        }

        public String getActionTaken() {
            return actionTaken;
        }

        public void setActionTaken(String actionTaken) {
            this.actionTaken = actionTaken;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawTranscriptAnalysis {
        private List<String> topics = new ArrayList<>();
        private List<String> participants = new ArrayList<>();

        @JsonProperty("key_discussion_points")
        private List<String> keyDiscussionPoints = new ArrayList<>();

        public RawTranscriptAnalysis() {
        }

        public List<String> getTopics() {
            return topics != null ? topics : new ArrayList<>();
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }

        public List<String> getParticipants() {
            return participants != null ? participants : new ArrayList<>();
        }

        public void setParticipants(List<String> participants) {
            this.participants = participants;
        }

        public List<String> getKeyDiscussionPoints() {
            return keyDiscussionPoints != null ? keyDiscussionPoints : new ArrayList<>();
        }

        public void setKeyDiscussionPoints(List<String> keyDiscussionPoints) {
            this.keyDiscussionPoints = keyDiscussionPoints;
        }
    }
}
