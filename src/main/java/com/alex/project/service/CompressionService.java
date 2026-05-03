package com.alex.project.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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

    public String compressFile(String oldKey) throws IOException, InterruptedException {

        if (!oldKey.contains(".")) {
            throw new IllegalArgumentException("Invalid key format");
        }

        String fileType = oldKey.substring(oldKey.lastIndexOf('.') + 1);

        Path tempInput = Files.createTempFile("ffmpeg-in-", "." + fileType);
        Path tempOutput = Files.createTempFile("ffmpeg-out-", ".webp");

        try {
            try (InputStream is = imageService.getPhoto(oldKey)) {
                Files.copy(is, tempInput, StandardCopyOption.REPLACE_EXISTING);
            }

            List<String> command = List.of(
                    "ffmpeg",
                    "-i", tempInput.toString(),
                    "-q:v", "75",
                    "-y", tempOutput.toString()
            );

            runCommand(command);

            return imageService.photoUploadFinalization(tempOutput, oldKey);

        } finally {
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);
        }
    }

    private void runCommand(List<String> command) throws IOException, InterruptedException {
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

        boolean finished = process.waitFor(1, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg timed out");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("FFmpeg failed:\n" + output);
        }
    }
}