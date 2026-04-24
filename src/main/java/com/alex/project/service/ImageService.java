package com.alex.project.service;

import com.alex.project.dto.response.PresignedUrlInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

@ApplicationScoped
public class ImageService {

    @Inject
    S3Client s3Client;

    private static final String BUCKET = "alumni-media-service-s3-bucket";

    public String photoUpload(FileUpload fileUpload, String type) {

        String extension = getExtension(fileUpload.fileName());
        String key = type + "/" + UUID.randomUUID() + "." + extension;

        File file = fileUpload.uploadedFile().toFile();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType(fileUpload.contentType())
                .build();

        s3Client.putObject(request, RequestBody.fromFile(file));

        return key;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File has no extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
