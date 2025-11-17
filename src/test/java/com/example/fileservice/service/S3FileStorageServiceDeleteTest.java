package com.example.fileservice.service;

import com.example.fileservice.config.S3Config;
import com.example.fileservice.exception.FileDownloadException;
import com.example.fileservice.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceDeleteTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Config config;

    @Mock
    private FileMetadataRepository metadataRepository;

    private S3FileStorageService service;

    @BeforeEach
    void setUp() {
        // lenient().
        when(config.getBucket()).thenReturn("test-bucket");
        service = new S3FileStorageService(s3Client, config, metadataRepository);
    }

    @Test
    void deleteFile_success() {
        when(metadataRepository.deleteByStorageKey("myFileKey")).thenReturn(1);

        service.deleteFile("myFileKey");

        verify(s3Client, times(1))
                .deleteObject(argThat((DeleteObjectRequest req) ->
                        "test-bucket".equals(req.bucket()) &&
                                "myFileKey".equals(req.key())
                ));

        verify(metadataRepository, times(1)).deleteByStorageKey("myFileKey");
    }

    @Test
    void deleteFile_s3Throws_exceptionPropagated() {
        doThrow(new RuntimeException("S3 error"))
                .when(s3Client)
                .deleteObject(any(DeleteObjectRequest.class));

        FileDownloadException ex = assertThrows(
                FileDownloadException.class,
                () -> service.deleteFile("myFileKey"));

        assertNotNull(ex.getCause());
        assertEquals("S3 error", ex.getCause().getMessage());
        assertTrue(ex.toString().contains("myFileKey"));

        verify(metadataRepository, times(0)).deleteByStorageKey(any());
    }

    @Test
    void deleteFile_metadataDeleteFails_logsWarning() {
        when(metadataRepository.deleteByStorageKey("myFileKey"))
                .thenThrow(new RuntimeException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.deleteFile("myFileKey"));

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        verify(metadataRepository, times(1)).deleteByStorageKey("myFileKey");
    }
}
