package com.logai.deploy.cli;

import java.nio.file.Path;

public record CliOptions(
        Path inputLog,
        Path maskConfig,
        Path aiPrompt,
        boolean skipAiModelCheck) {

    public boolean hasMaskConfig() {
        return maskConfig != null;
    }

    public boolean hasAiPrompt() {
        return aiPrompt != null;
    }
}