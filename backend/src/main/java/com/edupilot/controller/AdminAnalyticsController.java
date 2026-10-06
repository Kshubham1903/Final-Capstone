package com.edupilot.controller;

import com.edupilot.dto.AdminAnalyticsFilterCriteria;
import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
import com.edupilot.dto.AdminCohortComparisonDTO;
import com.edupilot.dto.AdminCohortComparisonRequest;
import com.edupilot.dto.AdminCohortSubjectAnalyticsDTO;
import com.edupilot.dto.AdminResearchTrendsDTO;
import com.edupilot.dto.AdminStudentAnalyticsDTO;
import com.edupilot.dto.AdminStudentDirectoryDTO;
import com.edupilot.service.AdminAnalyticsExportService;
import com.edupilot.service.AdminAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Autowired
    private AdminAnalyticsExportService adminAnalyticsExportService;

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute AdminAnalyticsFilterCriteria criteria) {
        if (criteria == null) {
            criteria = new AdminAnalyticsFilterCriteria();
        }
        criteria.validate();
        byte[] excelBytes = adminAnalyticsExportService.generateExcelExport(criteria);
        String filename = "edupilot-research-analytics-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute AdminAnalyticsFilterCriteria criteria) {
        if (criteria == null) {
            criteria = new AdminAnalyticsFilterCriteria();
        }
        criteria.validate();
        byte[] pdfBytes = adminAnalyticsExportService.generatePdfExport(criteria);
        String filename = "edupilot-research-analytics-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/compare")
    public ResponseEntity<AdminCohortComparisonDTO> compareCohorts(@RequestBody AdminCohortComparisonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Comparison request body is required.");
        }
        request.validate();
        AdminCohortComparisonDTO comparison = adminAnalyticsService.compareCohorts(request);
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminAnalyticsOverviewDTO> getOverview(@ModelAttribute AdminAnalyticsFilterCriteria criteria) {
        if (criteria == null) {
            criteria = new AdminAnalyticsFilterCriteria();
        }
        criteria.validate();
        AdminAnalyticsOverviewDTO overview = adminAnalyticsService.getAnalyticsOverview(criteria);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/cohort")
    public ResponseEntity<AdminCohortAnalyticsDTO> getCohortAnalytics(@ModelAttribute AdminAnalyticsFilterCriteria criteria) {
        if (criteria == null) {
            criteria = new AdminAnalyticsFilterCriteria();
        }
        criteria.validate();
        AdminCohortAnalyticsDTO cohort = adminAnalyticsService.getCohortAnalytics(criteria);
        return ResponseEntity.ok(cohort);
    }

    @GetMapping("/trends")
    public ResponseEntity<AdminResearchTrendsDTO> getResearchTrends(@ModelAttribute AdminAnalyticsFilterCriteria criteria) {
        if (criteria == null) {
            criteria = new AdminAnalyticsFilterCriteria();
        }
        criteria.validate();
        AdminResearchTrendsDTO trends = adminAnalyticsService.getResearchTrends(criteria);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/subjects")
    public ResponseEntity<AdminCohortSubjectAnalyticsDTO> getCohortSubjectAnalytics(@ModelAttribute AdminAnalyticsFilterCriteria criteria) {
        if (criteria == null) {
            criteria = new AdminAnalyticsFilterCriteria();
        }
        criteria.validate();
        AdminCohortSubjectAnalyticsDTO subjects = adminAnalyticsService.getCohortSubjectAnalytics(criteria);
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/students")
    public ResponseEntity<List<AdminStudentDirectoryDTO>> getStudentDirectory(@ModelAttribute AdminAnalyticsFilterCriteria criteria) {
        if (criteria == null) {
            criteria = new AdminAnalyticsFilterCriteria();
        }
        criteria.validate();
        List<AdminStudentDirectoryDTO> directory = adminAnalyticsService.getStudentDirectory(criteria);
        return ResponseEntity.ok(directory);
    }

    @GetMapping("/students/{userId}")
    public ResponseEntity<?> getIndividualStudentAnalytics(@PathVariable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId parameter is required and cannot be blank"));
        }
        try {
            AdminStudentAnalyticsDTO analytics = adminAnalyticsService.getIndividualStudentAnalytics(userId.trim());
            return ResponseEntity.ok(analytics);
        } catch (IllegalArgumentException ex) {
            HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", ex.getMessage()));
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Invalid argument"));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBindException(BindException ex) {
        String errorMsg = "Invalid query parameter format";
        if (ex.getFieldError() != null) {
            errorMsg = "Invalid value for parameter '" + ex.getFieldError().getField() + "': " + ex.getFieldError().getRejectedValue();
        } else if (ex.getGlobalError() != null) {
            errorMsg = ex.getGlobalError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Map.of("message", errorMsg));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName() != null ? ex.getName() : "parameter";
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid value for parameter '" + paramName + "': " + ex.getValue()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeParseException(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format: " + ex.getParsedString()));
    }
}
