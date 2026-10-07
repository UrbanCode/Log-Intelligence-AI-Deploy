package com.logai.deploy.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskPropertiesTests {

    @Test
    void defaultsEnableAllMaskingRules() {

        MaskProperties properties =
                new MaskProperties();

        assertTrue(properties.isIp());
        assertTrue(properties.isEmail());
        assertTrue(properties.isUuid());
        assertTrue(properties.isMac());
        assertTrue(properties.isJdbc());
        assertTrue(properties.isHost());
        assertTrue(properties.isCn());
        assertTrue(properties.isId());
        assertTrue(properties.isFingerprint());
        assertTrue(properties.isFilePath());
    }

    @Test
    void appliesExternalOverrides(@TempDir Path tempDir)
            throws Exception {

        Path config =
                tempDir.resolve("mask.properties");

        Files.writeString(
                config,
                """
                mask.ip=false
                mask.email=false
                mask.uuid=true
                mask.mac=false
                mask.jdbc=true
                mask.host=false
                mask.cn=false
                mask.id=true
                mask.fingerprint=false
                mask.filepath=false
                """
        );

        MaskProperties properties =
                new MaskProperties();

        properties.applyOverrides(
                config.toString()
        );

        assertFalse(properties.isIp());
        assertFalse(properties.isEmail());
        assertTrue(properties.isUuid());
        assertFalse(properties.isMac());
        assertTrue(properties.isJdbc());
        assertFalse(properties.isHost());
        assertFalse(properties.isCn());
        assertTrue(properties.isId());
        assertFalse(properties.isFingerprint());
        assertFalse(properties.isFilePath());
    }
}