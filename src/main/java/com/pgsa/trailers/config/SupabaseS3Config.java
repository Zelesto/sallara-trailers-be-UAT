// src/main/java/com/pgsa/trailers/config/SupabaseS3Config.java
package com.pgsa.trailers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@Slf4j
public class SupabaseS3Config {

    @Value("${supabase.s3.endpoint}")
    private String endpoint;

    @Value("${supabase.s3.region:us-east-1}")
    private String region;

    @Value("${supabase.s3.access-key-id}")
    private String accessKeyId;

    @Value("${supabase.s3.secret-access-key}")
    private String secretAccessKey;

    @Bean
    public S3Client supabaseS3Client() {
        log.info("Initializing Supabase S3 client with endpoint: {}", endpoint);
        
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)  // Required for Supabase S3
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner supabaseS3Presigner() {
        log.info("Initializing Supabase S3 presigner");
        
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)  // Required for Supabase S3
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }
}
