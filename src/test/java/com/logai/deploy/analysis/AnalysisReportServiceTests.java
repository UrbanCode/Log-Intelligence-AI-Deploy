package com.logai.deploy.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisReportServiceTests {

    private final AnalysisReportService service =
            new AnalysisReportService();

    @Test
    void buildsReportContainingSummaryAndAiAnalysis() {

        List<ErrorSummary> errors = List.of(
                new ErrorSummary(
                        "Connection refused",
                        "Connection refused sample",
                        "2026-08-11 10:00:00"
                ),
                new ErrorSummary(
                        "Database timeout",
                        "Database timeout sample",
                        "2026-08-11 10:05:00"
                ),
                new ErrorSummary(
                        "SSL handshake failed",
                        "SSL handshake failed sample",
                        "2026-08-11 10:10:00"
                )
        );

        String report = service.buildReport(
                10,
                errors,
                "Likely Root Cause: Database unavailable"
        );

        assertTrue(
                report.contains("Total Errors: 10")
        );

        assertTrue(
                report.contains("Unique Types: 3")
        );

        assertTrue(
                report.contains("Top Error: Connection refused")
        );

        assertTrue(
                report.contains("ERROR DETAILS")
        );

        assertTrue(
                report.contains("AI ANALYSIS")
        );

        assertTrue(
                report.contains(
                        "Likely Root Cause: Database unavailable"
                )
        );
    }

    @Test
    void reportContainsExpectedSections() {

        List<ErrorSummary> errors = List.of(
                new ErrorSummary(
                        "Test error",
                        "Test details",
                        "2026-08-11 10:00:00"
                )
        );

        String report = service.buildReport(
                1,
                errors,
                "Test AI analysis"
        );

        assertTrue(
                report.contains("===== ERROR DETAILS =====")
        );

        assertTrue(
                report.contains("===== AI ANALYSIS =====")
        );
    }
}