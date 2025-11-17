package com.example.fileservice.controller;

import com.example.fileservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam MultipartFile file) {
        String key = fileStorageService.uploadFile(file, ""); // TODO Исправить! передавать Id пользователя
        return ResponseEntity.ok("File uploaded successfully: " + key);
    }

    @GetMapping()
    public ResponseEntity<byte[]> downloadFile(@RequestParam String key) {
        byte[] data = fileStorageService.downloadFile(key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteFile(@RequestParam String key) {
        fileStorageService.deleteFile(key);
        return ResponseEntity.noContent().build();
    }
}
