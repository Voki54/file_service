package com.example.fileservice.exception;

public class FileDownloadException extends StorageException {

    public FileDownloadException(String key, Throwable cause) {
        super("Failed to download file with key: " + key, cause);
    }

    public FileDownloadException(String key, String details, Throwable cause) {
        super("Failed to download file with key: " + key + ". " + details, cause);
    }
}
