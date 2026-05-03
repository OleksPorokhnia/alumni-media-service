package com.alex.project.service;

import com.alex.project.controller.enums.PhotoOwnerType;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

@ApplicationScoped
public class ImageService {

    private static final String BUCKET = "alumni-media-service-s3-bucket";
    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "quarkus.s3.aws.region")
    String region;

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id")
    String accessKey;

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key")
    String secretAccessKey;
    private S3Client s3Client;

    @PostConstruct
    void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretAccessKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    public String photoInitialUpload(FileUpload fileUpload, PhotoOwnerType type) {

        String extension = getExtension(fileUpload.fileName());

        String key = switch (type) {
            case POST -> type.name().toLowerCase() + "/" + UUID.randomUUID() + "." + extension;
            case PROFILE -> type.name().toLowerCase() + "/" + jwt.claim("userid") + "." + extension;
            default -> throw new WebApplicationException(Response.Status.BAD_REQUEST);
        };

        File file = fileUpload.uploadedFile().toFile();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType(fileUpload.contentType())
                .build();

        s3Client.putObject(request, RequestBody.fromFile(file));

        return key;
    }

    public String photoUploadFinalization(Path filePath, String oldKey) {

        int dotIndex = oldKey.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new IllegalArgumentException("Invalid key format");
        }

        String newKey = oldKey.substring(0, dotIndex) + ".webp";

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(newKey)
                .contentType("image/webp")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromFile(filePath.toFile()));

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(BUCKET)
                .key(oldKey)
                .build();

        s3Client.deleteObject(deleteRequest);

        return newKey;
    }

    public ResponseInputStream<GetObjectResponse> getPhoto(String key) {
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

    public DeleteObjectResponse deletePhoto(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        return s3Client.deleteObject(deleteObjectRequest);
    }
}