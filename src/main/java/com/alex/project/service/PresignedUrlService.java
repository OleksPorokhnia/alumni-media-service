package com.alex.project.service;

import com.alex.project.dto.response.PresignedUrlInfo;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PresignedUrlService {

    private static final String BUCKET = "alumni-media-service-s3-bucket";

    @ConfigProperty(name = "quarkus.s3.aws.region")
    String region;

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id")
    String accessKey;

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key")
    String secretAccessKey;

    private S3Presigner presigner;

    private final Map<String, PresignedUrlInfo> cache = new ConcurrentHashMap<>();

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

    public PresignedUrlInfo getPresignedUrl(String key){
        PresignedUrlInfo cached = cache.get(key);

        if(cached != null && !cached.isExpired()){
            return new PresignedUrlInfo(key, cached.url(), cached.expireAt());
        }

        PresignedUrlInfo info = generatePresignedUrl(key);
        cache.put(key, info);
        return new PresignedUrlInfo(key, info.url(), info.expireAt());
    }

    public List<PresignedUrlInfo> getPresignedUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<PresignedUrlInfo> responses = new ArrayList<>();
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
        String url = presigned.url().toExternalForm();
        Instant expiration = presigned.expiration();
        return new PresignedUrlInfo(key, url, expiration);
    }
}
