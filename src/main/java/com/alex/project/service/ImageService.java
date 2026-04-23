package com.alex.project.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
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
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
