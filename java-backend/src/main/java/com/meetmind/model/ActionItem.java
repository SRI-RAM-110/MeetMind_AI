package com.meetmind.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "action_items")
public class ActionItem {

    @Id
    private String id;

    @Indexed
    private String meetingId;

    private String taskId;
    private String title;
    private String description;
    private String ownerName = "Unassigned";
    private String ownerRole = "Not specified";
    private String deadline = "Not specified";
    private String priority = "Medium"; // "High", "Medium", "Low"
    private String status = "Pending";   // "Pending", "In Progress", "Completed"
    private List<String> dependencies = new ArrayList<>();
    private String sourceEvidence;
    private String sourceTimestamp = "Not specified";
    private String confidence = "High";
    private boolean needsConfirmation = false;

    private Instant createdAt = Instant.now();

    public ActionItem() {
    }

    public ActionItem(String meetingId, String task, String owner, String deadline, String status, String evidence) {
        this.meetingId = meetingId;
        this.title = task;
        this.description = task;
        this.ownerName = (owner == null || owner.isBlank()) ? "Unassigned" : owner;
        this.deadline = (deadline == null || deadline.isBlank()) ? "Not specified" : deadline;
        this.status = (status == null || status.isBlank()) ? "Pending" : status;
        this.sourceEvidence = evidence;
        this.needsConfirmation = "Unassigned".equalsIgnoreCase(this.ownerName) || "Not specified".equalsIgnoreCase(this.deadline);
        this.createdAt = Instant.now();
    }

    public ActionItem(String meetingId, String taskId, String title, String description,
                      String ownerName, String ownerRole, String deadline, String priority,
                      String status, List<String> dependencies, String sourceEvidence,
                      String sourceTimestamp, String confidence, boolean needsConfirmation) {
        this.meetingId = meetingId;
        this.taskId = taskId;
        this.title = (title != null && !title.isBlank()) ? title : description;
        this.description = (description != null && !description.isBlank()) ? description : title;
        this.ownerName = (ownerName == null || ownerName.isBlank()) ? "Unassigned" : ownerName;
        this.ownerRole = (ownerRole == null || ownerRole.isBlank()) ? "Not specified" : ownerRole;
        this.deadline = (deadline == null || deadline.isBlank()) ? "Not specified" : deadline;
        this.priority = (priority == null || priority.isBlank()) ? "Medium" : priority;
        this.status = (status == null || status.isBlank()) ? "Pending" : status;
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        this.sourceEvidence = sourceEvidence;
        this.sourceTimestamp = (sourceTimestamp == null || sourceTimestamp.isBlank()) ? "Not specified" : sourceTimestamp;
        this.confidence = (confidence == null || confidence.isBlank()) ? "High" : confidence;
        this.needsConfirmation = needsConfirmation || "Unassigned".equalsIgnoreCase(this.ownerName) || "Not specified".equalsIgnoreCase(this.deadline);
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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title != null ? title : description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Backwards compatibility getter & setter
    public String getTask() {
        return getTitle();
    }

    public void setTask(String task) {
        this.title = task;
        this.description = task;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = (ownerName == null || ownerName.isBlank()) ? "Unassigned" : ownerName;
    }

    // Backwards compatibility getter & setter
    public String getOwner() {
        return getOwnerName();
    }

    public void setOwner(String owner) {
        setOwnerName(owner);
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
        this.deadline = (deadline == null || deadline.isBlank()) ? "Not specified" : deadline;
    }

    public String getPriority() {
        return priority;
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
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
    }

    public String getSourceEvidence() {
        return sourceEvidence;
    }

    public void setSourceEvidence(String sourceEvidence) {
        this.sourceEvidence = sourceEvidence;
    }

    // Backwards compatibility getter & setter
    public String getEvidence() {
        return getSourceEvidence();
    }

    public void setEvidence(String evidence) {
        setSourceEvidence(evidence);
    }

    public String getSourceTimestamp() {
        return sourceTimestamp;
    }

    public void setSourceTimestamp(String sourceTimestamp) {
        this.sourceTimestamp = sourceTimestamp;
    }

    public String getConfidence() {
        return confidence;
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
