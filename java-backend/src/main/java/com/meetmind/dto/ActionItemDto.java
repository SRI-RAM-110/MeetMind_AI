package com.meetmind.dto;

import com.meetmind.model.ActionItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ActionItemDto {

    private String id;
    private String meetingId;
    private String taskId;
    private String title;
    private String description;
    private String ownerName;
    private String ownerRole;
    private String deadline;
    private String priority;
    private String status;
    private List<String> dependencies = new ArrayList<>();
    private String sourceEvidence;
    private String sourceTimestamp;
    private String confidence;
    private boolean needsConfirmation;
    private Instant createdAt;

    // Backwards-compatible aliases
    private String task;
    private String owner;
    private String evidence;

    public ActionItemDto() {
    }

    public ActionItemDto(ActionItem entity) {
        if (entity != null) {
            this.id = entity.getId();
            this.meetingId = entity.getMeetingId();
            this.taskId = entity.getTaskId();
            this.title = entity.getTitle();
            this.description = entity.getDescription();
            this.ownerName = entity.getOwnerName();
            this.ownerRole = entity.getOwnerRole();
            this.deadline = entity.getDeadline();
            this.priority = entity.getPriority();
            this.status = entity.getStatus();
            this.dependencies = entity.getDependencies() != null ? entity.getDependencies() : new ArrayList<>();
            this.sourceEvidence = entity.getSourceEvidence();
            this.sourceTimestamp = entity.getSourceTimestamp();
            this.confidence = entity.getConfidence();
            this.needsConfirmation = entity.isNeedsConfirmation();
            this.createdAt = entity.getCreatedAt();

            // Aliases
            this.task = entity.getTask();
            this.owner = entity.getOwner();
            this.evidence = entity.getEvidence();
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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title != null ? title : (task != null ? task : description);
    }

    public void setTitle(String title) {
        this.title = title;
        this.task = title;
    }

    public String getDescription() {
        return description != null ? description : (title != null ? title : task);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTask() {
        return getTitle();
    }

    public void setTask(String task) {
        setTitle(task);
    }

    public String getOwnerName() {
        return ownerName != null ? ownerName : (owner != null ? owner : "Unassigned");
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        this.owner = ownerName;
    }

    public String getOwner() {
        return getOwnerName();
    }

    public void setOwner(String owner) {
        setOwnerName(owner);
    }

    public String getOwnerRole() {
        return ownerRole != null ? ownerRole : "Not specified";
    }

    public void setOwnerRole(String ownerRole) {
        this.ownerRole = ownerRole;
    }

    public String getDeadline() {
        return deadline != null ? deadline : "Not specified";
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
        return status != null ? status : "Pending";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDependencies() {
        return dependencies != null ? dependencies : new ArrayList<>();
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
    }

    public String getSourceEvidence() {
        return sourceEvidence != null ? sourceEvidence : evidence;
    }

    public void setSourceEvidence(String sourceEvidence) {
        this.sourceEvidence = sourceEvidence;
        this.evidence = sourceEvidence;
    }

    public String getEvidence() {
        return getSourceEvidence();
    }

    public void setEvidence(String evidence) {
        setSourceEvidence(evidence);
    }

    public String getSourceTimestamp() {
        return sourceTimestamp != null ? sourceTimestamp : "Not specified";
    }

    public void setSourceTimestamp(String sourceTimestamp) {
        this.sourceTimestamp = sourceTimestamp;
    }

    public String getConfidence() {
        return confidence != null ? confidence : "High";
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public boolean isNeedsConfirmation() {
        return needsConfirmation;
    }

    public void setNeedsConfirmation(boolean needsConfirmation) {
        this.needsConfirmation = needsConfirmation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
