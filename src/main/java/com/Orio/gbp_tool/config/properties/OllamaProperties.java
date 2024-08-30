package com.Orio.gbp_tool.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ollama.text")
public class OllamaProperties {
    private String model;

    // Getter and Setter
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}