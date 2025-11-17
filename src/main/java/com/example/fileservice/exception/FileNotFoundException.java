package com.example.fileservice.exception;

public class FileNotFoundException extends StorageException {
    public FileNotFoundException(String key, Throwable cause) {
        super("File not found: key='" + key + "'", cause);
    }
}
