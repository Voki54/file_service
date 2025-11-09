package com.example.fileservice.exception;

public class FileUploadException extends StorageException {

    public FileUploadException(String filename, Throwable cause) {
        super("Failed to upload file: " + filename, cause);
    }

    public FileUploadException(String filename, String details, Throwable cause) {
        super("Failed to upload file: " + filename + ". " + details, cause);
    }
}
