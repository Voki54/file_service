package com.example.fileservice.dto;

public record InitiateMultipartRequest(
        String filename,
        String contentType,
        long size,
        Integer partSize
) {}
