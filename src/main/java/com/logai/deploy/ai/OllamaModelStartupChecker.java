package com.logai.deploy.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class OllamaModelStartupChecker {

    private final String baseUrl;
    private final String configuredModel;
    private final ObjectMapper mapper;
    private final HttpClient client;

    public OllamaModelStartupChecker(
            @Value(
                    "${spring.ai.ollama.base-url:http://localhost:11434}"
            )
            String baseUrl,

            @Value(
                    "${spring.ai.ollama.chat.options.model:}"
            )
            String configuredModel,

            ObjectMapper mapper) {

        this.baseUrl =
                trimTrailingSlash(baseUrl);

        this.configuredModel =
                configuredModel == null
                        ? ""
                        : configuredModel.trim();

        this.mapper = mapper;

        this.client =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(3)
                        )
                        .build();
    }

    public Optional<String> validateConfiguredModel() {

        if (configuredModel.isBlank()) {

            return Optional.of(
                    "Ollama model is not configured. "
                            + "Set "
                            + "spring.ai.ollama.chat.options.model "
                            + "in application.yml"
            );
        }

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            baseUrl
                                                    + "/api/tags"
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(5)
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                return Optional.of(
                        "Unable to verify Ollama models "
                                + "(HTTP "
                                + response.statusCode()
                                + ")."
                );
            }

            List<String> available =
                    parseModelNames(
                            response.body()
                    );

            boolean found =
                    available.stream()
                            .anyMatch(
                                    name ->
                                            name.equalsIgnoreCase(
                                                    configuredModel
                                            )
                            );

            if (found) {
                return Optional.empty();
            }

            String availableModels =
                    available.isEmpty()
                            ? "none"
                            : String.join(
                            ", ",
                            available
                    );

            return Optional.of(
                    "Configured model '"
                            + configuredModel
                            + "' is not available in Ollama. "
                            + "Available: "
                            + availableModels
                            + ". Run: ollama pull "
                            + configuredModel
            );

        } catch (Exception ex) {

            return Optional.of(
                    "Unable to verify Ollama model "
                            + "at startup: "
                            + ex.getMessage()
            );
        }
    }

    private List<String> parseModelNames(
            String body) throws Exception {

        List<String> names =
                new ArrayList<>();

        JsonNode root =
                mapper.readTree(body);

        JsonNode models =
                root.path("models");

        if (!models.isArray()) {
            return names;
        }

        for (JsonNode model : models) {

            String name =
                    model.path("name")
                            .asText("")
                            .trim();

            if (!name.isBlank()) {

                names.add(
                        name.toLowerCase(
                                Locale.ROOT
                        )
                );
            }
        }

        return names;
    }

    private String trimTrailingSlash(
            String value) {

        if (value == null || value.isBlank()) {
            return "http://localhost:11434";
        }

        String result =
                value.trim();

        while (result.endsWith("/")
                && result.length() > 1) {

            result =
                    result.substring(
                            0,
                            result.length() - 1
                    );
        }

        return result;
    }
}