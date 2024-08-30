package com.Orio.gbp_tool.config;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class OllamaConfig {
    @Bean
    public OllamaApi getOllamaApi() {
        return new OllamaApi();
    }

    @Value("${ollama.text.model}")
    public String model;
}
