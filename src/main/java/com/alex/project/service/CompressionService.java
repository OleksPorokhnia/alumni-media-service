package com.alex.project.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class CompressionService {

    @Inject
    ImageService imageService;

    @Inject
    JsonWebToken jwt;

    public void compressFile(String type) throws IOException, InterruptedException {
        Path tempInput = Files.createTempFile("ffmpeg-in-", ".webp");
        Path tempOutput = Files.createTempFile("ffmpeg-out-", ".webp");

        try {
            Files.copy(imageService.getPhoto(type + "/" + jwt.claim("userid") + ".webp"), tempInput, StandardCopyOption.REPLACE_EXISTING);

            List<String> command = List.of(
                    "ffmpeg",
                    "-i", tempInput.toString(),
                    "-q:v", String.valueOf(75),
                    "-y", tempOutput.toString()
            );

            runCommand(command);

            imageService.photoUpload(tempOutput, "image/webp", type);
        }finally {
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);
        }
    }

    private void runCommand(List<String> command) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg timed out");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("FFmpeg failed:\n" + output);
        }
    }
}
