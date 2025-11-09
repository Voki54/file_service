package com.example.fileservice.service;

import com.example.fileservice.config.S3Config;
import com.example.fileservice.exception.FileUploadException;
import com.example.fileservice.exception.StorageException;
import com.example.fileservice.model.FileMetadata;
import com.example.fileservice.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

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

    @Override
    public String uploadFile(MultipartFile file, String ownerId) {
        String originalFilename = file.getOriginalFilename();
        log.info("Starting file upload: name='{}'", originalFilename);

        // TODO имя загружаемого файла должно содержать префиксы, включающие userId, userDirId и прочее
        String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
        log.debug("Generated S3 key: '{}' for original file: '{}'", key, originalFilename);

        try (InputStream input = file.getInputStream()) {
            long startTime = System.currentTimeMillis();

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(config.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(input, file.getSize()));

            FileMetadata metadata = FileMetadata.builder()
                    .storageKey(key)
                    .originalName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
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
            throw new FileUploadException("Error uploading file", e);
        }
    }

// TODO использовать eTag для контроля целостности данных

//    @Override
//    public String uploadFile(MultipartFile file) {
//        try (InputStream input = file.getInputStream()) {
//            long startTime = System.currentTimeMillis();
//
//            PutObjectResponse response = s3Client.putObject(
//                    PutObjectRequest.builder()
//                            .bucket(config.getBucket())
//                            .key(key)
//                            .contentType(contentType)
//                            .build(),
//                    RequestBody.fromInputStream(input, fileSize));
//
//            long duration = System.currentTimeMillis() - startTime;
//
//            log.info("File uploaded successfully: key='{}', size={} bytes, duration={}ms, eTag={}",
//                    key, fileSize, duration, response.eTag());
//
//            return key;
//        }
//    }

//    @Override
//    public byte[] downloadFile(String key) {
//        try (InputStream s3Object = s3Client.getObject(GetObjectRequest.builder()
//                .bucket(config.getBucket())
//                .key(key)
//                .build())) {
//            return s3Object.readAllBytes();
//        } catch (Exception e) {
//            throw new RuntimeException("Error downloading file", e);
//        }
//    }
//
//    @Override
//    public void deleteFile(String key) {
//        try {
//            s3Client.deleteObject(DeleteObjectRequest.builder()
//                    .bucket(config.getBucket())
//                    .key(key)
//                    .build());
//        } catch (Exception e) {
//            throw new RuntimeException("Error deleting file", e);
//        }
//    }
}
