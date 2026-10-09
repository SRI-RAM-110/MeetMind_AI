package com.meetmind.dto;

import com.meetmind.model.ReviewFinding;

public class ReviewFindingDto {

    private String id;
    private String meetingId;
    private String category;
    private String severity;
    private String issue;
    private String evidenceChecked;
    private String actionTaken;

    public ReviewFindingDto() {
    }

    public ReviewFindingDto(ReviewFinding finding) {
        if (finding != null) {
            this.id = finding.getId();
            this.meetingId = finding.getMeetingId();
            this.category = finding.getCategory();
            this.severity = finding.getSeverity();
            this.issue = finding.getIssue();
            this.evidenceChecked = finding.getEvidenceChecked();
            this.actionTaken = finding.getActionTaken();
        }
    }

    public ReviewFindingDto(String category, String severity, String issue, String evidenceChecked, String actionTaken) {
        this.category = category;
        this.severity = severity;
        this.issue = issue;
        this.evidenceChecked = evidenceChecked;
        this.actionTaken = actionTaken;
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
}
