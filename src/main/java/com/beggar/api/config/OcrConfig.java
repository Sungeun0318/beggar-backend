package com.beggar.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

@Configuration
public class OcrConfig {

    @Value("${google.vision.credentials.path}")
    private String credentialsPath;

    @Bean
    public ImageAnnotatorClient imageAnnotatorClient(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource(credentialsPath);
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
        return ImageAnnotatorClient.create(settings);
    }
}
