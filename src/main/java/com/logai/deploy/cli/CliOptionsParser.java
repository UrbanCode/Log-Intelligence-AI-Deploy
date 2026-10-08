package com.logai.deploy.cli;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class CliOptionsParser {

    private final ApplicationArguments args;

    public CliOptionsParser(ApplicationArguments args) {
        this.args = args;
    }

    public CliOptions parse() {

        String inputLog =
                requiredOption("inputLog");

        if (inputLog == null) {
            return null;
        }

        String maskConfig =
                firstOption("maskConfig");

        String aiPrompt =
                firstOption("aiPrompt");

        boolean skipAiModelCheck =
                isTrueOption("skipAiModelCheck");

        return new CliOptions(
                Path.of(inputLog),
                maskConfig == null
                        ? null
                        : Path.of(maskConfig),
                aiPrompt == null
                        ? null
                        : Path.of(aiPrompt),
                skipAiModelCheck
        );
    }

    private String requiredOption(String name) {

        List<String> values =
                args.getOptionValues(name);

        if (values == null
                || values.isEmpty()
                || values.get(0).isBlank()) {

            System.out.println(
                    "Missing required option --"
                            + name
                            + "=<path>"
            );

            return null;
        }

        return values.get(0);
    }

    private String firstOption(String name) {

        List<String> values =
                args.getOptionValues(name);

        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    private boolean isTrueOption(String name) {

        String value =
                firstOption(name);

        return value != null
                && "true".equalsIgnoreCase(value);
    }

    public void printUsage() {

        System.out.println(
                "Usage:"
        );

        System.out.println(
                "java -jar log-intelligence-ai-deploy.jar "
                        + "--inputLog=<logfile> "
                        + "[--maskConfig=<mask.properties>] "
                        + "[--aiPrompt=<prompt.txt>] "
                        + "[--skipAiModelCheck=true]"
        );
    }
}