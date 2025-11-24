package com.example.fileservice.dto;

public record PartPresignedUrl(
        int partNumber,
        String url
) {}
