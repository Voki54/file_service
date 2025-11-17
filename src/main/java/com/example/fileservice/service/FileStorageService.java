package com.example.fileservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String ownerId);
    byte[] downloadFile(String fileName);
    void deleteFile(String fileName);
}

