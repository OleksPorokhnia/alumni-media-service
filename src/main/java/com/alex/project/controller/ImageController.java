package com.alex.project.controller;

import com.alex.project.dto.UrlRequest;
import com.alex.project.service.ImageService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jose4j.json.internal.json_simple.JSONArray;

import java.util.Map;

@Path("/images")
public class ImageController {

    @Inject
    ImageService storageService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @Path("/profile")
    public String profilePhoto(@RestForm("file") FileUpload file) {
        return storageService.photoUpload(file, "profile");
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getPresigned")
    public String getPresignedUrl(UrlRequest key){
        return storageService.createPresignedUrl(key.key());
    }

}
