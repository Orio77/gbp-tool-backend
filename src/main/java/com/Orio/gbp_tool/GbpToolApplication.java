package com.Orio.gbp_tool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.Orio.gbp_tool.config.properties.OllamaProperties;

@SpringBootApplication
@EnableConfigurationProperties(OllamaProperties.class)
public class GbpToolApplication {

	public static void main(String[] args) {
		SpringApplication.run(GbpToolApplication.class, args);
	}

}
