package com.example.fileservice.exception;

public class FileUploadException extends StorageException {
    public FileUploadException(String fileKey, String ownerId, Throwable cause) {
        super("Failed to upload file: " + fileKey + "; owner: " + ownerId, cause);
    }
}
