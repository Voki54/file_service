package com.example.fileservice.service;

import com.example.fileservice.config.S3Config;
import com.example.fileservice.exception.FileDownloadException;
import com.example.fileservice.exception.FileNotFoundException;
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
//        byte[] data = "hello".getBytes();
//        InputStream input = new ByteArrayInputStream(data);
//
////        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(input);
////        when(s3Client.getObject(any(GetObjectArgs.class))).thenReturn(input);
//        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
//                .thenReturn(input);
//
//
//        byte[] result = service.downloadFile("key");
//
//        assertArrayEquals(data, result);
//    }

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
