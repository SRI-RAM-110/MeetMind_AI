package com.meetmind.dto;

import java.util.ArrayList;
import java.util.List;

public class TranscriptAnalysisDto {

    private List<String> topics = new ArrayList<>();
    private List<String> participants = new ArrayList<>();
    private List<String> keyDiscussionPoints = new ArrayList<>();

    public TranscriptAnalysisDto() {
    }

    public TranscriptAnalysisDto(List<String> topics, List<String> participants, List<String> keyDiscussionPoints) {
        this.topics = topics != null ? topics : new ArrayList<>();
        this.participants = participants != null ? participants : new ArrayList<>();
        this.keyDiscussionPoints = keyDiscussionPoints != null ? keyDiscussionPoints : new ArrayList<>();
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public List<String> getKeyDiscussionPoints() {
        return keyDiscussionPoints;
    }

    public void setKeyDiscussionPoints(List<String> keyDiscussionPoints) {
        this.keyDiscussionPoints = keyDiscussionPoints;
    }
}
