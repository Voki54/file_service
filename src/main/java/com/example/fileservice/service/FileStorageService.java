package com.example.fileservice.service;

import com.example.fileservice.dto.DownloadedFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String ownerId);
    DownloadedFile downloadFile(String fileName);
    void deleteFile(String fileName);
}

