package com.edupilot.service;

import com.edupilot.dto.AdminAnalyticsFilterCriteria;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
import com.edupilot.dto.AdminCohortSubjectAnalyticsDTO;
import com.edupilot.dto.AdminCohortSubjectAnalyticsDTO.SubjectResearchSummaryDTO;
import com.edupilot.dto.AdminStudentDirectoryDTO;
import com.edupilot.model.AssessmentResult;
import com.edupilot.model.QuizSession;
import com.edupilot.model.StudentProfile;
import com.edupilot.model.User;
import com.edupilot.repository.AssessmentResultRepository;
import com.edupilot.repository.QuizSessionRepository;
import com.edupilot.service.AdminAnalyticsService.FilteredStudentContext;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminAnalyticsExportService {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsExportService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    // =========================================================================
    // 1. EXCEL EXPORT (.XLSX)
    // =========================================================================

    public byte[] generateExcelExport(AdminAnalyticsFilterCriteria criteria) {
        if (criteria != null) {
            criteria.validate();
        }

        FilteredStudentContext ctx = adminAnalyticsService.resolveFilteredPopulation(criteria);
        AdminCohortAnalyticsDTO cohort = adminAnalyticsService.getCohortAnalytics(criteria);
        AdminCohortSubjectAnalyticsDTO subjectAnalytics = adminAnalyticsService.getCohortSubjectAnalytics(criteria);
        List<AdminStudentDirectoryDTO> studentDirectory = adminAnalyticsService.getStudentDirectory(criteria);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle regularStyle = createRegularStyle(workbook);

            // Sheet 1: Research Summary
            buildResearchSummarySheet(workbook, criteria, cohort, headerStyle, boldStyle, regularStyle);

            // Sheet 2: Student Analytics
            buildStudentAnalyticsSheet(workbook, criteria, ctx, studentDirectory, headerStyle, regularStyle);

            // Sheet 3: Subject Analytics
            buildSubjectAnalyticsSheet(workbook, subjectAnalytics, headerStyle, regularStyle);

            // Sheet 4: Assessment History
            buildAssessmentHistorySheet(workbook, criteria, ctx, headerStyle, regularStyle);

            // Sheet 5: Quiz Analytics
            buildQuizAnalyticsSheet(workbook, criteria, ctx, headerStyle, regularStyle);

            // Auto-size columns on all sheets
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                int maxCols = 0;
                for (Row r : sheet) {
                    if (r.getLastCellNum() > maxCols) {
                        maxCols = r.getLastCellNum();
                    }
                }
                for (int c = 0; c < maxCols; c++) {
                    sheet.autoSizeColumn(c);
                    int currentWidth = sheet.getColumnWidth(c);
                    if (currentWidth < 3500) {
                        sheet.setColumnWidth(c, 3500);
                    } else if (currentWidth > 15000) {
                        sheet.setColumnWidth(c, 15000);
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to generate Excel export", ex);
            throw new RuntimeException("Excel export generation failed: " + ex.getMessage(), ex);
        }
    }

    private void buildResearchSummarySheet(Workbook wb, AdminAnalyticsFilterCriteria criteria,
                                           AdminCohortAnalyticsDTO cohort, CellStyle headerStyle,
                                           CellStyle boldStyle, CellStyle regularStyle) {
        Sheet sheet = wb.createSheet("Research Summary");
        int rowNum = 0;

        // Title Block
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EduPilot AI - Research & Analytics Summary Report");
        titleCell.setCellStyle(boldStyle);

        Row genRow = sheet.createRow(rowNum++);
        genRow.createCell(0).setCellValue("Generated At:");
        genRow.createCell(1).setCellValue(LocalDateTime.now().format(DATE_TIME_FORMATTER));

        rowNum++; // Blank row

        // Filter Scope Section
        Row filterHeader = sheet.createRow(rowNum++);
        filterHeader.createCell(0).setCellValue("Research Filter Scope");
        filterHeader.getCell(0).setCellStyle(headerStyle);
        filterHeader.createCell(1).setCellValue("Selected Value");
        filterHeader.getCell(1).setCellStyle(headerStyle);

        addSummaryRow(sheet, rowNum++, "Branch", criteria != null && criteria.getBranch() != null ? criteria.getBranch() : "ALL", regularStyle);
        addSummaryRow(sheet, rowNum++, "Semester", criteria != null && criteria.getSemester() != null ? String.valueOf(criteria.getSemester()) : "ALL", regularStyle);
        addSummaryRow(sheet, rowNum++, "Subject Code", criteria != null && criteria.getSubjectCode() != null ? criteria.getSubjectCode() : "ALL", regularStyle);
        addSummaryRow(sheet, rowNum++, "Activity Status", criteria != null && criteria.getActivityStatus() != null ? criteria.getActivityStatus() : "ALL", regularStyle);
        addSummaryRow(sheet, rowNum++, "Authentic Baseline Filter", criteria != null && criteria.getHasAuthenticBaseline() != null ? (criteria.getHasAuthenticBaseline() ? "Yes (Required)" : "No (Non-baseline only)") : "ALL", regularStyle);
        addSummaryRow(sheet, rowNum++, "Observation Date Range", (criteria != null && criteria.getStartDate() != null ? criteria.getStartDate() : "All Past") + " to " + (criteria != null && criteria.getEndDate() != null ? criteria.getEndDate() : "Present"), regularStyle);

        rowNum++; // Blank row

        // Cohort Overview Section
        Row metricsHeader = sheet.createRow(rowNum++);
        metricsHeader.createCell(0).setCellValue("Cohort Metric");
        metricsHeader.getCell(0).setCellStyle(headerStyle);
        metricsHeader.createCell(1).setCellValue("Value");
        metricsHeader.getCell(1).setCellStyle(headerStyle);
        metricsHeader.createCell(2).setCellValue("Unit / Scale");
        metricsHeader.getCell(2).setCellStyle(headerStyle);
        metricsHeader.createCell(3).setCellValue("Interpretation Notes");
        metricsHeader.getCell(3).setCellStyle(headerStyle);

        if (cohort != null) {
            addMetricRow(sheet, rowNum++, "Total Enrolled Students", cohort.getTotalEnrolled(), "Students", "Filtered population total", regularStyle);
            addMetricRow(sheet, rowNum++, "Evaluated Cohort Size", cohort.getEvaluatedCohortSize(), "Students", "Students with authentic baseline diagnostic", regularStyle);
            addMetricRow(sheet, rowNum++, "Active Students", cohort.getActiveLast7Days(), "Students", "Activity within past 7 days", regularStyle);
            addMetricRow(sheet, rowNum++, "At-Risk Students", cohort.getAtRiskStudents(), "Students", "Activity between 8 and 14 days ago", regularStyle);
            addMetricRow(sheet, rowNum++, "Inactive Students", cohort.getInactiveStudents(), "Students", "No activity in >14 days or no recorded activity", regularStyle);

            boolean hasEvaluated = cohort.getEvaluatedCohortSize() > 0;
            addMetricRow(sheet, rowNum++, "Mean Baseline Knowledge (K0)", hasEvaluated ? cohort.getMeanBaselineKnowledge() + "%" : "N/A", "0 - 100%", "Earliest authentic diagnostic accuracy", regularStyle);
            addMetricRow(sheet, rowNum++, "Mean Current Knowledge (Kt)", hasEvaluated ? cohort.getMeanCurrentKnowledge() + "%" : "N/A", "0 - 100%", "Current persisted topic mastery", regularStyle);

            double meanDiff = hasEvaluated ? round2(cohort.getMeanCurrentKnowledge() - cohort.getMeanBaselineKnowledge()) : 0.0;
            String diffStr = hasEvaluated ? (meanDiff > 0 ? "+" : "") + meanDiff + " pp" : "N/A";
            addMetricRow(sheet, rowNum++, "Mean Knowledge Difference (Kt - K0)", diffStr, "Percentage Points", "Difference between cohort mean current knowledge and cohort mean baseline knowledge; baseline/current sample sizes may differ.", regularStyle);
            addMetricRow(sheet, rowNum++, "Mean Normalized Learning Gain (g)", hasEvaluated ? String.format("%.3f", cohort.getMeanNormalizedGain()) : "N/A", "0.0 - 1.0 (Hake)", "Standardized learning gain (Kt - K0)/(1 - K0)", regularStyle);

            if (cohort.getGrowthDistribution() != null) {
                addMetricRow(sheet, rowNum++, "Improved Students", cohort.getGrowthDistribution().getImprovedCount() + " (" + cohort.getGrowthDistribution().getImprovedPercentage() + "%)", "Students (%)", "Kt > K0", regularStyle);
                addMetricRow(sheet, rowNum++, "Unchanged Students", cohort.getGrowthDistribution().getUnchangedCount() + " (" + cohort.getGrowthDistribution().getUnchangedPercentage() + "%)", "Students (%)", "Kt == K0", regularStyle);
                addMetricRow(sheet, rowNum++, "Declined Students", cohort.getGrowthDistribution().getDeclinedCount() + " (" + cohort.getGrowthDistribution().getDeclinedPercentage() + "%)", "Students (%)", "Kt < K0", regularStyle);
            }

            if (cohort.getSatisfaction() != null) {
                boolean hasReviews = cohort.getSatisfaction().getTotalReviews() > 0;
                addMetricRow(sheet, rowNum++, "Average Satisfaction Rating", hasReviews ? cohort.getSatisfaction().getAverageRating() + " / 5.0" : "N/A", "1.0 - 5.0 Stars", "Student satisfaction feedback", regularStyle);
                addMetricRow(sheet, rowNum++, "Total Satisfaction Reviews", cohort.getSatisfaction().getTotalReviews(), "Reviews", "Number of submitted reviews", regularStyle);
            }

            rowNum++; // Blank row
            Row noteRow = sheet.createRow(rowNum++);
            noteRow.createCell(0).setCellValue("Data Sufficiency Note:");
            noteRow.getCell(0).setCellStyle(boldStyle);
            noteRow.createCell(1).setCellValue(cohort.getDataSufficiencyNote() != null ? cohort.getDataSufficiencyNote() : "Sufficient cohort data available.");

            Row disclaimerRow = sheet.createRow(rowNum++);
            disclaimerRow.createCell(0).setCellValue("Research Disclaimer:");
            disclaimerRow.getCell(0).setCellStyle(boldStyle);
            disclaimerRow.createCell(1).setCellValue("Observational & descriptive research data only. No causal claims, predictive inferences, or artificial 50% imputations.");
        }
    }

    private void addSummaryRow(Sheet sheet, int rowNum, String label, String value, CellStyle regularStyle) {
        Row row = sheet.createRow(rowNum);
        Cell c0 = row.createCell(0);
        c0.setCellValue(label);
        c0.setCellStyle(regularStyle);
        Cell c1 = row.createCell(1);
        c1.setCellValue(value);
        c1.setCellStyle(regularStyle);
    }

    private void addMetricRow(Sheet sheet, int rowNum, String metric, Object value, String unit, String notes, CellStyle regularStyle) {
        Row row = sheet.createRow(rowNum);
        Cell c0 = row.createCell(0);
        c0.setCellValue(metric);
        c0.setCellStyle(regularStyle);

        Cell c1 = row.createCell(1);
        if (value instanceof Number) {
            c1.setCellValue(((Number) value).doubleValue());
        } else {
            c1.setCellValue(String.valueOf(value));
        }
        c1.setCellStyle(regularStyle);

        Cell c2 = row.createCell(2);
        c2.setCellValue(unit);
        c2.setCellStyle(regularStyle);

        Cell c3 = row.createCell(3);
        c3.setCellValue(notes);
        c3.setCellStyle(regularStyle);
    }

    private void buildStudentAnalyticsSheet(Workbook wb, AdminAnalyticsFilterCriteria criteria,
                                            FilteredStudentContext ctx, List<AdminStudentDirectoryDTO> directory,
                                            CellStyle headerStyle, CellStyle regularStyle) {
        Sheet sheet = wb.createSheet("Student Analytics");
        int rowNum = 0;

        Row hRow = sheet.createRow(rowNum++);
        String[] headers = {
                "Student ID", "Full Name", "Email", "Branch", "Semester",
                "Authentic Baseline (K0 %)", "Current Knowledge (Kt %)", "Growth (pp)",
                "Normalized Gain (g)", "Growth Classification", "Activity Status",
                "Last Activity", "Assessments Count", "Quizzes Count"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        Map<String, Integer> studentAssessmentCounts = new HashMap<>();
        Map<String, LocalDateTime> studentLastActivityMap = new HashMap<>();
        List<AssessmentResult> allAssessments = assessmentResultRepository.findAll();
        for (AssessmentResult ar : allAssessments) {
            String studentId = resolveStudentId(ar.getUserId(), ar.getStudentProfileId(), ctx.validStudentIds, ctx.profileToUserMap);
            if (studentId != null) {
                if (ar.getCreatedAt() != null) {
                    studentLastActivityMap.merge(studentId, ar.getCreatedAt(), (d1, d2) -> d1.isAfter(d2) ? d1 : d2);
                }
                if (criteria != null && !criteria.isDateTimeInRange(ar.getCreatedAt())) continue;
                if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(ar.getSubjectCode(), ar.getSubjectName(), criteria.getSubjectCode())) continue;
                studentAssessmentCounts.merge(studentId, 1, Integer::sum);
            }
        }

        Map<String, Integer> studentQuizCounts = new HashMap<>();
        List<QuizSession> allQuizzes = quizSessionRepository.findAll();
        for (QuizSession qs : allQuizzes) {
            String studentId = resolveStudentId(qs.getUserId(), qs.getStudentProfileId(), ctx.validStudentIds, ctx.profileToUserMap);
            if (studentId != null && qs.getStatus() == QuizSession.Status.COMPLETED) {
                LocalDateTime qTime = qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : qs.getStartTime();
                if (qTime != null) {
                    studentLastActivityMap.merge(studentId, qTime, (d1, d2) -> d1.isAfter(d2) ? d1 : d2);
                }
                if (criteria != null && !criteria.isDateTimeInRange(qTime)) continue;
                if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(qs.getSubjectCode(), qs.getSubjectName(), criteria.getSubjectCode())) continue;
                studentQuizCounts.merge(studentId, 1, Integer::sum);
            }
        }

        for (AdminStudentDirectoryDTO studentDto : directory) {
            String studentId = studentDto.getUserId();
            String name = studentDto.getFullName() != null && !studentDto.getFullName().isBlank() ? studentDto.getFullName() : "N/A";
            String email = studentDto.getEmail() != null && !studentDto.getEmail().isBlank() ? studentDto.getEmail() : "N/A";
            String branch = studentDto.getBranch() != null && !studentDto.getBranch().isBlank() ? studentDto.getBranch() : "N/A";
            String semester = studentDto.getSemester() != null ? String.valueOf(studentDto.getSemester()) : "N/A";
            String activityStatus = studentDto.getActivityStatus() != null ? studentDto.getActivityStatus() : "NO_ACTIVITY";

            boolean hasBaseline = studentDto.isHasAuthenticBaseline();
            Double k0 = studentDto.getBaselineKnowledge();
            Double kt = studentDto.getCurrentKnowledge();
            Double growthPp = studentDto.getGrowthPp();
            Double normalizedGain = null;

            if (hasBaseline && k0 != null && kt != null) {
                normalizedGain = round2(LearningGainService.computeGain(k0 / 100.0, kt / 100.0));
            }

            String classification = "Insufficient Baseline";
            if (hasBaseline) {
                if (growthPp != null) {
                    if (growthPp > 0.0001) classification = "Improved";
                    else if (growthPp < -0.0001) classification = "Declined";
                    else classification = "Unchanged";
                } else {
                    classification = "Unchanged";
                }
            }

            LocalDateTime lastAct = studentLastActivityMap.get(studentId);

            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(studentId);
            row.createCell(c++).setCellValue(name);
            row.createCell(c++).setCellValue(email);
            row.createCell(c++).setCellValue(branch);
            row.createCell(c++).setCellValue(semester);

            if (k0 != null) {
                Cell k0Cell = row.createCell(c++);
                k0Cell.setCellValue(k0);
                k0Cell.setCellStyle(regularStyle);
            } else {
                row.createCell(c++).setCellValue("N/A");
            }

            if (kt != null) {
                Cell ktCell = row.createCell(c++);
                ktCell.setCellValue(kt);
                ktCell.setCellStyle(regularStyle);
            } else {
                row.createCell(c++).setCellValue("N/A");
            }

            if (growthPp != null) {
                Cell gCell = row.createCell(c++);
                gCell.setCellValue(growthPp);
                gCell.setCellStyle(regularStyle);
            } else {
                row.createCell(c++).setCellValue("N/A");
            }

            if (normalizedGain != null) {
                Cell ngCell = row.createCell(c++);
                ngCell.setCellValue(normalizedGain);
                ngCell.setCellStyle(regularStyle);
            } else {
                row.createCell(c++).setCellValue("N/A");
            }

            row.createCell(c++).setCellValue(classification);
            row.createCell(c++).setCellValue(activityStatus);
            row.createCell(c++).setCellValue(lastAct != null ? lastAct.format(DATE_TIME_FORMATTER) : "N/A");
            row.createCell(c++).setCellValue(studentAssessmentCounts.getOrDefault(studentId, 0));
            row.createCell(c++).setCellValue(studentQuizCounts.getOrDefault(studentId, 0));
        }
    }

    private void buildSubjectAnalyticsSheet(Workbook wb, AdminCohortSubjectAnalyticsDTO subjectAnalytics,
                                            CellStyle headerStyle, CellStyle regularStyle) {
        Sheet sheet = wb.createSheet("Subject Analytics");
        int rowNum = 0;

        Row hRow = sheet.createRow(rowNum++);
        String[] headers = {
                "Subject Code", "Subject Name", "Enrolled Students", "Observed Knowledge Sample",
                "Authentic Baseline Sample", "Mean Baseline (K0 %)", "Mean Current (Kt %)",
                "Mean Growth (pp)", "Mean Normalized Gain (g)", "Total Concepts", "Weak Concepts",
                "Quiz Sessions Count", "Quiz Questions Total", "Quiz Accuracy (%)",
                "Roadmap Students", "Roadmap Completion (%)"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        if (subjectAnalytics != null && subjectAnalytics.getSubjects() != null) {
            for (SubjectResearchSummaryDTO sub : subjectAnalytics.getSubjects()) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(sub.getSubjectCode() != null ? sub.getSubjectCode() : "N/A");
                row.createCell(c++).setCellValue(sub.getSubjectName() != null ? sub.getSubjectName() : "N/A");
                row.createCell(c++).setCellValue(sub.getStudentsRepresented());
                row.createCell(c++).setCellValue(sub.getStudentsWithObservedKnowledge());
                row.createCell(c++).setCellValue(sub.getStudentsWithAuthenticBaseline());

                row.createCell(c++).setCellValue(sub.getMeanBaselineKnowledge() != null ? String.valueOf(sub.getMeanBaselineKnowledge()) : "N/A");
                row.createCell(c++).setCellValue(sub.getMeanCurrentKnowledge() != null ? String.valueOf(sub.getMeanCurrentKnowledge()) : "N/A");
                row.createCell(c++).setCellValue(sub.getMeanGrowthPp() != null ? String.valueOf(sub.getMeanGrowthPp()) : "N/A");
                row.createCell(c++).setCellValue(sub.getMeanNormalizedGain() != null ? String.format("%.3f", sub.getMeanNormalizedGain()) : "N/A");

                row.createCell(c++).setCellValue(sub.getConceptCount());
                row.createCell(c++).setCellValue(sub.getWeakConceptCount());
                row.createCell(c++).setCellValue(sub.getQuizSessionsCount());
                row.createCell(c++).setCellValue(sub.getTotalQuizQuestions());
                row.createCell(c++).setCellValue(sub.getMeanQuizAccuracy() != null ? String.valueOf(sub.getMeanQuizAccuracy()) : "N/A");
                row.createCell(c++).setCellValue(sub.getStudentsWithRoadmap());
                row.createCell(c++).setCellValue(sub.getRoadmapCompletionPercentage() != null ? String.valueOf(sub.getRoadmapCompletionPercentage()) : "N/A");
            }
        }
    }

    private void buildAssessmentHistorySheet(Workbook wb, AdminAnalyticsFilterCriteria criteria,
                                             FilteredStudentContext ctx, CellStyle headerStyle,
                                             CellStyle regularStyle) {
        Sheet sheet = wb.createSheet("Assessment History");
        int rowNum = 0;

        Row hRow = sheet.createRow(rowNum++);
        String[] headers = {
                "Assessment ID", "Student User ID", "Student Name", "Subject Code", "Subject Name",
                "Score", "Total Marks", "Percentage (%)", "Accuracy (%)",
                "Mastery Level", "Total Questions", "Correct Answers", "Date/Time"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        List<AssessmentResult> allAssessments = assessmentResultRepository.findAll();
        for (AssessmentResult ar : allAssessments) {
            String studentId = resolveStudentId(ar.getUserId(), ar.getStudentProfileId(), ctx.validStudentIds, ctx.profileToUserMap);
            if (studentId == null) continue;
            if (criteria != null && !criteria.isDateTimeInRange(ar.getCreatedAt())) continue;
            if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(ar.getSubjectCode(), ar.getSubjectName(), criteria.getSubjectCode())) continue;

            User u = ctx.userMap.get(studentId);
            StudentProfile p = ctx.profileMap.get(studentId);
            String name = u != null && u.getFullName() != null ? u.getFullName() : (p != null && p.getFullName() != null ? p.getFullName() : "N/A");

            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(ar.getId() != null ? ar.getId() : "N/A");
            row.createCell(c++).setCellValue(studentId);
            row.createCell(c++).setCellValue(name);
            row.createCell(c++).setCellValue(ar.getSubjectCode() != null ? ar.getSubjectCode() : "N/A");
            row.createCell(c++).setCellValue(ar.getSubjectName() != null ? ar.getSubjectName() : "N/A");
            row.createCell(c++).setCellValue(ar.getScore());
            row.createCell(c++).setCellValue(ar.getTotalMarks());
            row.createCell(c++).setCellValue(ar.getPercentage());
            row.createCell(c++).setCellValue(ar.getAccuracy());
            row.createCell(c++).setCellValue(ar.getMasteryLevel() != null ? ar.getMasteryLevel() : "N/A");
            row.createCell(c++).setCellValue(ar.getTotalQuestions());
            row.createCell(c++).setCellValue(ar.getCorrectAnswers());
            row.createCell(c++).setCellValue(ar.getCreatedAt() != null ? ar.getCreatedAt().format(DATE_TIME_FORMATTER) : "N/A");
        }
    }

    private void buildQuizAnalyticsSheet(Workbook wb, AdminAnalyticsFilterCriteria criteria,
                                         FilteredStudentContext ctx, CellStyle headerStyle,
                                         CellStyle regularStyle) {
        Sheet sheet = wb.createSheet("Quiz Analytics");
        int rowNum = 0;

        Row hRow = sheet.createRow(rowNum++);
        String[] headers = {
                "Quiz Session ID", "Student User ID", "Student Name", "Subject Code", "Subject Name",
                "Module Type", "Target Concept", "Status", "Total Questions",
                "Correct Count", "Incorrect Count", "Accuracy (%)", "Completed At"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        List<QuizSession> allQuizzes = quizSessionRepository.findAll();
        for (QuizSession qs : allQuizzes) {
            String studentId = resolveStudentId(qs.getUserId(), qs.getStudentProfileId(), ctx.validStudentIds, ctx.profileToUserMap);
            if (studentId == null) continue;
            LocalDateTime qTime = qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : qs.getStartTime();
            if (criteria != null && !criteria.isDateTimeInRange(qTime)) continue;
            if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(qs.getSubjectCode(), qs.getSubjectName(), criteria.getSubjectCode())) continue;

            User u = ctx.userMap.get(studentId);
            StudentProfile p = ctx.profileMap.get(studentId);
            String name = u != null && u.getFullName() != null ? u.getFullName() : (p != null && p.getFullName() != null ? p.getFullName() : "N/A");

            double acc = qs.getTotalQuestions() > 0 ? round2((double) qs.getCorrectCount() * 100.0 / qs.getTotalQuestions()) : 0.0;

            Row row = sheet.createRow(rowNum++);
            int c = 0;
            row.createCell(c++).setCellValue(qs.getId() != null ? qs.getId() : "N/A");
            row.createCell(c++).setCellValue(studentId);
            row.createCell(c++).setCellValue(name);
            row.createCell(c++).setCellValue(qs.getSubjectCode() != null ? qs.getSubjectCode() : "N/A");
            row.createCell(c++).setCellValue(qs.getSubjectName() != null ? qs.getSubjectName() : "N/A");
            row.createCell(c++).setCellValue(qs.getModuleType() != null ? qs.getModuleType().name() : "N/A");
            row.createCell(c++).setCellValue(qs.getTargetConcept() != null ? qs.getTargetConcept() : "N/A");
            row.createCell(c++).setCellValue(qs.getStatus() != null ? qs.getStatus().name() : "N/A");
            row.createCell(c++).setCellValue(qs.getTotalQuestions());
            row.createCell(c++).setCellValue(qs.getCorrectCount());
            row.createCell(c++).setCellValue(qs.getIncorrectCount());
            row.createCell(c++).setCellValue(acc);
            row.createCell(c++).setCellValue(qTime != null ? qTime.format(DATE_TIME_FORMATTER) : "N/A");
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle createRegularStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    // =========================================================================
    // 2. PDF EXPORT (.PDF)
    // =========================================================================

    public byte[] generatePdfExport(AdminAnalyticsFilterCriteria criteria) {
        if (criteria != null) {
            criteria.validate();
        }

        FilteredStudentContext ctx = adminAnalyticsService.resolveFilteredPopulation(criteria);
        AdminCohortAnalyticsDTO cohort = adminAnalyticsService.getCohortAnalytics(criteria);
        AdminCohortSubjectAnalyticsDTO subjectAnalytics = adminAnalyticsService.getCohortSubjectAnalytics(criteria);
        List<AdminStudentDirectoryDTO> studentDirectory = adminAnalyticsService.getStudentDirectory(criteria);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(79, 70, 229));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59));
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(30, 41, 59));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(30, 41, 59));
            Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(100, 116, 139));

            // Header Banner
            Paragraph title = new Paragraph("EduPilot AI - Research & Analytics Report", titleFont);
            title.setSpacingAfter(2f);
            document.add(title);

            Paragraph subtitle = new Paragraph("Descriptive Cohort Learning Growth & Observational Research Metrics  |  Generated: " + LocalDateTime.now().format(DATE_TIME_FORMATTER), subTitleFont);
            subtitle.setSpacingAfter(12f);
            document.add(subtitle);

            // 1. Applied Research Filters Box
            Paragraph pFilters = new Paragraph("Applied Research Filters", sectionTitleFont);
            pFilters.setSpacingAfter(6f);
            document.add(pFilters);

            PdfPTable filterTable = new PdfPTable(2);
            filterTable.setWidthPercentage(100);
            filterTable.setWidths(new float[]{30f, 70f});

            addPdfKeyValueRow(filterTable, "Branch", criteria != null && criteria.getBranch() != null ? criteria.getBranch() : "ALL", boldFont, regularFont);
            addPdfKeyValueRow(filterTable, "Semester", criteria != null && criteria.getSemester() != null ? String.valueOf(criteria.getSemester()) : "ALL", boldFont, regularFont);
            addPdfKeyValueRow(filterTable, "Subject Code", criteria != null && criteria.getSubjectCode() != null ? criteria.getSubjectCode() : "ALL", boldFont, regularFont);
            addPdfKeyValueRow(filterTable, "Activity Status", criteria != null && criteria.getActivityStatus() != null ? criteria.getActivityStatus() : "ALL", boldFont, regularFont);
            addPdfKeyValueRow(filterTable, "Authentic Baseline Filter", criteria != null && criteria.getHasAuthenticBaseline() != null ? (criteria.getHasAuthenticBaseline() ? "Yes (Required)" : "No (Non-baseline only)") : "ALL", boldFont, regularFont);
            addPdfKeyValueRow(filterTable, "Observation Date Window", (criteria != null && criteria.getStartDate() != null ? criteria.getStartDate() : "All Past") + " to " + (criteria != null && criteria.getEndDate() != null ? criteria.getEndDate() : "Present"), boldFont, regularFont);
            filterTable.setSpacingAfter(14f);
            document.add(filterTable);

            // 2. Cohort Overview Summary Table
            Paragraph pCohort = new Paragraph("1. Cohort Research Summary", sectionTitleFont);
            pCohort.setSpacingAfter(6f);
            document.add(pCohort);

            PdfPTable cohortTable = new PdfPTable(4);
            cohortTable.setWidthPercentage(100);
            cohortTable.setWidths(new float[]{30f, 20f, 30f, 20f});

            addPdfHeaderCell(cohortTable, "Metric", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(cohortTable, "Value", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(cohortTable, "Metric", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(cohortTable, "Value", tableHeaderFont, new Color(79, 70, 229));

            if (cohort != null) {
                addPdfMetricPair(cohortTable, "Total Enrolled", String.valueOf(cohort.getTotalEnrolled()), "Evaluated Cohort (N)", String.valueOf(cohort.getEvaluatedCohortSize()), regularFont, boldFont);
                addPdfMetricPair(cohortTable, "Active (<=7d)", String.valueOf(cohort.getActiveLast7Days()), "At-Risk (8-14d)", String.valueOf(cohort.getAtRiskStudents()), regularFont, boldFont);

                boolean hasReviews = cohort.getSatisfaction() != null && cohort.getSatisfaction().getTotalReviews() > 0;
                String satStr = hasReviews ? cohort.getSatisfaction().getAverageRating() + " / 5.0" : "N/A";
                addPdfMetricPair(cohortTable, "Inactive (>14d)", String.valueOf(cohort.getInactiveStudents()), "Satisfaction Rating", satStr, regularFont, boldFont);

                boolean hasEvaluated = cohort.getEvaluatedCohortSize() > 0;
                String k0Str = hasEvaluated ? cohort.getMeanBaselineKnowledge() + "%" : "N/A";
                String ktStr = hasEvaluated ? cohort.getMeanCurrentKnowledge() + "%" : "N/A";
                addPdfMetricPair(cohortTable, "Mean Baseline (K0)", k0Str, "Mean Current (Kt)", ktStr, regularFont, boldFont);

                double meanDiff = hasEvaluated ? round2(cohort.getMeanCurrentKnowledge() - cohort.getMeanBaselineKnowledge()) : 0.0;
                String diffStr = hasEvaluated ? (meanDiff > 0 ? "+" : "") + meanDiff + " pp" : "N/A";
                String gStr = hasEvaluated ? String.format("%.3f", cohort.getMeanNormalizedGain()) : "N/A";
                addPdfMetricPair(cohortTable, "Mean Difference (Kt-K0)", diffStr, "Mean Norm. Gain (g)", gStr, regularFont, boldFont);

                if (cohort.getGrowthDistribution() != null) {
                    addPdfMetricPair(cohortTable, "Improved Count", cohort.getGrowthDistribution().getImprovedCount() + " (" + cohort.getGrowthDistribution().getImprovedPercentage() + "%)", "Unchanged Count", cohort.getGrowthDistribution().getUnchangedCount() + " (" + cohort.getGrowthDistribution().getUnchangedPercentage() + "%)", regularFont, boldFont);
                    addPdfMetricPair(cohortTable, "Declined Count", cohort.getGrowthDistribution().getDeclinedCount() + " (" + cohort.getGrowthDistribution().getDeclinedPercentage() + "%)", "Data Sufficiency", cohort.getDataSufficiencyNote() != null ? cohort.getDataSufficiencyNote() : "Normal", regularFont, boldFont);
                }
            }
            cohortTable.setSpacingAfter(14f);
            document.add(cohortTable);

            // 3. Subject-Level Analytics
            if (subjectAnalytics != null && subjectAnalytics.getSubjects() != null && !subjectAnalytics.getSubjects().isEmpty()) {
                Paragraph pSub = new Paragraph("2. Subject Research Analytics", sectionTitleFont);
                pSub.setSpacingAfter(6f);
                document.add(pSub);

                PdfPTable subTable = new PdfPTable(7);
                subTable.setWidthPercentage(100);
                subTable.setWidths(new float[]{15f, 27f, 10f, 12f, 12f, 12f, 12f});

                addPdfHeaderCell(subTable, "Code", tableHeaderFont, new Color(79, 70, 229));
                addPdfHeaderCell(subTable, "Subject Name", tableHeaderFont, new Color(79, 70, 229));
                addPdfHeaderCell(subTable, "Students", tableHeaderFont, new Color(79, 70, 229));
                addPdfHeaderCell(subTable, "Mean K0", tableHeaderFont, new Color(79, 70, 229));
                addPdfHeaderCell(subTable, "Mean Kt", tableHeaderFont, new Color(79, 70, 229));
                addPdfHeaderCell(subTable, "Gain (g)", tableHeaderFont, new Color(79, 70, 229));
                addPdfHeaderCell(subTable, "Quiz Acc.", tableHeaderFont, new Color(79, 70, 229));

                for (SubjectResearchSummaryDTO sub : subjectAnalytics.getSubjects()) {
                    subTable.addCell(new Phrase(sub.getSubjectCode() != null ? sub.getSubjectCode() : "N/A", regularFont));
                    subTable.addCell(new Phrase(sub.getSubjectName() != null ? sub.getSubjectName() : "N/A", regularFont));
                    subTable.addCell(new Phrase(String.valueOf(sub.getStudentsRepresented()), regularFont));
                    subTable.addCell(new Phrase(sub.getMeanBaselineKnowledge() != null ? sub.getMeanBaselineKnowledge() + "%" : "N/A", regularFont));
                    subTable.addCell(new Phrase(sub.getMeanCurrentKnowledge() != null ? sub.getMeanCurrentKnowledge() + "%" : "N/A", regularFont));
                    subTable.addCell(new Phrase(sub.getMeanNormalizedGain() != null ? String.format("%.3f", sub.getMeanNormalizedGain()) : "N/A", regularFont));
                    subTable.addCell(new Phrase(sub.getMeanQuizAccuracy() != null ? sub.getMeanQuizAccuracy() + "%" : "N/A", regularFont));
                }
                subTable.setSpacingAfter(14f);
                document.add(subTable);
            }

            // 4. Filtered Student Cohort Sample (Limit to 30 rows in PDF summary)
            Paragraph pStudents = new Paragraph("3. Student Cohort Sample (Filtered Population)", sectionTitleFont);
            pStudents.setSpacingAfter(6f);
            document.add(pStudents);

            PdfPTable studentTable = new PdfPTable(7);
            studentTable.setWidthPercentage(100);
            studentTable.setWidths(new float[]{24f, 14f, 10f, 13f, 13f, 13f, 13f});

            addPdfHeaderCell(studentTable, "Student Name", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(studentTable, "Branch", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(studentTable, "Sem", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(studentTable, "K0", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(studentTable, "Kt", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(studentTable, "Gain (g)", tableHeaderFont, new Color(79, 70, 229));
            addPdfHeaderCell(studentTable, "Status", tableHeaderFont, new Color(79, 70, 229));

            int studentCount = 0;
            for (AdminStudentDirectoryDTO studentDto : studentDirectory) {
                if (studentCount >= 30) {
                    break;
                }
                String name = studentDto.getFullName() != null && !studentDto.getFullName().isBlank() ? studentDto.getFullName() : "N/A";
                String branch = studentDto.getBranch() != null && !studentDto.getBranch().isBlank() ? studentDto.getBranch() : "N/A";
                String semester = studentDto.getSemester() != null ? String.valueOf(studentDto.getSemester()) : "N/A";
                String activityStatus = studentDto.getActivityStatus() != null ? studentDto.getActivityStatus() : "NO_ACTIVITY";

                boolean hasBaseline = studentDto.isHasAuthenticBaseline();
                Double k0 = studentDto.getBaselineKnowledge();
                Double kt = studentDto.getCurrentKnowledge();
                Double normalizedGain = null;

                if (hasBaseline && k0 != null && kt != null) {
                    normalizedGain = round2(LearningGainService.computeGain(k0 / 100.0, kt / 100.0));
                }

                studentTable.addCell(new Phrase(name, regularFont));
                studentTable.addCell(new Phrase(branch, regularFont));
                studentTable.addCell(new Phrase(semester, regularFont));
                studentTable.addCell(new Phrase(k0 != null ? k0 + "%" : "N/A", regularFont));
                studentTable.addCell(new Phrase(kt != null ? kt + "%" : "N/A", regularFont));
                studentTable.addCell(new Phrase(normalizedGain != null ? String.format("%.3f", normalizedGain) : "N/A", regularFont));
                studentTable.addCell(new Phrase(activityStatus, regularFont));
                studentCount++;
            }

            if (studentDirectory.size() > 30) {
                PdfPCell moreCell = new PdfPCell(new Phrase("... and " + (studentDirectory.size() - 30) + " more students (see complete Excel export for all records)", noteFont));
                moreCell.setColspan(7);
                moreCell.setPadding(4f);
                moreCell.setBackgroundColor(new Color(248, 250, 252));
                studentTable.addCell(moreCell);
            }
            studentTable.setSpacingAfter(14f);
            document.add(studentTable);

            // 5. Research Integrity & Methodology Statement
            Paragraph pMethod = new Paragraph("4. Research Methodology & Data Integrity Statement", sectionTitleFont);
            pMethod.setSpacingAfter(4f);
            document.add(pMethod);

            Paragraph pNotes = new Paragraph(
                    "- Baseline Knowledge (K0): Anchored exclusively to the student's earliest authentic diagnostic assessment. Never replaced by subsequent quizzes or defaulted to a synthetic 50% value.\n" +
                            "- Current Knowledge (Kt): Sourced from persisted concept mastery records across topics.\n" +
                            "- Mean Knowledge Difference (Kt - K0): Difference between cohort mean current knowledge and cohort mean baseline knowledge; baseline/current sample sizes may differ.\n" +
                            "- Learning Gain: Computed as Hake's normalized gain (Kt - K0)/(1 - K0) exclusively when both authentic K0 and valid Kt are available. Missing metrics remain uncalculated (N/A).\n" +
                            "- Observational Bounds: All reported statistics are strictly descriptive. No causal assertions, predictive inferences (e.g. CGPA/dropout prediction), or unsupported hypothesis claims are made.\n" +
                            "- Small Samples: Cohort subgroups with N < 5 should be interpreted cautiously.",
                    noteFont
            );
            document.add(pNotes);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            log.error("Failed to generate PDF export", ex);
            throw new RuntimeException("PDF export generation failed: " + ex.getMessage(), ex);
        }
    }

    private void addPdfHeaderCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addPdfKeyValueRow(PdfPTable table, String key, String value, Font boldFont, Font regularFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(key, boldFont));
        c1.setBackgroundColor(new Color(248, 250, 252));
        c1.setPadding(4f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, regularFont));
        c2.setPadding(4f);
        table.addCell(c2);
    }

    private void addPdfMetricPair(PdfPTable table, String k1, String v1, String k2, String v2, Font regFont, Font boldFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(k1, regFont));
        c1.setBackgroundColor(new Color(248, 250, 252));
        c1.setPadding(4f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(v1, boldFont));
        c2.setPadding(4f);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(k2, regFont));
        c3.setBackgroundColor(new Color(248, 250, 252));
        c3.setPadding(4f);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(v2, boldFont));
        c4.setPadding(4f);
        table.addCell(c4);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private String resolveStudentId(String userId, String profileId, Set<String> validStudentIds, Map<String, String> profileToUserMap) {
        if (userId != null && validStudentIds.contains(userId)) {
            return userId;
        }
        if (profileId != null && profileToUserMap.containsKey(profileId)) {
            String mapped = profileToUserMap.get(profileId);
            if (validStudentIds.contains(mapped)) {
                return mapped;
            }
        }
        if (userId != null && profileToUserMap.containsKey(userId)) {
            String mapped = profileToUserMap.get(userId);
            if (validStudentIds.contains(mapped)) {
                return mapped;
            }
        }
        return null;
    }

    private boolean matchesSubject(String recordSubjectCode, String recordSubjectName, String filterSubjectCode) {
        if (filterSubjectCode == null || filterSubjectCode.trim().isEmpty()) {
            return true;
        }
        String cleanFilter = filterSubjectCode.trim();
        // 1. Exact canonical subjectCode match, case-insensitive
        if (recordSubjectCode != null && recordSubjectCode.trim().equalsIgnoreCase(cleanFilter)) {
            return true;
        }
        // 2. Deterministic fallback: exact normalized name match only (NO substring/contains)
        if (recordSubjectName != null && isSubjectMatch(recordSubjectName, cleanFilter)) {
            return true;
        }
        return false;
    }

    private boolean isSubjectMatch(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        String clean1 = normalizeSubjectName(s1);
        String clean2 = normalizeSubjectName(s2);
        return clean1.equalsIgnoreCase(clean2);
    }

    private String normalizeSubjectName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
