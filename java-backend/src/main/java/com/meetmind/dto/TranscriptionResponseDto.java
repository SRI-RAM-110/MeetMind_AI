package com.meetmind.dto;

import java.util.ArrayList;
import java.util.List;

public class TranscriptionResponseDto {

    private String transcriptText;
    private double durationSeconds;
    private String language;
    private String sourceFileName;
    private List<SegmentDto> segments = new ArrayList<>();

    public TranscriptionResponseDto() {
    }

    public TranscriptionResponseDto(String transcriptText, double durationSeconds, String language, String sourceFileName) {
        this.transcriptText = transcriptText;
        this.durationSeconds = durationSeconds;
        this.language = language;
        this.sourceFileName = sourceFileName;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public List<SegmentDto> getSegments() {
        return segments;
    }

    public void setSegments(List<SegmentDto> segments) {
        this.segments = segments;
    }

    public static class SegmentDto {
        private double start;
        private double end;
        private String text;

        public SegmentDto() {
        }

        public SegmentDto(double start, double end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }

        public double getStart() {
            return start;
        }

        public void setStart(double start) {
            this.start = start;
        }

        public double getEnd() {
            return end;
        }

        public void setEnd(double end) {
            this.end = end;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
