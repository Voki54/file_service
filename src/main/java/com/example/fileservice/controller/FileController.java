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

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam MultipartFile file) {
        String key = fileStorageService.uploadFile(file, ""); // TODO Исправить! передавать Id пользователя
        return ResponseEntity.ok("File uploaded successfully: " + key);
    }

//    @GetMapping("/{key}")
//    public ResponseEntity<byte[]> downloadFile(@PathVariable String key) {
//        byte[] data = fileStorageService.downloadFile(key);
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(data);
//    }
//
//    @DeleteMapping("/{key}")
//    public ResponseEntity<Void> deleteFile(@PathVariable String key) {
//        fileStorageService.deleteFile(key);
//        return ResponseEntity.noContent().build();
//    }
}
