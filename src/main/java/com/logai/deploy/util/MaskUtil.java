package com.logai.deploy.util;

import com.logai.deploy.config.MaskProperties;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MaskUtil {

    private static final String REDACTED_IP = "[REDACTED_IP]";
    private static final String REDACTED_EMAIL = "[REDACTED_EMAIL]";
    private static final String REDACTED_UUID = "[REDACTED_UUID]";
    private static final String REDACTED_MAC = "[REDACTED_MAC]";
    private static final String REDACTED_JDBC = "[REDACTED_JDBC]";
    private static final String REDACTED_HOST = "[REDACTED_HOST]";
    private static final String REDACTED_CN = "[REDACTED_CN]";
    private static final String REDACTED_ID = "[REDACTED_ID]";
    private static final String REDACTED_FINGERPRINT = "[REDACTED_FINGERPRINT]";
    private static final String REDACTED_FILEPATH = "[REDACTED_FILEPATH]";

    private static final Pattern IPV4_PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(?:\\d{1,3}\\.){3}\\d{1,3}"
                            + "(?!\\d)"
            );

    private static final Pattern IPV6_PATTERN =
            Pattern.compile(
                    "(?i)(?<![0-9a-f:])"
                            + "(?=[0-9a-f:]*[a-f])"
                            + "(?:[0-9a-f]{1,4}:){2,7}"
                            + "[0-9a-f]{1,4}"
                            + "(?![0-9a-f:])"
            );

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,20}\\b"
            );

    private static final Pattern UUID_PATTERN =
            Pattern.compile(
                    "(?i)(?<![0-9a-f])"
                            + "[0-9a-f]{8}-"
                            + "[0-9a-f]{4}-"
                            + "[0-9a-f]{4}-"
                            + "[0-9a-f]{4}-"
                            + "[0-9a-f]{12}"
                            + "(?![0-9a-f])"
            );

    private static final Pattern MAC_PATTERN =
            Pattern.compile(
                    "(?i)(?<![0-9a-f])"
                            + "(?:[0-9a-f]{2}[:-]){5}"
                            + "[0-9a-f]{2}"
                            + "(?![0-9a-f])"
            );

    private static final Pattern FINGERPRINT_PATTERN =
            Pattern.compile(
                    "(?i)(?<![0-9a-f])"
                            + "[0-9a-f]{2}"
                            + "(?::[0-9a-f]{2}){19,}"
                            + "(?![0-9a-f])"
            );

    private static final Pattern HOST_PATTERN =
            Pattern.compile(
                    "(?i)\\bhost=[^\\s,}\"']+"
            );

    private static final Pattern CN_PATTERN =
            Pattern.compile(
                    "(?i)\\bCN=[^\\s,}\"']+"
            );

    private static final Pattern ID_PATTERN =
            Pattern.compile(
                    "(?i)\\bID=\\d+"
            );

    private static final Pattern JDBC_PATTERN =
            Pattern.compile(
                    "(?i)jdbc:[^\\s,\"'}]+"
            );

    /*
     * Supports:
     *
     * Windows:
     * C:\temp\application.log
     *
     * Unix/Linux:
     * /var/log/application/error.log
     */
    private static final Pattern FILEPATH_PATTERN =
            Pattern.compile(
                    "(?:[a-zA-Z]:)?"
                            + "(?:[\\\\/][\\w. @-]+)+"
            );

    private final MaskProperties properties;

    public MaskUtil(MaskProperties properties) {
        this.properties = properties;
    }

    public String mask(String line) {

        if (line == null || line.isEmpty()) {
            return line;
        }

        String value = line;

        /*
         * Fingerprint MUST be processed before MAC.
         *
         * A fingerprint starts with a MAC-shaped sequence.
         */
        if (properties.isFingerprint()) {
            value = replace(
                    value,
                    FINGERPRINT_PATTERN,
                    REDACTED_FINGERPRINT
            );
        }

        /*
         * MAC MUST be processed before IPv6.
         *
         * A MAC address such as:
         *
         * AA:BB:CC:DD:EE:FF
         *
         * can otherwise be incorrectly detected by a
         * permissive IPv6 pattern.
         */
        if (properties.isMac()) {
            value = replace(
                    value,
                    MAC_PATTERN,
                    REDACTED_MAC
            );
        }

        /*
         * IP
         */
        if (properties.isIp()) {
            value = replace(
                    value,
                    IPV4_PATTERN,
                    REDACTED_IP
            );

            value = replace(
                    value,
                    IPV6_PATTERN,
                    REDACTED_IP
            );
        }

        /*
         * Email
         */
        if (properties.isEmail()) {
            value = replace(
                    value,
                    EMAIL_PATTERN,
                    REDACTED_EMAIL
            );
        }

        /*
         * UUID
         */
        if (properties.isUuid()) {
            value = replace(
                    value,
                    UUID_PATTERN,
                    REDACTED_UUID
            );
        }

        /*
         * JDBC URL
         */
        if (properties.isJdbc()) {
            value = replace(
                    value,
                    JDBC_PATTERN,
                    REDACTED_JDBC
            );
        }

        /*
         * Host
         */
        if (properties.isHost()) {
            value = replace(
                    value,
                    HOST_PATTERN,
                    "host=" + REDACTED_HOST
            );
        }

        /*
         * Certificate CN
         */
        if (properties.isCn()) {
            value = replace(
                    value,
                    CN_PATTERN,
                    "CN=" + REDACTED_CN
            );
        }

        /*
         * ID
         */
        if (properties.isId()) {
            value = replace(
                    value,
                    ID_PATTERN,
                    "ID=" + REDACTED_ID
            );
        }

        /*
         * Windows file path
         */
        if (properties.isFilePath()) {
            value = replace(
                    value,
                    FILEPATH_PATTERN,
                    REDACTED_FILEPATH
            );
        }

        return value;
    }

    private static String replace(
            String value,
            Pattern pattern,
            String replacement) {

        return pattern.matcher(value)
                .replaceAll(replacement);
    }
}