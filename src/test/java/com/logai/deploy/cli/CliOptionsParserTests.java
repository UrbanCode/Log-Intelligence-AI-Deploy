package com.logai.deploy.cli;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliOptionsParserTests {

    @Test
    void parsesInputLog() {

        CliOptions options =
                new CliOptionsParser(
                        new DefaultApplicationArguments(
                                "--inputLog=application.log"
                        )
                ).parse();

        assertEquals(
                Path.of("application.log"),
                options.inputLog()
        );
    }

    @Test
    void parsesMaskConfig() {

        CliOptions options =
                new CliOptionsParser(
                        new DefaultApplicationArguments(
                                "--inputLog=application.log",
                                "--maskConfig=mask.properties"
                        )
                ).parse();

        assertEquals(
                Path.of("mask.properties"),
                options.maskConfig()
        );
    }

    @Test
    void parsesAiPrompt() {

        CliOptions options =
                new CliOptionsParser(
                        new DefaultApplicationArguments(
                                "--inputLog=application.log",
                                "--aiPrompt=prompt.txt"
                        )
                ).parse();

        assertEquals(
                Path.of("prompt.txt"),
                options.aiPrompt()
        );
    }

    @Test
    void parsesSkipAiModelCheck() {

        CliOptions options =
                new CliOptionsParser(
                        new DefaultApplicationArguments(
                                "--inputLog=application.log",
                                "--skipAiModelCheck=true"
                        )
                ).parse();

        assertTrue(
                options.skipAiModelCheck()
        );
    }

    @Test
    void skipAiModelCheckDefaultsToFalse() {

        CliOptions options =
                new CliOptionsParser(
                        new DefaultApplicationArguments(
                                "--inputLog=application.log"
                        )
                ).parse();

        assertFalse(
                options.skipAiModelCheck()
        );
    }

    @Test
    void optionalArgumentsDefaultToNull() {

        CliOptions options =
                new CliOptionsParser(
                        new DefaultApplicationArguments(
                                "--inputLog=application.log"
                        )
                ).parse();

        assertNull(options.maskConfig());
        assertNull(options.aiPrompt());
    }

    @Test
    void blankInputLogIsHandledAsMissing() {

        CliOptions options =
                new CliOptionsParser(
                        new DefaultApplicationArguments(
                                "--inputLog="
                        )
                ).parse();

        assertNull(options);
    }
}