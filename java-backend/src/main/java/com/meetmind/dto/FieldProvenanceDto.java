package com.meetmind.dto;

public class FieldProvenanceDto {

    private String value;
    private String status; // "VERIFIED", "INFERRED", "NOT_AVAILABLE", "MANUAL_CORRECTION"
    private String source; // "SOURCE_METADATA", "TRANSCRIPT_TEXT", "FILENAME", "AUDIO_METADATA", "USER_INPUT", "NOT_AVAILABLE"
    private Double confidence; // 0.0 to 1.0
    private String method; // "GOOGLE_MEET_HEADER", "SPEAKER_CUES", "INLINE_REGEX", "FILENAME_HEURISTIC", etc.

    public FieldProvenanceDto() {
    }

    public FieldProvenanceDto(String value, String status, String source, Double confidence, String method) {
        this.value = value;
        this.status = status;
        this.source = source;
        this.confidence = confidence;
        this.method = method;
    }

    public static FieldProvenanceDto verified(String value, String source, String method) {
        return new FieldProvenanceDto(value, "VERIFIED", source, 1.0, method);
    }

    public static FieldProvenanceDto inferred(String value, String source, double confidence, String method) {
        return new FieldProvenanceDto(value, "INFERRED", source, confidence, method);
    }

    public static FieldProvenanceDto notAvailable(String method) {
        return new FieldProvenanceDto(null, "NOT_AVAILABLE", "NOT_AVAILABLE", 0.0, method);
    }

    public static FieldProvenanceDto manual(String value) {
        return new FieldProvenanceDto(value, "MANUAL_CORRECTION", "USER_INPUT", 1.0, "USER_FORM_INPUT");
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
