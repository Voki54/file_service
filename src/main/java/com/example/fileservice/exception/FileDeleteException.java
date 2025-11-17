package com.example.fileservice.exception;

public class FileDeleteException extends StorageException {
    public FileDeleteException(String key, Throwable cause) {
        super("Failed to download file with key: " + key, cause);
    }
}
