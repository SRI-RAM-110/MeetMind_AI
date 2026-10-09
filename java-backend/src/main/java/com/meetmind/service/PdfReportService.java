package com.meetmind.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.meetmind.model.ActionItem;
import com.meetmind.model.FollowUpPlan;
import com.meetmind.model.Meeting;
import com.meetmind.model.ReviewFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfReportService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    // Color Palette
    private static final Color PRIMARY_NAVY = new Color(30, 58, 138);       // #1E3A8A
    private static final Color SECONDARY_BLUE = new Color(59, 130, 246);    // #3B82F6
    private static final Color TEXT_DARK = new Color(15, 23, 42);           // #0F172A
    private static final Color TEXT_MUTED = new Color(100, 116, 139);       // #64748B
    private static final Color BG_LIGHT = new Color(248, 250, 252);          // #F8FAFC
    private static final Color BORDER_GRAY = new Color(226, 232, 240);       // #E2E8F0
    private static final Color WARNING_AMBER = new Color(180, 83, 9);        // #B45309
    private static final Color SUCCESS_GREEN = new Color(22, 101, 52);       // #166534

    // Fonts
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY_NAVY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MUTED);
    private static final Font FONT_SECTION_H1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY_NAVY);
    private static final Font FONT_SECTION_H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, TEXT_DARK);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, TEXT_DARK);
    private static final Font FONT_BODY_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_MUTED);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
    private static final Font FONT_TABLE_BODY = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_DARK);
    private static final Font FONT_TABLE_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_DARK);
    private static final Font FONT_BADGE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE);

    public byte[] generateFollowUpPlanPdf(Meeting meeting,
                                          List<ActionItem> actionItems,
                                          FollowUpPlan followUpPlan,
                                          List<ReviewFinding> reviewFindings) {
        return generateMeetingReportPdf(meeting, actionItems, followUpPlan, reviewFindings);
    }

    public byte[] generateMeetingReportPdf(Meeting meeting,
                                          List<ActionItem> actionItems,
                                          FollowUpPlan followUpPlan,
                                          List<ReviewFinding> reviewFindings) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 46, 46);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            HeaderFooterPageEvent event = new HeaderFooterPageEvent(meeting.getMeetingTitle(), meeting.getMeetingDate());
            writer.setPageEvent(event);

            document.open();

            // Document Header Banner
            addReportHeader(document, meeting);

            // Section 1: Meeting Information
            addSection1MeetingInfo(document, meeting);

            // Section 2: Executive Summary
            addSection2Summary(document, meeting);

            // Section 3: Decisions Made
            addSection3Decisions(document, meeting);

            // Section 4: Detailed Action Plan (Wide Table with repeated headers)
            addSection4ActionPlan(document, actionItems);

            // Section 5: Person-Wise Responsibilities
            addSection5PersonWise(document, actionItems);

            // Section 6: Follow-Up Schedule
            addSection6FollowUpSchedule(document, followUpPlan, meeting);

            // Section 7: Pending Clarifications
            addSection7PendingClarifications(document, actionItems, followUpPlan, reviewFindings);

            document.close();
            log.info("Generated detailed follow-up plan PDF for meeting ID {}: {} bytes", meeting.getId(), baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate meeting report PDF for ID {}: {}", meeting.getId(), e.getMessage(), e);
            throw new RuntimeException("Could not generate meeting report PDF: " + e.getMessage(), e);
        }
    }

    private void addReportHeader(Document doc, Meeting meeting) throws DocumentException {
        Paragraph title = new Paragraph(meeting.getMeetingTitle(), FONT_TITLE);
        title.setSpacingAfter(2);
        doc.add(title);

        String genDate = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm z")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        Paragraph subtitle = new Paragraph("MeetMind AI — Comprehensive Business Meeting Intelligence & Follow-Up Plan • Generated: " + genDate, FONT_SUBTITLE);
        subtitle.setSpacingAfter(14);
        doc.add(subtitle);

        // Divider
        addDivider(doc);
    }

    private void addSection1MeetingInfo(Document doc, Meeting meeting) throws DocumentException {
        addSectionHeader(doc, "1. Meeting Information & Verified Metadata");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{22, 28, 22, 28});
        table.setSpacingAfter(12);

        String title = (meeting.getMeetingTitle() != null && !meeting.getMeetingTitle().isBlank())
                ? meeting.getMeetingTitle()
                : "Not specified (Requires confirmation)";
        addInfoCell(table, "Meeting Title:", title, true);

        String date = (meeting.getMeetingDate() != null && !meeting.getMeetingDate().isBlank())
                ? meeting.getMeetingDate()
                : "Not specified (Requires confirmation)";
        addInfoCell(table, "Meeting Date:", date, false);

        String start = meeting.getStartTime() != null ? meeting.getStartTime() : "Not specified";
        String end = meeting.getEndTime() != null ? meeting.getEndTime() : "Not specified";
        String timeSpan = (start.equals("Not specified") && end.equals("Not specified"))
                ? "Not specified"
                : start + " - " + end;
        addInfoCell(table, "Scheduled Time:", timeSpan, true);

        String tz = (meeting.getTimeZone() != null && !meeting.getTimeZone().isBlank())
                ? meeting.getTimeZone()
                : "Not specified";
        addInfoCell(table, "Time Zone:", tz, false);

        String duration = meeting.getDurationMinutes() != null
                ? meeting.getDurationMinutes() + " minutes"
                : "Not specified";
        addInfoCell(table, "Duration:", duration, true);

        String sourceType = meeting.getSourceType() != null ? meeting.getSourceType() : "PASTE";
        addInfoCell(table, "Source Type:", sourceType, false);

        if (meeting.getGoogleMeetingId() != null && !meeting.getGoogleMeetingId().isBlank()) {
            addInfoCell(table, "Meeting ID:", meeting.getGoogleMeetingId(), true);
            addInfoCell(table, "Source File:", meeting.getSourceFileName() != null ? meeting.getSourceFileName() : "N/A", false);
        }

        String participants = (meeting.getParticipants() != null && !meeting.getParticipants().isEmpty())
                ? String.join(", ", meeting.getParticipants())
                : "Not specified (Requires confirmation)";
        PdfPCell pCellLabel = createCell("Participants:", FONT_BODY_BOLD, BG_LIGHT, false);
        table.addCell(pCellLabel);
        PdfPCell pCellVal = createCell(participants, FONT_BODY, Color.WHITE, false);
        pCellVal.setColspan(3);
        table.addCell(pCellVal);

        if (meeting.getSpeakers() != null && !meeting.getSpeakers().isEmpty()) {
            PdfPCell sCellLabel = createCell("Speakers Detected:", FONT_BODY_BOLD, BG_LIGHT, false);
            table.addCell(sCellLabel);
            PdfPCell sCellVal = createCell(String.join(", ", meeting.getSpeakers()), FONT_BODY_MUTED, Color.WHITE, false);
            sCellVal.setColspan(3);
            table.addCell(sCellVal);
        }

        doc.add(table);
    }

    private void addSection2Summary(Document doc, Meeting meeting) throws DocumentException {
        addSectionHeader(doc, "2. Executive Summary");

        Paragraph p = new Paragraph(meeting.getSummary() != null ? meeting.getSummary() : "No summary provided.", FONT_BODY);
        p.setLeading(14);
        p.setSpacingAfter(12);
        doc.add(p);
    }

    private void addSection3Decisions(Document doc, Meeting meeting) throws DocumentException {
        addSectionHeader(doc, "3. Confirmed Decisions Made");

        List<String> decisions = meeting.getDecisions();
        if (decisions == null || decisions.isEmpty()) {
            Paragraph p = new Paragraph("No confirmed decisions recorded in this meeting.", FONT_BODY_MUTED);
            p.setSpacingAfter(12);
            doc.add(p);
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 92});
        table.setSpacingAfter(12);

        int idx = 1;
        for (String dec : decisions) {
            PdfPCell cNum = createCell("#" + idx++, FONT_BODY_BOLD, BG_LIGHT, true);
            cNum.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cNum);

            PdfPCell cDec = createCell(dec, FONT_BODY, Color.WHITE, true);
            table.addCell(cDec);
        }
        doc.add(table);
    }

    private void addSection4ActionPlan(Document doc, List<ActionItem> actionItems) throws DocumentException {
        addSectionHeader(doc, "4. Detailed Action Plan");

        if (actionItems == null || actionItems.isEmpty()) {
            Paragraph p = new Paragraph("No action items recorded for this meeting.", FONT_BODY_MUTED);
            p.setSpacingAfter(12);
            doc.add(p);
            return;
        }

        // 9-column comprehensive action items table with repeated header row
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new float[]{9, 18, 12, 12, 11, 8, 8, 11, 11});
        table.setSpacingAfter(14);

        // Header
        addTableHeaderCell(table, "ID");
        addTableHeaderCell(table, "Task / Deliverable");
        addTableHeaderCell(table, "Owner");
        addTableHeaderCell(table, "Role");
        addTableHeaderCell(table, "Deadline");
        addTableHeaderCell(table, "Priority");
        addTableHeaderCell(table, "Status");
        addTableHeaderCell(table, "Dependencies");
        addTableHeaderCell(table, "Evidence");

        int rowIdx = 0;
        for (ActionItem item : actionItems) {
            Color rowBg = (rowIdx % 2 == 0) ? Color.WHITE : BG_LIGHT;
            rowIdx++;

            String taskId = (item.getTaskId() != null && !item.getTaskId().isBlank()) ? item.getTaskId() : "TSK-" + rowIdx;
            table.addCell(createCell(taskId, FONT_TABLE_BODY_BOLD, rowBg, true));

            String title = item.getTitle() != null ? item.getTitle() : (item.getDescription() != null ? item.getDescription() : "Deliverable " + rowIdx);
            table.addCell(createCell(title, FONT_TABLE_BODY, rowBg, true));

            String owner = (item.getOwnerName() != null && !item.getOwnerName().isBlank()) ? item.getOwnerName() : "Unassigned";
            boolean isUnassigned = "Unassigned".equalsIgnoreCase(owner);
            Font ownerFont = isUnassigned ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, WARNING_AMBER) : FONT_TABLE_BODY_BOLD;
            table.addCell(createCell(owner, ownerFont, rowBg, true));

            String role = (item.getOwnerRole() != null && !item.getOwnerRole().isBlank()) ? item.getOwnerRole() : "Not specified";
            table.addCell(createCell(role, FONT_TABLE_BODY, rowBg, true));

            String deadline = (item.getDeadline() != null && !item.getDeadline().isBlank()) ? item.getDeadline() : "Not specified";
            boolean isNoDeadline = "Not specified".equalsIgnoreCase(deadline);
            Font deadlineFont = isNoDeadline ? FontFactory.getFont(FontFactory.HELVETICA, 8.5f, WARNING_AMBER) : FONT_TABLE_BODY;
            table.addCell(createCell(deadline, deadlineFont, rowBg, true));

            String priority = (item.getPriority() != null && !item.getPriority().isBlank()) ? item.getPriority() : "Medium";
            table.addCell(createCell(priority, FONT_TABLE_BODY, rowBg, true));

            String status = (item.getStatus() != null && !item.getStatus().isBlank()) ? item.getStatus() : "Pending";
            table.addCell(createCell(status, FONT_TABLE_BODY, rowBg, true));

            String deps = (item.getDependencies() != null && !item.getDependencies().isEmpty())
                    ? String.join(", ", item.getDependencies())
                    : "None";
            table.addCell(createCell(deps, FONT_TABLE_BODY, rowBg, true));

            String evidence = (item.getSourceEvidence() != null && !item.getSourceEvidence().isBlank()) ? item.getSourceEvidence() : "Not specified";
            table.addCell(createCell(evidence, FONT_BODY_MUTED, rowBg, true));
        }

        doc.add(table);
    }

    private void addSection5PersonWise(Document doc, List<ActionItem> actionItems) throws DocumentException {
        addSectionHeader(doc, "5. Person-Wise Responsibilities");

        if (actionItems == null || actionItems.isEmpty()) {
            Paragraph p = new Paragraph("No assigned owners recorded.", FONT_BODY_MUTED);
            p.setSpacingAfter(12);
            doc.add(p);
            return;
        }

        Map<String, List<ActionItem>> byPerson = actionItems.stream()
                .collect(Collectors.groupingBy(
                        item -> (item.getOwnerName() != null && !item.getOwnerName().isBlank()) ? item.getOwnerName() : "Unassigned",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<String, List<ActionItem>> entry : byPerson.entrySet()) {
            String person = entry.getKey();
            List<ActionItem> tasks = entry.getValue();

            boolean isUnassigned = "Unassigned".equalsIgnoreCase(person);
            Color headerBg = isUnassigned ? new Color(254, 243, 199) : new Color(239, 246, 255);
            Font nameFont = isUnassigned ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, WARNING_AMBER) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, PRIMARY_NAVY);

            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(100);
            card.setSpacingAfter(8);

            String roleInfo = tasks.stream()
                    .map(ActionItem::getOwnerRole)
                    .filter(r -> r != null && !r.equalsIgnoreCase("Not specified"))
                    .findFirst()
                    .orElse("Role: Individual Contributor");

            PdfPCell headerCell = createCell(person + " (" + roleInfo + ") — " + tasks.size() + " Deliverable(s)", nameFont, headerBg, true);
            headerCell.setPadding(6);
            card.addCell(headerCell);

            StringBuilder sb = new StringBuilder();
            for (ActionItem t : tasks) {
                String dLine = t.getDeadline() != null ? t.getDeadline() : "Not specified";
                String tName = t.getTitle() != null ? t.getTitle() : (t.getDescription() != null ? t.getDescription() : "Task");
                String prio = t.getPriority() != null ? t.getPriority() : "Medium";
                sb.append("• [").append(dLine).append("] ").append(tName)
                        .append(" (Priority: ").append(prio).append(")\n");
            }
            PdfPCell bodyCell = createCell(sb.toString().trim(), FONT_TABLE_BODY, Color.WHITE, true);
            bodyCell.setPadding(6);
            card.addCell(bodyCell);

            doc.add(card);
        }
        addDivider(doc);
    }

    private void addSection6FollowUpSchedule(Document doc, FollowUpPlan followUpPlan, Meeting meeting) throws DocumentException {
        addSectionHeader(doc, "6. Follow-Up Schedule & Execution Checkpoints");

        if (followUpPlan == null) {
            Paragraph p = new Paragraph("No follow-up plan generated.", FONT_BODY_MUTED);
            p.setSpacingAfter(12);
            doc.add(p);
            return;
        }

        // Overview
        if (followUpPlan.getOverview() != null && !followUpPlan.getOverview().isBlank()) {
            Paragraph ov = new Paragraph(followUpPlan.getOverview(), FONT_BODY);
            ov.setSpacingAfter(8);
            doc.add(ov);
        }

        // Next Meeting Date Badge
        String nextMeeting = followUpPlan.getNextMeetingDate();
        Paragraph nextP = new Paragraph();
        nextP.add(new Chunk("Scheduled Next Meeting: ", FONT_BODY_BOLD));
        nextP.add(new Chunk(nextMeeting != null ? nextMeeting : "Not specified", FONT_BODY));
        nextP.setSpacingAfter(10);
        doc.add(nextP);

        // Steps
        List<String> steps = followUpPlan.getSteps();
        if (steps != null && !steps.isEmpty()) {
            Paragraph stTitle = new Paragraph("Chronological Action Steps:", FONT_SECTION_H2);
            stTitle.setSpacingAfter(4);
            doc.add(stTitle);

            for (String step : steps) {
                Paragraph sp = new Paragraph("  → " + step, FONT_TABLE_BODY);
                sp.setSpacingAfter(3);
                doc.add(sp);
            }
        }

        // Checkpoints (AI Recommendations)
        List<String> checkpoints = followUpPlan.getCheckpoints();
        if (checkpoints != null && !checkpoints.isEmpty()) {
            Paragraph cpTitle = new Paragraph("AI-Recommended Checkpoints (For Confirmation):", FONT_SECTION_H2);
            cpTitle.setSpacingBefore(6);
            cpTitle.setSpacingAfter(4);
            doc.add(cpTitle);

            for (String cp : checkpoints) {
                Paragraph p = new Paragraph("  ◈ " + cp, FONT_TABLE_BODY);
                p.setSpacingAfter(3);
                doc.add(p);
            }
        }
        addDivider(doc);
    }

    private void addSection7PendingClarifications(Document doc,
                                                 List<ActionItem> actionItems,
                                                 FollowUpPlan followUpPlan,
                                                 List<ReviewFinding> reviewFindings) throws DocumentException {
        addSectionHeader(doc, "7. Pending Clarifications & Items Needing Confirmation");

        List<String> clarifications = new ArrayList<>();

        // 1. Unassigned tasks
        if (actionItems != null) {
            for (ActionItem item : actionItems) {
                if ("Unassigned".equalsIgnoreCase(item.getOwnerName())) {
                    clarifications.add("Assign Owner: Task '" + item.getTitle() + "' currently has no designated owner.");
                }
                if ("Not specified".equalsIgnoreCase(item.getDeadline())) {
                    clarifications.add("Set Deadline: Task '" + item.getTitle() + "' requires an explicit due date.");
                }
            }
        }

        // 2. Follow-up plan pending clarifications
        if (followUpPlan != null && followUpPlan.getPendingClarifications() != null) {
            clarifications.addAll(followUpPlan.getPendingClarifications());
        }

        // 3. Review Agent flags
        if (reviewFindings != null) {
            for (ReviewFinding f : reviewFindings) {
                if ("FLAG".equalsIgnoreCase(f.getSeverity()) || "WARNING".equalsIgnoreCase(f.getSeverity())) {
                    clarifications.add("Review Agent Flag [" + f.getCategory() + "]: " + f.getIssue());
                }
            }
        }

        if (clarifications.isEmpty()) {
            Paragraph p = new Paragraph("✓ All deliverables, owners, and dates have been verified. No pending clarifications.", FONT_BODY);
            p.setSpacingAfter(12);
            doc.add(p);
            return;
        }

        for (String c : clarifications) {
            Paragraph p = new Paragraph("  ⚠ " + c, FONT_TABLE_BODY);
            p.setSpacingAfter(4);
            doc.add(p);
        }
    }

    // Helper formatting methods
    private void addSectionHeader(Document doc, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, FONT_SECTION_H1);
        p.setSpacingBefore(8);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void addDivider(Document doc) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private void addInfoCell(PdfPTable table, String label, String value, boolean isEven) {
        PdfPCell cLabel = createCell(label, FONT_BODY_BOLD, BG_LIGHT, false);
        table.addCell(cLabel);
        PdfPCell cVal = createCell(value, FONT_BODY, Color.WHITE, false);
        table.addCell(cVal);
    }

    private void addTableHeaderCell(PdfPTable table, String headerText) {
        PdfPCell cell = new PdfPCell(new Phrase(headerText, FONT_TABLE_HEADER));
        cell.setBackgroundColor(PRIMARY_NAVY);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_GRAY);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, Color bgColor, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bgColor);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(4);
        if (!border) {
            cell.setBorder(Rectangle.NO_BORDER);
        } else {
            cell.setBorderColor(BORDER_GRAY);
        }
        return cell;
    }

    /**
     * Header & Footer Event Helper for Page Numbers and Running Headers
     */
    static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final String meetingTitle;
        private final String meetingDate;

        HeaderFooterPageEvent(String meetingTitle, String meetingDate) {
            this.meetingTitle = meetingTitle != null ? meetingTitle : "MeetMind AI Meeting";
            this.meetingDate = meetingDate != null ? meetingDate : "";
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            // Header (on pages 2+)
            if (writer.getPageNumber() > 1) {
                PdfPTable header = new PdfPTable(2);
                try {
                    header.setWidths(new float[]{70, 30});
                    header.setTotalWidth(document.right() - document.left());
                    PdfPCell titleCell = new PdfPCell(new Phrase(meetingTitle + " • " + meetingDate, FONT_BODY_MUTED));
                    titleCell.setBorder(Rectangle.BOTTOM);
                    titleCell.setBorderColor(BORDER_GRAY);
                    titleCell.setPaddingBottom(4);
                    header.addCell(titleCell);

                    PdfPCell badgeCell = new PdfPCell(new Phrase("MeetMind AI Follow-Up Plan", FONT_BODY_MUTED));
                    badgeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    badgeCell.setBorder(Rectangle.BOTTOM);
                    badgeCell.setBorderColor(BORDER_GRAY);
                    badgeCell.setPaddingBottom(4);
                    header.addCell(badgeCell);

                    header.writeSelectedRows(0, -1, document.left(), document.top() + 24, writer.getDirectContent());
                } catch (DocumentException ignored) {
                }
            }

            // Footer (all pages)
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidths(new float[]{70, 30});
                footer.setTotalWidth(document.right() - document.left());

                PdfPCell confCell = new PdfPCell(new Phrase("CONFIDENTIAL • MEETMIND AI BUSINESS MEETING ASSISTANT", FONT_BODY_MUTED));
                confCell.setBorder(Rectangle.TOP);
                confCell.setBorderColor(BORDER_GRAY);
                confCell.setPaddingTop(4);
                footer.addCell(confCell);

                PdfPCell pageCell = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), FONT_BODY_MUTED));
                pageCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                pageCell.setBorder(Rectangle.TOP);
                pageCell.setBorderColor(BORDER_GRAY);
                pageCell.setPaddingTop(4);
                footer.addCell(pageCell);

                footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());
            } catch (DocumentException ignored) {
            }
        }
    }
}
