package com.example.fileservice.dto;

import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;

public record CompleteMultipartRequest(
        String key,
        String uploadId,
        List<CompletedPart> parts
) {
}
