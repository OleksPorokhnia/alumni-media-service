package com.alex.project.dto.response;

import java.time.Instant;

public record PresignedUrlInfo(String key, String url, Instant expireAt) {

    public boolean isExpired(){
        Instant now = Instant.now();

        long bufferSeconds = 60;
        return now.isAfter(expireAt.minusSeconds(bufferSeconds));
    }
}
