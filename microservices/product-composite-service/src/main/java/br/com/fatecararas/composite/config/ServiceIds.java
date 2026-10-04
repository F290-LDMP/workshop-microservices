package br.com.fatecararas.composite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.services")
public record ServiceIds(String product, String recommendation, String review) { }
