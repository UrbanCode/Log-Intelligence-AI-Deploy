package com.logai.deploy.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logai.deploy.analysis.ErrorSummary;
import com.logai.deploy.util.MaskUtil;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogParserService {

    private static final DateTimeFormatter OUTPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter LOCAL_WITH_MILLIS =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss.SSS"
            );

    private static final DateTimeFormatter LOCAL_WITHOUT_MILLIS =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private static final Pattern PLAINTEXT_TS =
            Pattern.compile(
                    "^(\\d{4}-\\d{2}-\\d{2}"
                            + "[ T]"
                            + "\\d{2}:\\d{2}:\\d{2}"
                            + "(?:[.,]\\d{3})?)"
            );

    private static final Pattern LEADING_TS =
            Pattern.compile(
                    "^\\d{4}-\\d{2}-\\d{2}"
                            + "[ T]"
                            + "\\d{2}:\\d{2}:\\d{2}"
                            + "(?:[.,]\\d{3})?\\s*"
            );

    private static final Pattern CHANNEL_ID_PATTERN =
            Pattern.compile(
                    "\\bch=[0-9a-fA-F]{6,}\\b"
            );

    private static final Pattern HEX_TOKEN_PATTERN =
            Pattern.compile(
                    "\\b[0-9a-fA-F]{8,}\\b"
            );

    private static final Pattern PORT_PATTERN =
            Pattern.compile(
                    "(?<=:)\\d{2,5}\\b"
            );

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(
                    "\\b\\d{4,}\\b"
            );

    private static final Pattern SPACE_PATTERN =
            Pattern.compile("\\s+");

    private static final Pattern BRACKET_LOG_LEVEL_PATTERN =
            Pattern.compile(
                    "\\[(ERROR|FATAL)]",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PLAIN_LOG_LEVEL_PATTERN =
            Pattern.compile(
                    "(?i)^(?:\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3})?\\s+)?"
                            + "(?:\\[[^\\]]+\\]\\s+)?"
                            + "(ERROR|FATAL)(?:\\s|$)"
            );

    private static final String[] JSON_LEVEL_FIELDS = {
            "lvl",
            "level",
            "severity",
            "loglevel",
            "logLevel"
    };

    private static final String[] JSON_TIMESTAMP_FIELDS = {
            "ts_loc",
            "ts_utc",
            "timestamp",
            "@timestamp",
            "time"
    };

    private static final String[] JSON_THREAD_FIELDS = {
            "thr",
            "thread"
    };

    private static final String[] JSON_LOGGER_FIELDS = {
            "log",
            "logger"
    };

    private static final String[] JSON_MESSAGE_FIELDS = {
            "msg",
            "message",
            "error",
            "exception"
    };

    private final ObjectMapper mapper;
    private final MaskUtil maskUtil;

    public LogParserService(
            ObjectMapper mapper,
            MaskUtil maskUtil) {

        this.mapper = mapper;
        this.maskUtil = maskUtil;
    }

    public Map<String, ErrorSummary> extractErrorSummary(
            BufferedReader reader) throws IOException {

        Map<String, ErrorSummary> summary =
                new LinkedHashMap<>();

        String line;

        while ((line = reader.readLine()) != null) {

            if (line.isBlank()) {
                continue;
            }

            JsonNode originalJson =
                    tryParseJson(line);

            if (!isErrorLogLevel(line, originalJson)) {
                continue;
            }

            /*
             * Sensitive information is masked before the
             * signature/sample is stored.
             */
            String maskedLine = maskUtil.mask(line);

            String timestamp =
                    extractTimestamp(line, originalJson);

            String signature =
                    buildSignature(maskedLine);

            summary.compute(
                    signature,
                    (key, existing) -> {

                        if (existing == null) {
                            return new ErrorSummary(
                                    signature,
                                    maskedLine,
                                    timestamp
                            );
                        }

                        existing.addOccurrence(timestamp);
                        return existing;
                    }
            );
        }

        return summary;
    }

    private boolean isErrorLogLevel(
            String line,
            JsonNode json) {

        if (json != null && json.isObject()) {

            String level =
                    readNodeText(
                            json,
                            JSON_LEVEL_FIELDS
                    );

            if (!level.isBlank()) {
                return isErrorLevel(level);
            }
        }

        Matcher bracketMatcher =
                BRACKET_LOG_LEVEL_PATTERN.matcher(line);

        if (bracketMatcher.find()) {
            return true;
        }

        Matcher plainLevelMatcher =
                PLAIN_LOG_LEVEL_PATTERN.matcher(line);

        return plainLevelMatcher.find();
    }

    private boolean isErrorLevel(String level) {

        return "ERROR".equalsIgnoreCase(level)
                || "FATAL".equalsIgnoreCase(level);
    }

    private String buildSignature(String maskedLine) {

        JsonNode node = tryParseJson(maskedLine);

        if (node == null || !node.isObject()) {
            return normalizeSignature(maskedLine);
        }

        String level =
                readNodeText(
                        node,
                        JSON_LEVEL_FIELDS
                );

        String thread =
                readNodeText(
                        node,
                        JSON_THREAD_FIELDS
                );

        String logger =
                readNodeText(
                        node,
                        JSON_LOGGER_FIELDS
                );

        String message =
                readNodeText(
                        node,
                        JSON_MESSAGE_FIELDS
                );

        if (message.isBlank()) {
            return normalizeSignature(maskedLine);
        }

        String normalizedMessage =
                normalizeSignature(message);

        StringBuilder signature =
                new StringBuilder();

        if (!level.isBlank()) {
            signature
                    .append("[")
                    .append(level)
                    .append("] ");
        }

        if (!thread.isBlank()) {
            signature
                    .append("[")
                    .append(thread)
                    .append("] ");
        }

        if (!logger.isBlank()) {
            signature
                    .append(logger)
                    .append(" - ");
        }

        signature.append(normalizedMessage);

        return signature.toString().trim();
    }

    private String readNodeText(
            JsonNode node,
            String... fieldNames) {

        for (String fieldName : fieldNames) {

            String value =
                    node.path(fieldName)
                            .asText("")
                            .trim();

            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private String normalizeSignature(String value) {

        String normalized =
                LEADING_TS
                        .matcher(value)
                        .replaceFirst("");

        normalized =
                CHANNEL_ID_PATTERN
                        .matcher(normalized)
                        .replaceAll("ch=[CHAN]");

        normalized =
                HEX_TOKEN_PATTERN
                        .matcher(normalized)
                        .replaceAll("[HEX]");

        normalized =
                PORT_PATTERN
                        .matcher(normalized)
                        .replaceAll("[PORT]");

        normalized =
                NUMBER_PATTERN
                        .matcher(normalized)
                        .replaceAll("[NUM]");

        normalized =
                SPACE_PATTERN
                        .matcher(normalized)
                        .replaceAll(" ")
                        .trim();

        return normalized;
    }

    private String extractTimestamp(
            String line,
            JsonNode json) {

        if (json != null && json.isObject()) {

            String timestamp =
                    extractTimestampFromJson(json);

            if (!"N/A".equals(timestamp)) {
                return timestamp;
            }
        }

        return extractTimestampFromPlainText(line);
    }

    private String extractTimestampFromJson(
            JsonNode node) {

        for (String field : JSON_TIMESTAMP_FIELDS) {

            if (!node.has(field)) {
                continue;
            }

            String value =
                    node.get(field).asText("");

            String parsed =
                    parseTimestamp(value);

            if (!"N/A".equals(parsed)) {
                return parsed;
            }
        }

        return "N/A";
    }

    private String extractTimestampFromPlainText(
            String line) {

        Matcher matcher =
                PLAINTEXT_TS.matcher(line);

        if (!matcher.find()) {
            return "N/A";
        }

        return parseTimestamp(
                matcher.group(1)
                        .replace(',', '.')
        );
    }

    private String parseTimestamp(String raw) {

        if (raw == null || raw.isBlank()) {
            return "N/A";
        }

        try {
            return ZonedDateTime
                    .parse(raw)
                    .format(OUTPUT_FORMAT);

        } catch (DateTimeParseException ignored) {
        }

        try {
            return Instant
                    .parse(raw)
                    .atZone(ZoneId.systemDefault())
                    .format(OUTPUT_FORMAT);

        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime
                    .parse(raw, LOCAL_WITH_MILLIS)
                    .format(OUTPUT_FORMAT);

        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime
                    .parse(raw, LOCAL_WITHOUT_MILLIS)
                    .format(OUTPUT_FORMAT);

        } catch (DateTimeParseException ignored) {
        }

        return "N/A";
    }

    private JsonNode tryParseJson(String line) {

        try {
            JsonNode node = mapper.readTree(line);

            if (node != null && node.isObject()) {
                return node;
            }

        } catch (Exception ignored) {
            // Not JSON.
        }

        return null;
    }
}