package com.meetmind.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.dto.*;
import com.meetmind.exception.MeetingNotFoundException;
import com.meetmind.model.ActionItem;
import com.meetmind.model.FollowUpPlan;
import com.meetmind.model.Meeting;
import com.meetmind.model.ReviewFinding;
import com.meetmind.repository.ActionItemRepository;
import com.meetmind.repository.FollowUpPlanRepository;
import com.meetmind.repository.MeetingRepository;
import com.meetmind.repository.ReviewFindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingRepository meetingRepository;
    private final ActionItemRepository actionItemRepository;
    private final FollowUpPlanRepository followUpPlanRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final AiAgentClientService aiAgentClientService;
    private final ObjectMapper objectMapper;
    private final PdfReportService pdfReportService;

    public MeetingService(MeetingRepository meetingRepository,
                          ActionItemRepository actionItemRepository,
                          FollowUpPlanRepository followUpPlanRepository,
                          ReviewFindingRepository reviewFindingRepository,
                          AiAgentClientService aiAgentClientService,
                          ObjectMapper objectMapper,
                          PdfReportService pdfReportService) {
        this.meetingRepository = meetingRepository;
        this.actionItemRepository = actionItemRepository;
        this.followUpPlanRepository = followUpPlanRepository;
        this.reviewFindingRepository = reviewFindingRepository;
        this.aiAgentClientService = aiAgentClientService;
        this.objectMapper = objectMapper;
        this.pdfReportService = pdfReportService;
    }

    /**
     * Analyzes meeting notes using the Flask AI agent and persists results in MongoDB.
     * Prevents duplicate persistence on rapid duplicate retries.
    /**
     * Overload for backward compatibility with existing tests/callers.
     */
    public MeetingResponseDto analyzeAndSaveMeeting(String meetingNotes) {
        AnalyzeRequest request = new AnalyzeRequest(meetingNotes);
        return analyzeAndSaveMeeting(request);
    }

    /**
     * Analyzes meeting notes using the Flask AI agent and persists results in MongoDB.
     * Captures and stores full meeting metadata (title, date, time, zone, participants, source).
     * Prevents duplicate persistence on rapid duplicate retries.
     */
    public MeetingResponseDto analyzeAndSaveMeeting(AnalyzeRequest request) {
        String trimmedNotes = (request.getMeetingNotes() != null) ? request.getMeetingNotes().trim() : "";
        if (trimmedNotes.length() < 10) {
            throw new IllegalArgumentException("Meeting notes must be at least 10 characters long.");
        }

        String notesHash = calculateSha256(trimmedNotes);

        // Check if identical meeting notes were already analyzed to prevent duplicate records on retry
        Optional<Meeting> existingOpt = meetingRepository.findByNotesHash(notesHash);
        if (existingOpt.isPresent()) {
            Meeting existing = existingOpt.get();
            log.info("Found existing meeting record with same notes hash: ID {}", existing.getId());
            return buildMeetingResponseDto(existing);
        }

        // Call the Python AI agent
        AiAgentResponse agentResponse = aiAgentClientService.analyzeMeeting(trimmedNotes);

        // 1. Save Meeting entity
        String nextMeetingDate = agentResponse.getNextMeetingDate();
        if ((nextMeetingDate == null || nextMeetingDate.isBlank()) && agentResponse.getFollowUpPlan() != null) {
            nextMeetingDate = agentResponse.getFollowUpPlan().getNextMeetingDate();
        }

        Meeting meeting = new Meeting(
                trimmedNotes,
                agentResponse.getSummary(),
                agentResponse.getDecisions(),
                "COMPLETED",
                nextMeetingDate,
                notesHash
        );

        // Populate meeting metadata from request
        if (request.getMeetingTitle() != null && !request.getMeetingTitle().isBlank()) {
            meeting.setMeetingTitle(request.getMeetingTitle());
        }
        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setStartTime(request.getStartTime());
        meeting.setEndTime(request.getEndTime());
        if (request.getTimeZone() != null && !request.getTimeZone().isBlank()) {
            meeting.setTimeZone(request.getTimeZone());
        }
        if (request.getSourceType() != null && !request.getSourceType().isBlank()) {
            meeting.setSourceType(request.getSourceType());
        }
        meeting.setSourceFileName(request.getSourceFileName());
        meeting.setGoogleMeetingId(request.getGoogleMeetingId());
        meeting.setDurationMinutes(request.getDurationMinutes());

        // Merge participants from request and transcript analysis
        List<String> combinedParticipants = new ArrayList<>(request.getParticipants());
        if (agentResponse.getTranscriptAnalysis() != null && agentResponse.getTranscriptAnalysis().getParticipants() != null) {
            for (String p : agentResponse.getTranscriptAnalysis().getParticipants()) {
                if (p != null && !p.isBlank() && !combinedParticipants.contains(p.trim())) {
                    combinedParticipants.add(p.trim());
                }
            }
        }
        meeting.setParticipants(combinedParticipants);
        meeting.setSpeakers(request.getSpeakers() != null ? request.getSpeakers() : new ArrayList<>());
        meeting.setMetadataProvenance(request.getMetadataProvenance() != null ? request.getMetadataProvenance() : new java.util.LinkedHashMap<>());

        meeting = meetingRepository.save(meeting);
        String meetingId = meeting.getId();
        log.info("Persisted Meeting record in MongoDB: ID {}", meetingId);

        // 2. Save ActionItem entities
        List<ActionItem> savedActionItems = new ArrayList<>();
        if (agentResponse.getActionItems() != null) {
            int idx = 1;
            for (AiAgentResponse.RawActionItem rawItem : agentResponse.getActionItems()) {
                String taskId = (rawItem.getTaskId() != null && !rawItem.getTaskId().isBlank())
                        ? rawItem.getTaskId()
                        : String.format("TASK-%02d", idx++);
                String title = (rawItem.getTitle() != null && !rawItem.getTitle().isBlank())
                        ? rawItem.getTitle()
                        : rawItem.getTask();
                String desc = (rawItem.getDescription() != null && !rawItem.getDescription().isBlank())
                        ? rawItem.getDescription()
                        : rawItem.getTask();
                String ownerRole = (rawItem.getOwnerRole() != null && !rawItem.getOwnerRole().isBlank())
                        ? rawItem.getOwnerRole()
                        : "Not specified";
                String confidenceStr = rawItem.getConfidence() != null
                        ? String.format("%.2f", rawItem.getConfidence())
                        : "High";

                ActionItem item = new ActionItem(
                        meetingId,
                        taskId,
                        title,
                        desc,
                        rawItem.getOwner(),
                        ownerRole,
                        rawItem.getDeadline(),
                        rawItem.getPriority(),
                        rawItem.getStatus(),
                        rawItem.getDependencies(),
                        rawItem.getEvidence(),
                        rawItem.getSourceTimestamp(),
                        confidenceStr,
                        rawItem.getNeedsConfirmation() != null && rawItem.getNeedsConfirmation()
                );
                savedActionItems.add(actionItemRepository.save(item));
            }
        }
        log.info("Persisted {} ActionItems for Meeting ID {}", savedActionItems.size(), meetingId);

        // 3. Save FollowUpPlan entity
        FollowUpPlan followUpPlan = null;
        if (agentResponse.getFollowUpPlan() != null) {
            AiAgentResponse.RawFollowUpPlan rawPlan = agentResponse.getFollowUpPlan();
            followUpPlan = new FollowUpPlan(
                    meetingId,
                    rawPlan.getOverview(),
                    rawPlan.getSteps(),
                    rawPlan.getCheckpoints(),
                    rawPlan.getUpcomingReviewMeetings(),
                    rawPlan.getPendingClarifications(),
                    nextMeetingDate
            );
            followUpPlan = followUpPlanRepository.save(followUpPlan);
            log.info("Persisted FollowUpPlan for Meeting ID {}", meetingId);
        }

        List<ActionItemDto> actionItemDtos = savedActionItems.stream()
                .map(ActionItemDto::new)
                .collect(Collectors.toList());
        FollowUpPlanDto followUpPlanDto = (followUpPlan != null) ? new FollowUpPlanDto(followUpPlan) : null;

        // 4. Save ReviewFinding entities
        List<ReviewFinding> savedReviewFindings = new ArrayList<>();
        if (agentResponse.getReviewFindings() != null) {
            for (AiAgentResponse.RawReviewFinding rawFinding : agentResponse.getReviewFindings()) {
                ReviewFinding finding = new ReviewFinding(
                        meetingId,
                        rawFinding.getCategory(),
                        rawFinding.getSeverity(),
                        rawFinding.getIssue(),
                        rawFinding.getEvidenceChecked(),
                        rawFinding.getActionTaken()
                );
                savedReviewFindings.add(reviewFindingRepository.save(finding));
            }
        }
        log.info("Persisted {} ReviewFindings for Meeting ID {}", savedReviewFindings.size(), meetingId);

        List<ReviewFindingDto> reviewFindingDtos = savedReviewFindings.stream()
                .map(ReviewFindingDto::new)
                .collect(Collectors.toList());

        TranscriptAnalysisDto transcriptAnalysisDto = null;
        if (agentResponse.getTranscriptAnalysis() != null) {
            transcriptAnalysisDto = new TranscriptAnalysisDto(
                    agentResponse.getTranscriptAnalysis().getTopics(),
                    agentResponse.getTranscriptAnalysis().getParticipants(),
                    agentResponse.getTranscriptAnalysis().getKeyDiscussionPoints()
            );
        }

        return new MeetingResponseDto(
                meeting,
                actionItemDtos,
                followUpPlanDto,
                reviewFindingDtos,
                transcriptAnalysisDto,
                agentResponse.getAgentExecution()
        );
    }

    /**
     * Retrieves meeting details by MongoDB ObjectId.
     */
    public MeetingResponseDto getMeetingById(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new MeetingNotFoundException("Meeting not found with ID: " + id));
        return buildMeetingResponseDto(meeting);
    }

    /**
     * Lists all meetings ordered by creation timestamp descending.
     */
    public List<MeetingSummaryDto> getAllMeetings() {
        List<Meeting> meetings = meetingRepository.findAllByOrderByCreatedAtDesc();
        return meetings.stream().map(m -> {
            List<ActionItem> items = actionItemRepository.findByMeetingId(m.getId());
            return new MeetingSummaryDto(m, items.size());
        }).collect(Collectors.toList());
    }

    /**
     * Generates CSV content for action items of a meeting.
     */
    public String generateActionItemsCsv(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException("Meeting not found with ID: " + meetingId));

        List<ActionItem> items = actionItemRepository.findByMeetingId(meeting.getId());

        StringBuilder csv = new StringBuilder();
        csv.append("Task,Owner,Deadline,Status,Evidence\n");

        for (ActionItem item : items) {
            csv.append(escapeCsv(item.getTask())).append(",")
                    .append(escapeCsv(item.getOwner())).append(",")
                    .append(escapeCsv(item.getDeadline())).append(",")
                    .append(escapeCsv(item.getStatus())).append(",")
                    .append(escapeCsv(item.getEvidence())).append("\n");
        }

        return csv.toString();
    }

    /**
     * Generates JSON content for the follow-up plan of a meeting.
     */
    public String generateFollowUpPlanJson(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException("Meeting not found with ID: " + meetingId));

        Optional<FollowUpPlan> planOpt = followUpPlanRepository.findByMeetingId(meeting.getId());
        if (planOpt.isEmpty()) {
            throw new MeetingNotFoundException("Follow-up plan not found for meeting ID: " + meetingId);
        }

        try {
            FollowUpPlanDto dto = new FollowUpPlanDto(planOpt.get());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
        } catch (Exception e) {
            log.error("Error serializing follow-up plan to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Could not serialize follow-up plan to JSON", e);
        }
    }

    /**
     * Generates a professional 7-section PDF report for the meeting follow-up plan.
     */
     public byte[] generateMeetingReportPdf(String meetingId) {
         Meeting meeting = meetingRepository.findById(meetingId)
                 .orElseThrow(() -> new MeetingNotFoundException("Meeting not found with ID: " + meetingId));

         List<ActionItem> actionItems = actionItemRepository.findByMeetingId(meeting.getId());
         Optional<FollowUpPlan> followUpPlanOpt = followUpPlanRepository.findByMeetingId(meeting.getId());
         List<ReviewFinding> reviewFindings = reviewFindingRepository.findByMeetingId(meeting.getId());

         return pdfReportService.generateFollowUpPlanPdf(
                 meeting,
                 actionItems,
                 followUpPlanOpt.orElse(null),
                 reviewFindings
         );
     }

    private MeetingResponseDto buildMeetingResponseDto(Meeting meeting) {
        List<ActionItem> actionItems = actionItemRepository.findByMeetingId(meeting.getId());
        List<ActionItemDto> actionItemDtos = actionItems.stream()
                .map(ActionItemDto::new)
                .collect(Collectors.toList());

        Optional<FollowUpPlan> planOpt = followUpPlanRepository.findByMeetingId(meeting.getId());
        FollowUpPlanDto planDto = planOpt.map(FollowUpPlanDto::new).orElse(null);

        List<ReviewFinding> findings = reviewFindingRepository.findByMeetingId(meeting.getId());
        List<ReviewFindingDto> findingDtos = findings.stream()
                .map(ReviewFindingDto::new)
                .collect(Collectors.toList());

        return new MeetingResponseDto(meeting, actionItemDtos, planDto, findingDtos, null, null);
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
