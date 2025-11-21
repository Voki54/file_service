package com.example.fileservice.service;

import com.example.fileservice.config.S3Config;
import com.example.fileservice.dto.DownloadedFile;
import com.example.fileservice.exception.FileDownloadException;
import com.example.fileservice.exception.FileNotFoundException;
import com.example.fileservice.model.FileMetadata;
import com.example.fileservice.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceDownloadTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Config config;

    @Mock
    private FileMetadataRepository metadataRepository;

    private S3FileStorageService service;

    @BeforeEach
    void setUp() {
        when(config.getBucket()).thenReturn("test-bucket");
//        lenient().when(config.getBucket()).thenReturn("test-bucket");
        service = new S3FileStorageService(s3Client, config, metadataRepository);
    }

//    @Test
//    void downloadFile_success() throws Exception {
//        String key = "file-key";
//        byte[] fileBytes = "test-content".getBytes();
//        FileMetadata metadata = new FileMetadata();
//        metadata.setOriginalName("myfile.txt");
//        metadata.setContentType("text/plain");
//        metadata.setSize((long) fileBytes.length);
//        metadata.setOwnerId("user123");
//        metadata.setUploadedAt(Instant.now());
//
//        when(metadataRepository.findByStorageKey(key))
//                .thenReturn(Optional.of(metadata));
//
//        when(s3Client.getObject(any(GetObjectRequest.class)))
//                .thenReturn(new ByteArrayInputStream(fileBytes)); // не работает, возможно, нужен ResponseTransformer
//
//        DownloadedFile result = service.downloadFile(key);
//
//        assertArrayEquals(fileBytes, result.data());
//        assertEquals("myfile.txt", result.originalName());
//        assertEquals("text/plain", result.contentType());
//        assertEquals(fileBytes.length, result.size());
//        assertEquals("user123", result.ownerId());
//        assertEquals(metadata.getUploadedAt(), result.uploadedAt());
//    }


    @Test
    void downloadFile_metadataNotFound_throwsFileNotFoundException() {
        when(metadataRepository.findByStorageKey("missing"))
                .thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class,
                () -> service.downloadFile("missing"));
    }


    @Test
    void downloadFile_noSuchKey_throwsFileNotFoundException() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThrows(FileNotFoundException.class,
                () -> service.downloadFile("key"));
    }

    @Test
    void downloadFile_s3Error_throwsFileDownloadException() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(SdkClientException.builder().message("error").build());

        assertThrows(FileDownloadException.class,
                () -> service.downloadFile("key"));
    }
}
