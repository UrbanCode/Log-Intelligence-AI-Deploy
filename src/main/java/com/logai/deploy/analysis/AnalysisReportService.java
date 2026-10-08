package com.logai.deploy.analysis;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AnalysisReportService {

    public String buildReport(
            int totalErrors,
            List<ErrorSummary> sortedErrors,
            String aiResult) {

        StringBuilder report =
                new StringBuilder();

        String topError =
                sortedErrors.isEmpty()
                        ? "N/A"
                        : sortedErrors.get(0).getSignature();

        report.append("Total Errors: ")
                .append(totalErrors)
                .append(System.lineSeparator());

        report.append("Unique Types: ")
                .append(sortedErrors.size())
                .append(System.lineSeparator());

        report.append("Top Error: ")
                .append(topError)
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        report.append(
                        "===== ERROR DETAILS ====="
                )
                .append(System.lineSeparator());

        int index = 1;

        for (ErrorSummary error : sortedErrors) {

            report.append(index++)
                    .append(". ")
                    .append(error.getSignature())
                    .append(System.lineSeparator());

            report.append("   Occurrences: ")
                    .append(error.getCount())
                    .append(" | First: ")
                    .append(error.getFirstOccurrence())
                    .append(" | Last: ")
                    .append(error.getLastOccurrence())
                    .append(System.lineSeparator());

            report.append("   Example: ")
                    .append(error.getSampleLine())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }

        report.append(
                        "===== AI ANALYSIS ====="
                )
                .append(System.lineSeparator());

        report.append(aiResult)
                .append(System.lineSeparator());

        return report.toString();
    }
}