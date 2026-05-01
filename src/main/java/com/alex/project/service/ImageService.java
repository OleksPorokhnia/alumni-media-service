package com.alex.project.service;

import com.alex.project.dto.response.PresignedUrlInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@ApplicationScoped
public class ImageService {

    @Inject
    S3Client s3Client;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "quarkus.s3.aws.region")
    String region;

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id")
    String accessKey;

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key")
    String secretAccessKey;

    private static final String BUCKET = "alumni-media-service-s3-bucket";

    @PostConstruct
    void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretAccessKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    public String photoUpload(FileUpload fileUpload, String type) {

        String extension = getExtension(fileUpload.fileName());
        String key = type + "/" + jwt.claim("userid") + "." + extension;

        File file = fileUpload.uploadedFile().toFile();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType(fileUpload.contentType())
                .build();

        s3Client.putObject(request, RequestBody.fromFile(file));

        return key;
    }

    public String photoUpload(Path filePath, String contentType, String type) {

        String key = type + "/" + jwt.claim("userid") + ".webp";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(filePath.toFile()));

        return key;
    }

    public ResponseInputStream<GetObjectResponse> getPhoto(String key){
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File has no extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
