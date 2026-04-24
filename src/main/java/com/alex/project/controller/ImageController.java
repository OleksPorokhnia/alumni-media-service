package com.alex.project.controller;

import com.alex.project.dto.response.PresignedUrlInfo;
import com.alex.project.service.ImageService;
import com.alex.project.service.PresignedUrlService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import javax.print.attribute.standard.Media;
import java.util.List;

@Path("/images")
public class ImageController {

    @Inject
    ImageService storageService;

    @Inject
    PresignedUrlService presignedUrlService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @Path("/profile")
    public String profilePhoto(@RestForm("file") FileUpload file) {
        return storageService.photoUpload(file, "profile");
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
