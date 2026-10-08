package com.logai.deploy.util;

import com.logai.deploy.config.MaskProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskUtilTests {

    private MaskProperties properties;
    private MaskUtil maskUtil;

    @BeforeEach
    void setUp() {
        properties = new MaskProperties();
        maskUtil = new MaskUtil(properties);
    }

    @Test
    void masksIpv4Address() {

        String result =
                maskUtil.mask("Connection from 192.168.1.100 failed");

        assertTrue(result.contains("[REDACTED_IP]"));
        assertFalse(result.contains("192.168.1.100"));
    }

    @Test
    void masksEmailAddress() {

        String result =
                maskUtil.mask("User john.doe@example.com failed");

        assertTrue(result.contains("[REDACTED_EMAIL]"));
        assertFalse(result.contains("john.doe@example.com"));
    }

    @Test
    void masksUuid() {

        String uuid =
                "550e8400-e29b-41d4-a716-446655440000";

        String result =
                maskUtil.mask("Request id=" + uuid);

        assertTrue(result.contains("[REDACTED_UUID]"));
        assertFalse(result.contains(uuid));
    }

    @Test
    void masksMacAddress() {

        String result =
                maskUtil.mask("MAC 00:1A:2B:3C:4D:5E");

        assertTrue(result.contains("[REDACTED_MAC]"));
        assertFalse(result.contains("00:1A:2B:3C:4D:5E"));
    }

    @Test
    void masksJdbcUrl() {

        String jdbc =
                "jdbc:postgresql://localhost:5432/mydb";

        String result =
                maskUtil.mask("Database=" + jdbc);

        assertTrue(result.contains("[REDACTED_JDBC]"));
        assertFalse(result.contains(jdbc));
    }

    @Test
    void masksHost() {

        String result =
                maskUtil.mask("Connection failed host=production-server");

        assertTrue(
                result.contains("host=[REDACTED_HOST]")
        );

        assertFalse(
                result.contains("production-server")
        );
    }

    @Test
    void masksCommonName() {

        String result =
                maskUtil.mask("Certificate CN=production.example.com");

        assertTrue(
                result.contains("CN=[REDACTED_CN]")
        );

        assertFalse(
                result.contains("production.example.com")
        );
    }

    @Test
    void masksId() {

        String result =
                maskUtil.mask("Request ID=12345678");

        assertTrue(
                result.contains("ID=[REDACTED_ID]")
        );

        assertFalse(
                result.contains("12345678")
        );
    }

    @Test
    void masksFingerprint() {

        String fingerprint =
                "AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE";

        String result =
                maskUtil.mask("fingerprint=" + fingerprint);

        assertTrue(
                result.contains("[REDACTED_FINGERPRINT]")
        );

        assertFalse(
                result.contains(fingerprint)
        );
    }

    @Test
    void nullInputIsReturnedAsNull() {

        assertTrue(maskUtil.mask(null) == null);
    }

    @Test
    void emptyInputIsReturnedUnchanged() {

        assertTrue(maskUtil.mask("").isEmpty());
    }

    @Test
    void disabledMaskingLeavesValueUnchanged() {

        properties.setEmail(false);

        String email =
                "john.doe@example.com";

        String result =
                maskUtil.mask(email);

        assertTrue(result.contains(email));
    }

    @Test
    void multipleSensitiveValuesAreMasked() {

        String result = maskUtil.mask(
                "user=john@example.com ip=10.20.30.40"
        );

        assertTrue(
                result.contains("[REDACTED_EMAIL]")
        );

        assertTrue(
                result.contains("[REDACTED_IP]")
        );

        assertFalse(
                result.contains("john@example.com")
        );

        assertFalse(
                result.contains("10.20.30.40")
        );
    }
}