package com.example.fileservice.dto;

public record PresignedPutResponse(
        String url,
        String key
) {}

