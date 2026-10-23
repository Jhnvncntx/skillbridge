package com.skillbridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the React front end call this API from another origin.
 * ALLOWED_ORIGIN = comma-separated list (e.g. your Render static-site URL).
 * Defaults to the Vite dev server for local development.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String raw = System.getenv().getOrDefault("ALLOWED_ORIGIN", "http://localhost:5173");
        String[] origins = raw.split("\\s*,\\s*");
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
