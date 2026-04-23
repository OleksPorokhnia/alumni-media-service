package com.alex.project.service;

import com.alex.project.dto.response.BatchUrlResponse;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.utils.IoUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private S3Presigner presigner;


    @PostConstruct
    void init(){
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretAccessKey);

        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @PreDestroy
    void destroy() {
        presigner.close();
    }

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

    public BatchUrlResponse getPresignedUrls(List<String> urls){
        Map<String, String> result = urls.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        this::createPresignedUrl
                ));

        return new BatchUrlResponse(result);
    }

    public String createPresignedUrl(String keyName){
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(BUCKET)
                    .key(keyName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(20))
                    .getObjectRequest(objectRequest)
                    .build();


            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(presignRequest);

            return presignedGetObjectRequest.url().toExternalForm();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File has no extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
