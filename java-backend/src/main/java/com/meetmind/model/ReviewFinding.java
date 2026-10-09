package com.meetmind.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "review_findings")
public class ReviewFinding {

    @Id
    private String id;

    @Indexed
    private String meetingId;

    private String category;
    private String severity;
    private String issue;
    private String evidenceChecked;
    private String actionTaken;

    private Instant createdAt = Instant.now();

    public ReviewFinding() {
    }

    public ReviewFinding(String meetingId, String category, String severity, String issue, String evidenceChecked, String actionTaken) {
        this.meetingId = meetingId;
        this.category = (category == null || category.isBlank()) ? "AUDIT_CHECK" : category;
        this.severity = (severity == null || severity.isBlank()) ? "INFO" : severity;
        this.issue = (issue == null) ? "" : issue;
        this.evidenceChecked = (evidenceChecked == null) ? "" : evidenceChecked;
        this.actionTaken = (actionTaken == null) ? "" : actionTaken;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
