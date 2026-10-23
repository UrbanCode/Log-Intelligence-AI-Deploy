package com.logai.deploy.ai;

import com.logai.deploy.config.MaskProperties;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PIIMaskingAdvisor implements CallAdvisor {

    private static final String REDACTED_IP = "[REDACTED_IP]";
    private static final String REDACTED_EMAIL = "[REDACTED_EMAIL]";
    private static final String REDACTED_UUID = "[REDACTED_UUID]";
    private static final String REDACTED_MAC = "[REDACTED_MAC]";
    private static final String REDACTED_JDBC = "[REDACTED_JDBC]";
    private static final String REDACTED_HOST = "[REDACTED_HOST]";
    private static final String REDACTED_CN = "[REDACTED_CN]";
    private static final String REDACTED_ID = "[REDACTED_ID]";
    private static final String REDACTED_FINGERPRINT = "[REDACTED_FINGERPRINT]";
    private static final String REDACTED_CHANNEL = "ch=[CHAN]";
    private static final String REDACTED_REDACTED_IP_PORT = "[REDACTED_IP]:[PORT]";

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

    private static final Pattern CHANNEL_ID_PATTERN =
            Pattern.compile(
                    "\\bch=[0-9a-fA-F]{6,}\\b"
            );

    private static final Pattern REDACTED_IP_PORT_PATTERN =
            Pattern.compile(
                    "\\[REDACTED_IP]:(\\d{2,5})\\b"
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

    private final MaskProperties properties;

    public PIIMaskingAdvisor(MaskProperties properties) {
        this.properties = properties;
    }

    @Override
    @NonNull
    public String getName() {
        return "piiMaskingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @NonNull
    public ChatClientResponse adviseCall(
            @NonNull ChatClientRequest request,
            @NonNull CallAdvisorChain chain) {

        Prompt maskedPrompt = maskPrompt(request.prompt());

        return chain.nextCall(
                request.mutate()
                        .prompt(maskedPrompt)
                        .build()
        );
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
        if (properties.isIpPort()) {
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

        value = replace(
                value,
                CHANNEL_ID_PATTERN,
                REDACTED_CHANNEL
        );


        value = replace(
                value,
                REDACTED_IP_PORT_PATTERN,
                REDACTED_REDACTED_IP_PORT
        );

        return value;
    }

    private Prompt maskPrompt(Prompt prompt) {

        List<Message> maskedMessages =
                new ArrayList<>();

        for (Message message : prompt.getInstructions()) {

            if (message instanceof UserMessage) {
                maskedMessages.add(
                        new UserMessage(mask(message.getText()))
                );
                continue;
            }

            if (message instanceof SystemMessage) {
                maskedMessages.add(
                        new SystemMessage(mask(message.getText()))
                );
                continue;
            }

            maskedMessages.add(message);
        }

        return new Prompt(
                maskedMessages,
                prompt.getOptions()
        );
    }

    private static String replace(
            String value,
            Pattern pattern,
            String replacement) {

        return pattern.matcher(value)
                .replaceAll(replacement);
    }
}


