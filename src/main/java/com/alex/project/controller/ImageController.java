package com.alex.project.controller;

import com.alex.project.controller.enums.PhotoOwnerType;
import com.alex.project.dto.response.PresignedUrlInfo;
import com.alex.project.service.CompressionService;
import com.alex.project.service.ImageService;
import com.alex.project.service.PresignedUrlService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@Path("/images")
public class ImageController {

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpeg", "jpg", "heic");

    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/png", "image/jpeg", "image/heic");

    @Inject
    ImageService imageService;

    @Inject
    CompressionService compressionService;

    @Inject
    PresignedUrlService presignedUrlService;

    private String processPhotoMetadata(FileUpload file, PhotoOwnerType type) {

        String filename = file.fileName();
        String extension = getExtension(filename);

        if (file.size() > MAX_SIZE ||
                !ALLOWED_EXTENSIONS.contains(extension) ||
                !ALLOWED_MIME_TYPES.contains(file.contentType())) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        return imageService.photoInitialUpload(file, type);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new WebApplicationException("Invalid filename", Response.Status.BAD_REQUEST);
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll // TODO: not all, but this is only for dev version!
    @Path("/profile")
    public String uploadProfilePhoto(@RestForm("file") FileUpload file) {
        return processPhotoMetadata(file, PhotoOwnerType.PROFILE);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll // TODO: not all, but this is only for dev version!
    @Path("/post")
    public String uploadPostPhoto(@RestForm("file") FileUpload file) {
        return processPhotoMetadata(file, PhotoOwnerType.POST);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll // TODO: not all, but this is only for dev version!
    @Path("/compress")
    public String compressPhoto(String key) throws IOException, InterruptedException {
        return compressionService.compressFile(key);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getPresigned/{key}")
    @Authenticated
    public PresignedUrlInfo getPresignedUrl(@PathParam("key") String key) {
        return presignedUrlService.getPresignedUrl(key);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getPresigned/batch")
    @Authenticated
    public List<PresignedUrlInfo> getPresignedUrls(List<String> keys) {
        return presignedUrlService.getPresignedUrls(keys);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Authenticated
    public void deletePhoto(String key) {
        imageService.deletePhoto(key);
    }
}