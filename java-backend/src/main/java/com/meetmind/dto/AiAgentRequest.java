package com.meetmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiAgentRequest {

    @JsonProperty("meeting_notes")
    private String meetingNotes;

    public AiAgentRequest() {
    }

    public AiAgentRequest(String meetingNotes) {
        this.meetingNotes = meetingNotes;
    }

    public String getMeetingNotes() {
        return meetingNotes;
    }

    public void setMeetingNotes(String meetingNotes) {
        this.meetingNotes = meetingNotes;
    }
}
