package com.example.fileservice.controller;

import com.example.fileservice.dto.CompleteMultipartRequest;
import com.example.fileservice.dto.DownloadedFile;
import com.example.fileservice.dto.InitiateMultipartRequest;
import com.example.fileservice.dto.InitiateMultipartResponse;
import com.example.fileservice.service.FilePresignedService;
import com.example.fileservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    private final FilePresignedService filePresignedService;


    /**
     * Initiate multipart upload (server creates uploadId and returns presigned URLs for parts).
     */
    @PostMapping("/multipart/init")
    public ResponseEntity<InitiateMultipartResponse> initiateMultipart(@RequestBody InitiateMultipartRequest request) {
        // ownerId можно извлечь из токена/контекста — здесь передаём пустую строку или mock
        InitiateMultipartResponse resp = filePresignedService.initiateMultipartUpload(request, "");
        return ResponseEntity.ok(resp);
    }

    /**
     * Complete multipart upload — client supplies list of (partNumber, etag) after having uploaded parts to the presigned URLs.
     */
    @PostMapping("/multipart/complete")
    public ResponseEntity<Void> completeMultipart(@RequestBody CompleteMultipartRequest request) {
        filePresignedService.completeMultipartUpload(request, "");
        return ResponseEntity.ok().build();
    }

//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<String> uploadFile(@RequestParam MultipartFile file) {
//        String key = fileStorageService.uploadFile(file, ""); // TODO Исправить! передавать Id пользователя
//        return ResponseEntity.ok("File uploaded successfully: " + key);
//    }

//    @GetMapping()
//    public ResponseEntity<byte[]> downloadFile(@RequestParam String key) {
//        byte[] data = fileStorageService.downloadFile(key);
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(data);
//    }

    @GetMapping()
    public ResponseEntity<byte[]> download(@RequestParam String key) {

        DownloadedFile file = fileStorageService.downloadFile(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.originalName() + "\""
                )
                .contentLength(file.size())
                .body(file.data());
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteFile(@RequestParam String key) {
        fileStorageService.deleteFile(key);
        return ResponseEntity.noContent().build();
    }
}
