package com.example.fileservice.service;

import com.example.fileservice.config.S3Config;
import com.example.fileservice.dto.CompleteMultipartRequest;
import com.example.fileservice.dto.InitiateMultipartRequest;
import com.example.fileservice.dto.InitiateMultipartResponse;
import com.example.fileservice.dto.PartPresignedUrl;
import com.example.fileservice.exception.FileUploadException;
import com.example.fileservice.model.FileMetadata;
import com.example.fileservice.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gradle.internal.impldep.com.amazonaws.services.s3.transfer.internal.CompleteMultipartUpload;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilePresignedService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3Config config;
    private final FileMetadataRepository metadataRepository;

    /// Минимальный размер части S3 = 5MB
    private static final int DEFAULT_PART_SIZE = 10 * 1024 * 1024; // 10 MB

    /// Метод для маленьких файлов
//    public PresignedPutResponse createPresignedPutUrl(String userId, String filename, String contentType, Duration timeToLive) {
//        String key = String.format("%s_%s", UUID.randomUUID(), normalizeFilename(filename));
//
//        PutObjectRequest putRequest = PutObjectRequest.builder()
//                .bucket(config.getBucket())
//                .key(key)
//                .contentType(contentType)
//                .build();
//
//        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
//                .signatureDuration(timeToLive != null ? timeToLive : Duration.ofMinutes(10))
//                .putObjectRequest(putRequest)
//                .build();
//
//        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
//
//        return new PresignedPutResponse(presigned.url().toString(), key);
//    }

//@Override
    public InitiateMultipartResponse initiateMultipartUpload(InitiateMultipartRequest request, String ownerId) {
        // String key = String.format("%s/%s_%s", ownerId, UUID.randomUUID(), normalizeFilename(filename));
        // TODO имя загружаемого файла должно содержать префиксы, включающие userId, userDirId и прочее
        String key = String.format("%s_%s", UUID.randomUUID(), normalizeFilename(request.filename()));

        CreateMultipartUploadRequest createReq = CreateMultipartUploadRequest.builder()
                .bucket(config.getBucket())
                .key(key)
                .contentType(request.contentType())
                .build();

        CreateMultipartUploadResponse createResp = s3Client.createMultipartUpload(createReq);
        String uploadId = createResp.uploadId();
        log.info("Created multipart upload: key={}, uploadId={}", key, uploadId);

        long partSize = request.partSize() != null && request.partSize() > 0 ? request.partSize() : DEFAULT_PART_SIZE;
        long totalSize = request.size();
        int partsCount = (int) ((totalSize + partSize - 1) / partSize);
        if (partsCount < 1) partsCount = 1;

        List<PartPresignedUrl> presignedParts = new ArrayList<>(partsCount);

        for (int partNumber = 1; partNumber <= partsCount; partNumber++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(config.getBucket())
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .contentLength(determinePartLength(partNumber, partSize, totalSize))
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            PresignedUploadPartRequest presigned = presigner.presignUploadPart(presignRequest);
            presignedParts.add(new PartPresignedUrl(partNumber, presigned.url().toString()));
        }

        return new InitiateMultipartResponse(key, uploadId, presignedParts, partSize);
    }

    public void completeMultipartUpload(CompleteMultipartRequest request, String ownerId) {
        if (request == null) throw new IllegalArgumentException("request == null");
        if (request.parts() == null || request.parts().isEmpty())
            throw new IllegalArgumentException("No parts supplied");


        List<CompletedPart> completedParts = request.parts().stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.partNumber())
                        .eTag(p.eTag())
                        .build())
                .collect(Collectors.toList());

        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(config.getBucket())
                .key(request.key())
                .uploadId(request.uploadId())
                .multipartUpload(completedMultipartUpload)
                .build();

        try {
            CompleteMultipartUploadResponse completeResp = s3Client.completeMultipartUpload(completeRequest);
            log.info("Completed multipart upload: key={}, location={}", request.key(), completeResp.location());

            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(request.key())
                    .build());

            FileMetadata metadata = FileMetadata.builder()
                    .storageKey(request.key())
                    .originalName(request.key())
                    .contentType(head.contentType())
                    .size(head.contentLength())
                    .ownerId(ownerId)
                    .uploadedAt(Instant.now())
                    .build();

            metadataRepository.save(metadata);

        } catch (S3Exception e) {
            log.error("Failed to complete multipart upload: key={}, uploadId={}", request.key(), request.uploadId(), e);
            throw new FileUploadException(request.key(), ownerId, e);
        }
    }


    private String normalizeFilename(String filename) {
        if (filename == null) return "file_" + Instant.now();
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    private long determinePartLength(int partNumber, long partSize, long totalSize) {
        long offset = (long) (partNumber - 1) * partSize;
        long remaining = totalSize - offset;
        log.info("Подсчёт части: partNumber={}, remaining={}", partNumber, remaining);
        return Math.max(0, Math.min(partSize, remaining));
    }
}

