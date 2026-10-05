package com.edupilot.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Objects;

public class AdminCohortComparisonRequest {

    private AdminAnalyticsFilterCriteria cohortA;
    private AdminAnalyticsFilterCriteria cohortB;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public AdminCohortComparisonRequest() {
    }

    public AdminCohortComparisonRequest(
            AdminAnalyticsFilterCriteria cohortA,
            AdminAnalyticsFilterCriteria cohortB,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.cohortA = cohortA;
        this.cohortB = cohortB;
        this.startDate = startDate;
        this.endDate = endDate;
        validate();
    }

    public void validate() {
        if (cohortA == null) {
            throw new IllegalArgumentException("cohortA criteria must be provided.");
        }
        if (cohortB == null) {
            throw new IllegalArgumentException("cohortB criteria must be provided.");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate (" + startDate + ") cannot be after endDate (" + endDate + ")");
        }

        // Apply shared observation window
        if (startDate != null || endDate != null) {
            cohortA.setStartDate(startDate);
            cohortA.setEndDate(endDate);
            cohortB.setStartDate(startDate);
            cohortB.setEndDate(endDate);
        } else {
            LocalDate sA = cohortA.getStartDate();
            LocalDate eA = cohortA.getEndDate();
            LocalDate sB = cohortB.getStartDate();
            LocalDate eB = cohortB.getEndDate();
            if ((sA != null || sB != null) && !Objects.equals(sA, sB)) {
                throw new IllegalArgumentException("Cohort A and Cohort B must share the same startDate (" + sA + " vs " + sB + ")");
            }
            if ((eA != null || eB != null) && !Objects.equals(eA, eB)) {
                throw new IllegalArgumentException("Cohort A and Cohort B must share the same endDate (" + eA + " vs " + eB + ")");
            }
            this.startDate = sA != null ? sA : sB;
            this.endDate = eA != null ? eA : eB;
        }

        cohortA.validate();
        cohortB.validate();
    }

    public AdminAnalyticsFilterCriteria getCohortA() {
        return cohortA;
    }

    public void setCohortA(AdminAnalyticsFilterCriteria cohortA) {
        this.cohortA = cohortA;
    }

    public AdminAnalyticsFilterCriteria getCohortB() {
        return cohortB;
    }

    public void setCohortB(AdminAnalyticsFilterCriteria cohortB) {
        this.cohortB = cohortB;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
