package com.example.fileservice.dto;

import java.time.Instant;

public record DownloadedFile(
        byte[] data,
        String originalName,
        String contentType,
        long size,
        String ownerId,
        Instant uploadedAt
) {}

