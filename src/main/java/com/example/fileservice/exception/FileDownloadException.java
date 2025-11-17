package com.example.fileservice.exception;

public class FileDownloadException extends StorageException {
    public FileDownloadException(String key, Throwable cause) {
        super("Failed to delete file with key: " + key, cause);
    }
}
