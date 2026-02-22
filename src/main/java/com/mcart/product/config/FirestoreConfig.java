package com.mcart.product.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Value("${gcp.firestore.project-id}")
    private String projectId;

    @Value("${gcp.firestore.credentials-path}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(new FileInputStream(credentialsPath));

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        return options.getService();
    }
}