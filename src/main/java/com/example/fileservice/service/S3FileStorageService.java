package com.example.fileservice.service;

import com.example.fileservice.config.S3Config;
import com.example.fileservice.dto.DownloadedFile;
import com.example.fileservice.exception.*;
import com.example.fileservice.model.FileMetadata;
import com.example.fileservice.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import static com.example.fileservice.util.S3KeyGenerator.generateKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Config config;
    private final FileMetadataRepository metadataRepository;

    @PostConstruct
    public void init() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(config.getBucket())
                    .build());
            log.info("Bucket '{}' created successfully", config.getBucket());
        } catch (S3Exception e) {
            if ("BucketAlreadyOwnedByYou".equals(e.awsErrorDetails().errorCode())) {
                log.info("Bucket '{}' already exists and is owned by you", config.getBucket());
            } else if ("BucketAlreadyExists".equals(e.awsErrorDetails().errorCode())) {
                log.warn("Bucket '{}' already exists but owned by another account", config.getBucket());
            } else {
                log.error("Error creating bucket '{}'. Error code: {}, Message: {}",
                        config.getBucket(), e.awsErrorDetails().errorCode(),
                        e.awsErrorDetails().errorMessage(), e);
                throw new StorageException("Error creating bucket", e);
            }
        } catch (Exception e) {
            log.error("Unexpected error during bucket initialization for '{}'", config.getBucket(), e);
            throw new StorageException("Unexpected error during bucket initialization", e);
        }

        log.info("S3FileStorageService initialization completed");
    }

    // TODO использовать eTag для контроля целостности данных
    @Override
    public String uploadFile(MultipartFile file, String ownerId) {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        log.info("Starting file upload: name='{}'", originalFilename);

        String key = generateKey(ownerId, originalFilename);
        log.debug("Generated S3 key: '{}' for original file: '{}'", key, originalFilename);

        try (InputStream input = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = file.getBytes();
            byte[] md5Bytes = md.digest(fileBytes);
            String contentMd5 = Base64.getEncoder().encodeToString(md5Bytes);

            long startTime = System.currentTimeMillis();

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(config.getBucket())
                            .key(key)
                            .contentType(contentType)
                            .contentMD5(contentMd5)
                            .build(),
                    RequestBody.fromInputStream(input, fileSize));

            FileMetadata metadata = FileMetadata.builder()
                    .storageKey(key)
                    .originalName(originalFilename)
                    .contentType(contentType)
                    .size(fileSize)
                    .ownerId(ownerId)
                    .uploadedAt(Instant.now())
                    .build();

            metadataRepository.save(metadata);

            log.info("File uploaded successfully: key='{}', duration={}ms",
                    key, System.currentTimeMillis() - startTime);
            return key;
        } catch (Exception e) {
            log.error("Error uploading file: name='{}', key='{}'",
                    originalFilename, key, e);
            throw new FileUploadException(key, ownerId, e);
        }
    }

    @Override
    public DownloadedFile downloadFile(String key) {
        FileMetadata metadata = metadataRepository.findByStorageKey(key)
                .orElseThrow(() -> new FileNotFoundException(key));

        try (InputStream s3Object = s3Client.getObject(GetObjectRequest.builder()
                .bucket(config.getBucket())
                .key(key)
                .build())) {

            byte[] bytes = s3Object.readAllBytes();

            return new DownloadedFile(
                    bytes,
                    metadata.getOriginalName(),
                    metadata.getContentType(),
                    metadata.getSize(),
                    metadata.getOwnerId(),
                    metadata.getUploadedAt()
            );
        } catch (NoSuchKeyException e) {
            log.warn("File not found: key='{}'", key);
            throw new FileNotFoundException(key, e);
        } catch (Exception e) {
            log.error("Error downloading file: key='{}'", key, e);
            throw new FileDownloadException(key, e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(key)
                    .build());
            int deleted = metadataRepository.deleteByStorageKey(key);
            if (deleted == 0) {
                log.warn("No metadata found for key '{}'", key);
            }

            log.info("File '{}' deleted successfully from S3 and metadata removed", key);
        } catch (Exception e) {
            throw new FileDeleteException(key, e);
        }
    }
}
