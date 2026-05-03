package com.alex.project.service;

import com.alex.project.dto.response.PresignedUrlInfo;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PresignedUrlService {

    private static final String BUCKET = "alumni-media-service-s3-bucket";
    private final Map<String, PresignedUrlInfo> cache = new ConcurrentHashMap<>();
    @ConfigProperty(name = "quarkus.s3.aws.region")
    String region;
    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id")
    String accessKey;
    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key")
    String secretAccessKey;
    private S3Presigner presigner;

    @PostConstruct
    void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretAccessKey);
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @PreDestroy
    void destroy() {
        if (presigner != null) {
            presigner.close();
        }
    }


    public PresignedUrlInfo getPresignedUrl(String key) {
        return cache.compute(key, (k, existing) -> {
            if (existing != null && !existing.isExpired()) {
                return existing;
            }
            return generatePresignedUrl(k);
        });
    }

    public List<PresignedUrlInfo> getPresignedUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<PresignedUrlInfo> responses = new ArrayList<>(keys.size());

        for (String key : keys) {
            responses.add(getPresignedUrl(key));
        }

        return responses;
    }

    private PresignedUrlInfo generatePresignedUrl(String key) {

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        Duration duration = Duration.ofMinutes(20);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(objectRequest)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

        return new PresignedUrlInfo(
                key,
                presigned.url().toExternalForm(),
                presigned.expiration()
        );
    }

    public Map<String, PresignedUrlInfo> getCache() {
        return cache;
    }
}