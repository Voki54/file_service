package com.example.fileservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CompleteMultipartRequest(
        @JsonProperty("key") String key,
        @JsonProperty("uploadId") String uploadId,
        @JsonProperty("parts") List<CompletedPartDto> parts
) {
    @JsonCreator
    public CompleteMultipartRequest(
            @JsonProperty("key") String key,
            @JsonProperty("uploadId") String uploadId,
            @JsonProperty("parts") List<CompletedPartDto> parts
    ) {
        this.key = key;
        this.uploadId = uploadId;
        this.parts = parts;
    }
}

