package com.logai.deploy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "logai.ai")
@Getter
@Setter
public class AiModelConfig {
    private String systemPrompt;
}