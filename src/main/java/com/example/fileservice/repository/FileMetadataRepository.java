package com.example.fileservice.repository;

import com.example.fileservice.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    Optional<FileMetadata> findByStorageKey(String storageKey);
    int deleteByStorageKey(String storageKey);
}
