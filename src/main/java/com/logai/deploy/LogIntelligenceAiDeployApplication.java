package com.logai.deploy;

import com.logai.deploy.analysis.LogAnalysisRunner;
import com.logai.deploy.cli.CliOptions;
import com.logai.deploy.cli.CliOptionsParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LogIntelligenceAiDeployApplication
        implements CommandLineRunner {

    private final CliOptionsParser cliOptionsParser;
    private final LogAnalysisRunner analysisRunner;

    public LogIntelligenceAiDeployApplication(
            CliOptionsParser cliOptionsParser,
            LogAnalysisRunner analysisRunner) {

        this.cliOptionsParser = cliOptionsParser;
        this.analysisRunner = analysisRunner;
    }

    public static void main(String[] args) {

        SpringApplication app =
                new SpringApplication(
                        LogIntelligenceAiDeployApplication.class
                );

        app.setWebApplicationType(
                WebApplicationType.NONE
        );

        app.run(args);
    }

    @Override
    public void run(String... ignored)
            throws Exception {

        CliOptions options =
                cliOptionsParser.parse();

        if (options == null) {
            cliOptionsParser.printUsage();
            return;
        }

        analysisRunner.run(options);
    }
}