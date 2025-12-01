package com.example.fileservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CompletedPartDto(
        @JsonProperty("partNumber") int partNumber,
        @JsonProperty("eTag") String eTag
) {
    // Опционально: @JsonCreator для явной десериализации (полезно для records в старых Jackson)
    @JsonCreator
    public CompletedPartDto(@JsonProperty("partNumber") int partNumber, @JsonProperty("eTag") String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }
}