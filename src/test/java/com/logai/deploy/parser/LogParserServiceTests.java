package com.logai.deploy.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logai.deploy.analysis.ErrorSummary;
import com.logai.deploy.config.MaskProperties;
import com.logai.deploy.util.MaskUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogParserServiceTests {

    private LogParserService parser;

    @BeforeEach
    void setUp() {

        MaskProperties maskProperties =
                new MaskProperties();

        MaskUtil maskUtil =
                new MaskUtil(maskProperties);

        ObjectMapper objectMapper =
                new ObjectMapper();

        parser =
                new LogParserService(
                        objectMapper,
                        maskUtil
                );
    }

    private BufferedReader reader(String content) {
        return new BufferedReader(
                new StringReader(content)
        );
    }

    @Test
    void extractErrorSummaryGroupsAndCountsErrors() throws IOException {

        String logs = String.join("\n",
                "{\"ts_utc\":\"2026-08-11T10:15:30Z\",\"level\":\"ERROR\",\"message\":\"Timeout Exception host=server1\"}",
                "{\"ts_utc\":\"2026-08-11T10:15:31Z\",\"level\":\"ERROR\",\"message\":\"Timeout Exception host=server1\"}",
                "2026-08-11 10:16:00 INFO healthy",
                "2026-08-11 10:16:30 ERROR user=john@example.com failed from 10.1.2.3"
        );

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(2, summary.size());

        int totalErrors =
                summary.values()
                        .stream()
                        .mapToInt(ErrorSummary::getCount)
                        .sum();

        assertEquals(3, totalErrors);

        assertTrue(
                summary.keySet()
                        .stream()
                        .anyMatch(signature ->
                                signature.contains("[REDACTED_HOST]")
                                        || signature.contains("[REDACTED_EMAIL]")
                                        || signature.contains("[REDACTED_IP]")
                        )
        );
    }

    @Test
    void ignoresInfoAndDebugLines() throws IOException {

        String logs = String.join("\n",
                "2026-08-11 10:00:00 INFO Application started",
                "2026-08-11 10:00:01 DEBUG Processing request",
                "2026-08-11 10:00:02 WARN An error occurred while retrying",
                "2026-08-11 10:00:03 INFO error-fix.jar loaded",
                "2026-08-11 10:00:04 ERROR Database connection failed"
        );

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(1, summary.size());

        int totalErrors =
                summary.values()
                        .stream()
                        .mapToInt(ErrorSummary::getCount)
                        .sum();

        assertEquals(1, totalErrors);
    }

    @Test
    void extractTimestampFromPlainTextIsCaptured() throws IOException {

        String logs =
                "2026-08-11 11:01:09 ERROR Payment exception";

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(1, summary.size());

        ErrorSummary info =
                summary.values()
                        .iterator()
                        .next();

        assertEquals(
                "2026-08-11 11:01:09",
                info.getFirstOccurrence()
        );
    }

    @Test
    void jsonErrorSignatureUsesMessageInsteadOfFullJsonPayload() throws IOException {

        String logs = String.join("\n",
                "{\"ts_utc\":\"2026-04-09T03:14:05.204Z\",\"lvl\":\"ERROR\",\"thr\":\"netty-pool-0\",\"log\":\"com.urbancode.test\",\"msg\":\"Connection refused localhost:7919\"}",
                "{\"ts_utc\":\"2026-04-09T03:14:06.204Z\",\"lvl\":\"ERROR\",\"thr\":\"netty-pool-0\",\"log\":\"com.urbancode.test\",\"msg\":\"Connection refused localhost:7919\"}"
        );

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(1, summary.size());

        ErrorSummary info =
                summary.values()
                        .iterator()
                        .next();

        assertEquals(2, info.getCount());

        String signature =
                summary.keySet()
                        .iterator()
                        .next();

        assertTrue(signature.contains("[ERROR]"));
        assertTrue(signature.contains("Connection refused"));
        assertFalse(signature.startsWith("{"));
    }

    @Test
    void groupsRepeatedJsonErrorsIntoSingleSignature() throws IOException {

        String logs = String.join("\n",
                "{\"timestamp\":\"2026-08-11T10:00:00Z\",\"level\":\"ERROR\",\"message\":\"Connection refused host=server1\"}",
                "{\"timestamp\":\"2026-08-11T10:00:01Z\",\"level\":\"ERROR\",\"message\":\"Connection refused host=server2\"}",
                "{\"timestamp\":\"2026-08-11T10:00:02Z\",\"level\":\"ERROR\",\"message\":\"Connection refused host=server3\"}"
        );

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(1, summary.size());

        ErrorSummary info =
                summary.values()
                        .iterator()
                        .next();

        assertEquals(3, info.getCount());
    }

    @Test
    void doesNotTreatWordsContainingErrorAsErrorLevel() throws IOException {

        String logs = String.join("\n",
                "2026-08-11 10:00:00 INFO error-handler started",
                "2026-08-11 10:00:01 INFO error occurred during startup",
                "2026-08-11 10:00:02 DEBUG exception-error processing",
                "2026-08-11 10:00:03 ERROR Actual failure"
        );

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(1, summary.size());

        ErrorSummary info =
                summary.values()
                        .iterator()
                        .next();

        assertEquals(1, info.getCount());
    }

    @Test
    void detectsFatalAsError() throws IOException {

        String logs = String.join("\n",
                "2026-08-11 10:00:00 FATAL Application crashed",
                "2026-08-11 10:00:01 INFO Application restarting"
        );

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(1, summary.size());

        ErrorSummary info =
                summary.values()
                        .iterator()
                        .next();

        assertEquals(1, info.getCount());
    }

    @Test
    void emptyInputProducesEmptySummary() throws IOException {

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(""));

        assertTrue(summary.isEmpty());
    }

    @Test
    void blankLinesAreIgnored() throws IOException {

        String logs = String.join("\n",
                "",
                "   ",
                "2026-08-11 10:00:00 ERROR Database failed",
                "",
                "   "
        );

        Map<String, ErrorSummary> summary =
                parser.extractErrorSummary(reader(logs));

        assertEquals(1, summary.size());

        ErrorSummary info =
                summary.values()
                        .iterator()
                        .next();

        assertEquals(1, info.getCount());
    }
}