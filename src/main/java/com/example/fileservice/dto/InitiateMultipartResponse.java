package com.example.fileservice.dto;

import java.util.List;

public record InitiateMultipartResponse(
        String key,
        String uploadId,
        List<PartPresignedUrl> presignedParts,
        long partSize
) {}