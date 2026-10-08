package com.logai.deploy.analysis;

import com.logai.deploy.ai.AiAnalysisService;
import com.logai.deploy.parser.LogParserService;
import com.logai.deploy.ai.OllamaModelStartupChecker;
import com.logai.deploy.cli.CliOptions;
import com.logai.deploy.config.MaskProperties;
import com.logai.deploy.util.Spinner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class LogAnalysisRunner {

    private static final int MAX_AI_ERRORS = 5;

    private final LogParserService parser;
    private final AiAnalysisService ai;
    private final OllamaModelStartupChecker modelChecker;
    private final MaskProperties maskProperties;
    private final AnalysisReportService reportService;

    public LogAnalysisRunner(
            LogParserService parser,
            AiAnalysisService ai,
            OllamaModelStartupChecker modelChecker,
            MaskProperties maskProperties,
            AnalysisReportService reportService) {

        this.parser = parser;
        this.ai = ai;
        this.modelChecker = modelChecker;
        this.maskProperties = maskProperties;
        this.reportService = reportService;
    }

    public void run(CliOptions options)
            throws Exception {

        validateInputLog(options.inputLog());

        applyMaskConfiguration(options);

        System.out.println(
                "Using AI config from application.yml "
                        + "(spring.ai.ollama.* and "
                        + "optional logai.ai.system-prompt)"
        );

        validateAiModel(options);

        String promptText =
                loadPrompt(options);

        System.out.println(
                "Reading log file: "
                        + options.inputLog()
        );

        Map<String, ErrorSummary> errorSummary;

        try (BufferedReader reader =
                     Files.newBufferedReader(
                             options.inputLog(),
                             StandardCharsets.UTF_8
                     );
             Spinner spinner =
                     new Spinner("Processing logs...")) {

            errorSummary =
                    parser.extractErrorSummary(reader);

            spinner.stop(
                    "Analysis complete."
            );
        }

        if (errorSummary.isEmpty()) {

            System.out.println(
                    "No errors found in the logs."
            );

            return;
        }

        List<ErrorSummary> sortedErrors =
                errorSummary.values()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        ErrorSummary::getCount
                                ).reversed()
                        )
                        .toList();

        int totalErrors =
                sortedErrors.stream()
                        .mapToInt(
                                ErrorSummary::getCount
                        )
                        .sum();

        ErrorSummary topError =
                sortedErrors.get(0);

        printSummary(
                totalErrors,
                sortedErrors.size(),
                topError
        );

        printErrorDetails(sortedErrors);

        List<ErrorSummary> topErrorsForAi =
                sortedErrors.stream()
                        .limit(MAX_AI_ERRORS)
                        .toList();

        String aiResult =
                ai.analyze(
                        topErrorsForAi,
                        promptText
                );

        System.out.println(
                "===== AI ANALYSIS (Top "
                        + MAX_AI_ERRORS
                        + " Errors) =====\n"
        );

        System.out.println(aiResult);

        Path reportPath =
                Path.of(
                        options.inputLog()
                                + ".analysis.txt"
                );

        String report =
                reportService.buildReport(
                        totalErrors,
                        sortedErrors,
                        aiResult
                );

        Files.writeString(
                reportPath,
                report,
                StandardCharsets.UTF_8
        );

        System.out.println(
                "Report saved to: "
                        + reportPath
        );
    }

    private void validateInputLog(Path inputLog) {

        if (!Files.exists(inputLog)) {
            throw new IllegalArgumentException(
                    "Input log file not found: "
                            + inputLog
            );
        }

        if (!Files.isRegularFile(inputLog)) {
            throw new IllegalArgumentException(
                    "Input log is not a regular file: "
                            + inputLog
            );
        }
    }

    private void applyMaskConfiguration(
            CliOptions options) throws Exception {

        if (!options.hasMaskConfig()) {

            System.out.println(
                    "No mask config provided. "
                            + "Using masking configuration "
                            + "from application.yml."
            );

            return;
        }

        Path maskConfig =
                options.maskConfig();

        if (!maskConfig.toString()
                .toLowerCase()
                .endsWith(".properties")) {

            throw new IllegalArgumentException(
                    "Invalid mask config. "
                            + "Expected a .properties file: "
                            + maskConfig
            );
        }

        if (!Files.exists(maskConfig)) {

            throw new IllegalArgumentException(
                    "Mask config file not found: "
                            + maskConfig
            );
        }

        maskProperties.applyOverrides(
                maskConfig.toString()
        );

        System.out.println(
                "Using mask config overrides from: "
                        + maskConfig
        );
    }

    private void validateAiModel(
            CliOptions options) {

        if (options.skipAiModelCheck()) {

            System.out.println(
                    "AI model check skipped by "
                            + "--skipAiModelCheck=true"
            );

            return;
        }

        modelChecker
                .validateConfiguredModel()
                .ifPresent(
                        warning ->
                                System.out.println(
                                        "AI model check: "
                                                + warning
                                )
                );
    }

    private String loadPrompt(
            CliOptions options) throws Exception {

        if (!options.hasAiPrompt()) {
            return null;
        }

        Path promptFile =
                options.aiPrompt();

        if (!Files.exists(promptFile)) {

            throw new IllegalArgumentException(
                    "AI prompt file not found: "
                            + promptFile
            );
        }

        String promptText =
                Files.readString(
                        promptFile,
                        StandardCharsets.UTF_8
                );

        System.out.println(
                "Using AI prompt file: "
                        + promptFile
        );

        return promptText;
    }

    private void printSummary(
            int totalErrors,
            int uniqueTypes,
            ErrorSummary topError) {

        System.out.println(
                "\n===== LOG SUMMARY ====="
        );

        System.out.println(
                "Total Errors: "
                        + totalErrors
                        + " | Unique Types: "
                        + uniqueTypes
                        + " | Top Error: "
                        + topError.getSignature()
        );
    }

    private void printErrorDetails(
            List<ErrorSummary> errors) {

        System.out.println(
                "\n===== ERROR DETAILS ====="
        );

        int index = 1;

        for (ErrorSummary error : errors) {

            System.out.println(
                    index
                            + ". "
                            + error.getSignature()
            );

            System.out.println(
                    "   Occurrences: "
                            + error.getCount()
                            + " | First: "
                            + error.getFirstOccurrence()
                            + " | Last: "
                            + error.getLastOccurrence()
            );

            System.out.println(
                    "   Example: "
                            + error.getSampleLine()
            );

            System.out.println();

            index++;
        }
    }
}