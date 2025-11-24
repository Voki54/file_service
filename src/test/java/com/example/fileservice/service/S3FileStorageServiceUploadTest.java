package com.example.fileservice.service;

import com.example.fileservice.config.S3Config;
import com.example.fileservice.model.FileMetadata;
import com.example.fileservice.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceUploadTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private S3Config config;

    @Mock
    private FileMetadataRepository metadataRepository;

    private S3FileStorageService service;


    @BeforeEach
    void setUp() {
        lenient().when(config.getBucket()).thenReturn("test-bucket");
        service = new S3FileStorageService(s3Client, config, metadataRepository);
    }

    @Test
    void uploadFile_success() throws Exception {
        byte[] content = "test-data".getBytes();
        when(multipartFile.getOriginalFilename()).thenReturn("file.txt");
        when(multipartFile.getBytes()).thenReturn(content);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(multipartFile.getSize()).thenReturn((long) content.length);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        String key = service.uploadFile(multipartFile, "user123");

        assertNotNull(key);
        assertTrue(key.contains("/user123/"));
        assertTrue(key.endsWith(".txt"));
        String uuidPart = key.substring(key.lastIndexOf('/') + 1, key.lastIndexOf('.'));
        assertDoesNotThrow(() -> UUID.fromString(uuidPart));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client, times(1))
                .putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest actualRequest = requestCaptor.getValue();
        assertEquals("test-bucket", actualRequest.bucket());
        assertEquals("text/plain", actualRequest.contentType());

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] expectedMd5 = md.digest(content);
        String expectedContentMd5 = Base64.getEncoder().encodeToString(expectedMd5);

        assertEquals(expectedContentMd5, actualRequest.contentMD5());

        ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(metadataRepository, times(1)).save(metadataCaptor.capture());

        FileMetadata saved = metadataCaptor.getValue();

        assertEquals("file.txt", saved.getOriginalName());
        assertEquals("text/plain", saved.getContentType());
        assertEquals("user123", saved.getOwnerId());
        assertEquals(content.length, saved.getSize());
        assertNotNull(saved.getUploadedAt());
    }

    @Test
    void uploadFile_shouldThrowRuntimeException_whenInputStreamFails() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("broken.txt");
        when(multipartFile.getInputStream()).thenThrow(new IOException("Cannot read stream"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.uploadFile(multipartFile, ""));

        assertTrue(ex.getMessage().contains("Failed to upload file"));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
