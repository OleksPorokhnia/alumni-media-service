package com.alex.project.controller;

import com.alex.project.dto.CompressDto;
import com.alex.project.dto.response.PresignedUrlInfo;
import com.alex.project.service.CompressionService;
import com.alex.project.service.ImageService;
import com.alex.project.service.PresignedUrlService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import javax.print.attribute.standard.Media;
import java.io.IOException;
import java.util.List;

@Path("/images")
public class ImageController {

    @Inject
    ImageService storageService;

    @Inject
    CompressionService compressionService;

    @Inject
    PresignedUrlService presignedUrlService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @PermitAll
    @Path("/profile")
    public String profilePhoto(@RestForm("file") FileUpload file) {
        return storageService.photoUpload(file, "profile");
    }

    @POST
    @Path("/compress")
    public void compressPhoto(CompressDto dto) throws IOException, InterruptedException {
        compressionService.compressFile(dto.type());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getPresigned/{key}")
    @Authenticated
    public PresignedUrlInfo getPresignedUrl(@PathParam("key") String key){
        return presignedUrlService.getPresignedUrl(key);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getPresigned/batch")
    @Authenticated
    public List<PresignedUrlInfo> getPresignedUrls(List<String> keys){
        return presignedUrlService.getPresignedUrls(keys);
    }

}
