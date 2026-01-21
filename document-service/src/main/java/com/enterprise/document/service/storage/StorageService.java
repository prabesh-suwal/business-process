package com.enterprise.document.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Storage service abstraction - allows switching between Local, MinIO, S3, etc.
 */
public interface StorageService {

    /**
     * Store a file and return the storage path/key.
     */
    String store(MultipartFile file, String storedFilename);

    /**
     * Store a file from InputStream.
     */
    String store(InputStream inputStream, String storedFilename, String contentType, long size);

    /**
     * Retrieve a file as InputStream.
     */
    InputStream retrieve(String storagePath);

    /**
     * Delete a file from storage.
     */
    void delete(String storagePath);

    /**
     * Check if a file exists.
     */
    boolean exists(String storagePath);

    /**
     * Get the storage type identifier.
     */
    String getStorageType();
}
