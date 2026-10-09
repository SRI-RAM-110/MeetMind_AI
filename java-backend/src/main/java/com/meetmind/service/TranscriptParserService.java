package com.meetmind.service;

import com.meetmind.dto.FieldProvenanceDto;
import com.meetmind.dto.TranscriptParseResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TranscriptParserService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptParserService.class);

    // WebVTT timestamp cue pattern: 00:01:23.456 --> 00:01:28.999
    private static final Pattern VTT_TIME_PATTERN = Pattern.compile(
            "(\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}\\s*-->\\s*(\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}"
    );

    // SRT timestamp cue pattern: 00:01:23,456 --> 00:01:28,999
    private static final Pattern SRT_TIME_PATTERN = Pattern.compile(
            "(\\d{2}:)?\\d{2}:\\d{2},\\d{3}\\s*-->\\s*(\\d{2}:)?\\d{2}:\\d{2},\\d{3}"
    );

    // WebVTT voice tag: <v Alice Smith>
    private static final Pattern VTT_VOICE_TAG = Pattern.compile("<v\\s+([^>]+)>");

    // Google Meet Header Patterns:
    private static final Pattern GM_TITLE_HEADER = Pattern.compile(
            "^(?:Meeting\\s*(?:name|title)?|Title)\\s*:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GM_DATE_HEADER = Pattern.compile(
            "^(?:Date|Meeting\\s*date)\\s*:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GM_ID_HEADER = Pattern.compile(
            "^(?:Meeting\\s*ID|Meet\\s*link|Google\\s*Meet)\\s*:\\s*(?:https?:\\/\\/)?(?:meet\\.google\\.com\\/)?([a-z0-9-]+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GM_ATTENDEES_HEADER = Pattern.compile(
            "^(?:Attendees|Participants|Invited)\\s*:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    // Google Meet speaker turn lines:
    // e.g. "Alice Smith (10:00 AM): Hello" or "[10:00 AM] Alice Smith: Hello" or "10:00 AM Alice Smith: Hello"
    private static final Pattern GM_SPEAKER_WITH_TIME = Pattern.compile(
            "^(?:(?:\\[?(\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:AM|PM)?)\\]?\\s+)?([A-Z][a-zA-Z0-9\\s.'-]{1,35}?)(?:\\s*\\((\\d{1,2}:\\d{2}(?:\\s*[APMapm]{2})?)\\))?):\\s*(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    // Standard Speaker: message
    private static final Pattern SPEAKER_COLON_PATTERN = Pattern.compile(
            "^([A-Z][a-zA-Z0-9\\s.'-]{1,35}):\\s*(.*)$"
    );

    // Dates in text e.g. October 10, 2026, Oct 9, 2026, or 2026-10-10
    private static final Pattern INLINE_DATE_PATTERN = Pattern.compile(
            "\\b(?:(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2})\\b",
            Pattern.CASE_INSENSITIVE
    );

    public TranscriptParseResponseDto parseFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return parseTranscript("", "");
        }
        try {
            String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return parseTranscript(content, file.getOriginalFilename());
        } catch (Exception e) {
            log.error("Failed to read uploaded transcript file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Could not read uploaded transcript file: " + e.getMessage());
        }
    }

    public TranscriptParseResponseDto parseText(String rawText) {
        return parseTranscript(rawText, "pasted_transcript.txt");
    }

    public TranscriptParseResponseDto parseTranscript(String rawContent, String filename) {
        TranscriptParseResponseDto dto = new TranscriptParseResponseDto();
        Map<String, FieldProvenanceDto> fields = new LinkedHashMap<>();

        if (rawContent == null || rawContent.isBlank()) {
            dto.setCleanedTranscript("");
            dto.setFormat("EMPTY");
            fields.put("title", FieldProvenanceDto.notAvailable("EMPTY_CONTENT"));
            fields.put("date", FieldProvenanceDto.notAvailable("EMPTY_CONTENT"));
            fields.put("participants", FieldProvenanceDto.notAvailable("EMPTY_CONTENT"));
            dto.setMetadataFields(fields);
            return dto;
        }

        String lowerContent = rawContent.toLowerCase();
        String format;
        if (lowerContent.contains("webvtt") || (filename != null && filename.toLowerCase().endsWith(".vtt"))) {
            format = "VTT";
        } else if (SRT_TIME_PATTERN.matcher(rawContent).find() || (filename != null && filename.toLowerCase().endsWith(".srt"))) {
            format = "SRT";
        } else if (isGoogleMeetTranscript(rawContent)) {
            format = "GOOGLE_MEET_TXT";
        } else {
            format = "RAW_TXT";
        }
        dto.setFormat(format);

        // Header extraction state
        String headerTitle = null;
        String headerDate = null;
        String headerMeetingId = null;
        List<String> headerParticipants = new ArrayList<>();

        Set<String> speakers = new LinkedHashSet<>();
        List<String> cleanedLines = new ArrayList<>();
        List<String> extractedTimestamps = new ArrayList<>();
        List<String> clockTimestamps = new ArrayList<>();

        String[] rawLines = rawContent.split("\\r?\\n");
        boolean inHeaderSection = true;

        for (int i = 0; i < rawLines.length; i++) {
            String line = rawLines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            // Skip WebVTT header and notes
            if (format.equals("VTT")) {
                if (line.equalsIgnoreCase("WEBVTT") || line.startsWith("NOTE ") || line.equals("NOTE")) {
                    continue;
                }
            }

            // Skip SRT numeric cue identifiers
            if (format.equals("SRT") && line.matches("^\\d+$")) {
                continue;
            }

            // Check if line is a timestamp line (VTT or SRT)
            Matcher vttMatcher = VTT_TIME_PATTERN.matcher(line);
            if (vttMatcher.find()) {
                extractedTimestamps.add(vttMatcher.group(0));
                inHeaderSection = false;
                continue;
            }
            Matcher srtMatcher = SRT_TIME_PATTERN.matcher(line);
            if (srtMatcher.find()) {
                extractedTimestamps.add(srtMatcher.group(0));
                inHeaderSection = false;
                continue;
            }

            // Check for explicit headers at top of transcript
            if (inHeaderSection) {
                Matcher mTitle = GM_TITLE_HEADER.matcher(line);
                if (mTitle.matches()) {
                    headerTitle = mTitle.group(1).trim();
                    continue;
                }

                Matcher mDate = GM_DATE_HEADER.matcher(line);
                if (mDate.matches()) {
                    headerDate = mDate.group(1).trim();
                    continue;
                }

                Matcher mId = GM_ID_HEADER.matcher(line);
                if (mId.matches()) {
                    headerMeetingId = mId.group(1).trim();
                    continue;
                }

                Matcher mAttendees = GM_ATTENDEES_HEADER.matcher(line);
                if (mAttendees.matches()) {
                    String[] attArr = mAttendees.group(1).split("[,;]");
                    for (String a : attArr) {
                        String clean = cleanAttendeeName(a.trim());
                        if (!clean.isEmpty()) {
                            headerParticipants.add(clean);
                        }
                    }
                    continue;
                }

                // If line looks like "Transcript" or "Google Meet Transcript", skip header label
                if (line.equalsIgnoreCase("Transcript") || line.equalsIgnoreCase("Google Meet Transcript")) {
                    continue;
                }
            }

            // Check for WebVTT <v Speaker> tag
            Matcher voiceMatcher = VTT_VOICE_TAG.matcher(line);
            if (voiceMatcher.find()) {
                String speaker = voiceMatcher.group(1).trim();
                if (isValidSpeakerName(speaker)) {
                    speakers.add(speaker);
                }
                line = line.replaceAll("</?v(?:\\s+[^>]*)?>", "").trim();
                inHeaderSection = false;
            }

            // Strip remaining HTML tags
            line = line.replaceAll("<[^>]+>", "").trim();
            if (line.isEmpty()) {
                continue;
            }

            // Check for Google Meet speaker pattern with time
            Matcher gmTimeMatcher = GM_SPEAKER_WITH_TIME.matcher(line);
            if (gmTimeMatcher.matches()) {
                String prefixTime = gmTimeMatcher.group(1);
                String spk = gmTimeMatcher.group(2).trim();
                String suffixTime = gmTimeMatcher.group(3);
                String text = gmTimeMatcher.group(4).trim();

                String cueTime = suffixTime != null ? suffixTime.trim() : (prefixTime != null ? prefixTime.trim() : null);
                if (cueTime != null) {
                    clockTimestamps.add(cueTime);
                }

                if (isValidSpeakerName(spk)) {
                    speakers.add(spk);
                    cleanedLines.add(spk + ": " + text);
                    inHeaderSection = false;
                    continue;
                }
            }

            // Check for standard Speaker: Text
            Matcher spkMatcher = SPEAKER_COLON_PATTERN.matcher(line);
            if (spkMatcher.matches()) {
                String spk = spkMatcher.group(1).trim();
                String text = spkMatcher.group(2).trim();
                if (isValidSpeakerName(spk)) {
                    speakers.add(spk);
                    cleanedLines.add(spk + ": " + text);
                    inHeaderSection = false;
                    continue;
                }
            }

            // Regular line
            cleanedLines.add(line);
            if (line.length() > 50) {
                inHeaderSection = false;
            }
        }

        String cleanedTranscript = String.join("\n", cleanedLines);
        dto.setCleanedTranscript(cleanedTranscript);
        dto.setDetectedSpeakers(new ArrayList<>(speakers));
        dto.setSpeakerCount(speakers.size());
        dto.setTimestampCount(extractedTimestamps.size() + clockTimestamps.size());
        dto.setTranscriptTimestamps(new ArrayList<>(clockTimestamps.isEmpty() ? extractedTimestamps : clockTimestamps));

        // 1. Meeting Title
        if (headerTitle != null && !headerTitle.isBlank()) {
            dto.setSuggestedTitle(headerTitle);
            fields.put("title", FieldProvenanceDto.verified(headerTitle, "SOURCE_METADATA", "HEADER_PARSER"));
        } else {
            String titleFromContent = suggestTitleFromContent(cleanedLines);
            if (titleFromContent != null) {
                dto.setSuggestedTitle(titleFromContent);
                fields.put("title", FieldProvenanceDto.inferred(titleFromContent, "TRANSCRIPT_TEXT", 0.75, "TRANSCRIPT_FIRST_LINE"));
            } else if (filename != null && !filename.isBlank() && !filename.startsWith("pasted_")) {
                String fileTitle = suggestTitleFromFilename(filename);
                dto.setSuggestedTitle(fileTitle);
                fields.put("title", FieldProvenanceDto.inferred(fileTitle, "FILENAME", 0.60, "FILENAME_HEURISTIC"));
            } else {
                dto.setSuggestedTitle(null);
                fields.put("title", FieldProvenanceDto.notAvailable("NO_TITLE_FOUND"));
            }
        }

        // 2. Meeting Date (DO NOT assume file date or today's date if not present)
        if (headerDate != null && !headerDate.isBlank()) {
            dto.setSuggestedDate(headerDate);
            fields.put("date", FieldProvenanceDto.verified(headerDate, "SOURCE_METADATA", "HEADER_PARSER"));
        } else {
            String explicitDate = extractExplicitDate(rawContent);
            if (explicitDate != null) {
                dto.setSuggestedDate(explicitDate);
                fields.put("date", FieldProvenanceDto.inferred(explicitDate, "TRANSCRIPT_TEXT", 0.80, "INLINE_REGEX"));
            } else {
                dto.setSuggestedDate(null);
                fields.put("date", FieldProvenanceDto.notAvailable("NO_DATE_FOUND"));
            }
        }

        // 3. Source Meeting ID (Google Meet)
        if (headerMeetingId != null && !headerMeetingId.isBlank()) {
            dto.setGoogleMeetingId(headerMeetingId);
            fields.put("googleMeetingId", FieldProvenanceDto.verified(headerMeetingId, "SOURCE_METADATA", "HEADER_PARSER"));
        } else {
            Matcher idMatch = Pattern.compile("meet\\.google\\.com\\/([a-z0-9-]+)").matcher(rawContent);
            if (idMatch.find()) {
                String foundId = idMatch.group(1);
                dto.setGoogleMeetingId(foundId);
                fields.put("googleMeetingId", FieldProvenanceDto.verified(foundId, "TRANSCRIPT_TEXT", "URL_EXTRACTOR"));
            } else {
                fields.put("googleMeetingId", FieldProvenanceDto.notAvailable("NOT_PRESENT"));
            }
        }

        // 7. Participants vs Speakers
        if (!headerParticipants.isEmpty()) {
            dto.setDetectedParticipants(headerParticipants);
            fields.put("participants", FieldProvenanceDto.verified(String.join(", ", headerParticipants), "SOURCE_METADATA", "ATTENDEES_HEADER"));
        } else if (!speakers.isEmpty()) {
            List<String> spkList = new ArrayList<>(speakers);
            dto.setDetectedParticipants(spkList);
            fields.put("participants", FieldProvenanceDto.inferred(String.join(", ", spkList), "TRANSCRIPT_TEXT", 0.85, "SPEAKER_CUES"));
        } else {
            fields.put("participants", FieldProvenanceDto.notAvailable("NO_PARTICIPANTS_FOUND"));
        }

        // Word count
        int wordCount = cleanedTranscript.isBlank() ? 0 : cleanedTranscript.split("\\s+").length;
        dto.setWordCount(wordCount);

        dto.setMetadataFields(fields);
        log.info("Parsed transcript '{}': format={}, speakers={}, timestamps={}, title='{}', date='{}'",
                filename, format, speakers.size(), dto.getTimestampCount(), dto.getSuggestedTitle(), dto.getSuggestedDate());

        return dto;
    }

    private boolean isGoogleMeetTranscript(String text) {
        String lower = text.toLowerCase();
        return lower.contains("meet.google.com")
                || lower.contains("google meet transcript")
                || lower.contains("meeting id:")
                || lower.contains("meeting name:")
                || GM_SPEAKER_WITH_TIME.matcher(text).find();
    }

    private boolean isValidSpeakerName(String candidate) {
        if (candidate == null || candidate.length() < 2 || candidate.length() > 35) {
            return false;
        }
        String lower = candidate.toLowerCase();
        return !lower.startsWith("http")
                && !lower.equals("note")
                && !lower.equals("webvtt")
                && !lower.equals("decision")
                && !lower.equals("action")
                && !lower.equals("topic")
                && !lower.equals("agenda")
                && !lower.equals("transcript");
    }

    private String cleanAttendeeName(String name) {
        if (name == null) return "";
        // Remove email in parentheses or brackets e.g. "Alice Smith (alice@example.com)"
        String cleaned = name.replaceAll("\\([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\)", "")
                .replaceAll("<[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}>", "")
                .trim();
        return cleaned.replaceAll("\\s*\\([^)]*\\)", "").trim();
    }

    private String suggestTitleFromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String clean = filename.replaceFirst("[.][^.]+$", "");
        clean = clean.replace("_", " ").replace("-", " ");
        if (clean.length() > 40) {
            clean = clean.substring(0, 40);
        }
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    private String suggestTitleFromContent(List<String> lines) {
        if (lines != null && !lines.isEmpty()) {
            String firstLine = lines.get(0).trim();
            if (firstLine.length() >= 5 && firstLine.length() <= 60 && !firstLine.contains(":")) {
                return firstLine;
            }
        }
        return null;
    }

    private String extractExplicitDate(String content) {
        Matcher matcher = INLINE_DATE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    private String extractTimeZone(String content) {
        Pattern tzPat = Pattern.compile("\\b(EST|EDT|PST|PDT|CST|CDT|MST|MDT|UTC|GMT|IST|BST|CET|CEST)\\b");
        Matcher m = tzPat.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

}
