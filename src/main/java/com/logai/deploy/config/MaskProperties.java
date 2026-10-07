package com.logai.deploy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Component
@ConfigurationProperties(prefix = "logai.mask")
@Getter
@Setter
public class MaskProperties {

    private boolean ip = true;
    private boolean email = true;
    private boolean uuid = true;
    private boolean mac = true;
    private boolean jdbc = true;
    private boolean host = true;
    private boolean cn = true;
    private boolean id = true;
    private boolean fingerprint = true;
    private boolean filePath = true;

    public void applyOverrides(String propertiesFile) throws IOException {

        Properties overrides = new Properties();

        try (InputStream inputStream =
                     Files.newInputStream(Path.of(propertiesFile))) {

            overrides.load(inputStream);
        }

        for (String key : overrides.stringPropertyNames()) {

            if (!key.startsWith("mask.")) {
                continue;
            }

            String value = overrides.getProperty(key);

            switch (key) {
                case "mask.ip" ->
                        ip = parseBoolean(value, key);

                case "mask.email" ->
                        email = parseBoolean(value, key);

                case "mask.uuid" ->
                        uuid = parseBoolean(value, key);

                case "mask.mac" ->
                        mac = parseBoolean(value, key);

                case "mask.jdbc" ->
                        jdbc = parseBoolean(value, key);

                case "mask.host" ->
                        host = parseBoolean(value, key);

                case "mask.cn" ->
                        cn = parseBoolean(value, key);

                case "mask.id" ->
                        id = parseBoolean(value, key);

                case "mask.fingerprint" ->
                        fingerprint = parseBoolean(value, key);

                case "mask.file-path", "mask.filepath" ->
                        filePath = parseBoolean(value, key);

                default ->
                    // Ignore unknown mask properties.
                    // This allows future properties to be added
                    // without breaking older versions.
                        System.out.println(
                                "Ignoring unknown mask property: " + key
                        );
            }
        }
    }

    private boolean parseBoolean(String value, String key) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing value for mask property: " + key
            );
        }

        if (!"true".equalsIgnoreCase(value)
                && !"false".equalsIgnoreCase(value)) {

            throw new IllegalArgumentException(
                    "Invalid boolean value for "
                            + key
                            + ": "
                            + value
                            + ". Expected true or false."
            );
        }

        return Boolean.parseBoolean(value);
    }
}