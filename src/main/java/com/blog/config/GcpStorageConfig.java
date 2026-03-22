package com.blog.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
@Profile("prod")
public class GcpStorageConfig {

    @Value("${gcp.storage.credentials-path:}")
    private String credentialsPath;

    @Bean
    public Storage googleCloudStorage() throws IOException {
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(credentialsPath))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        }

        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        } catch (IOException e) {
            log.warn("No GCP credentials found. GCP Storage will not be available. " +
                    "Set GCP_CREDENTIALS_PATH or configure Application Default Credentials.");
            return StorageOptions.getDefaultInstance().getService();
        }
    }
}
