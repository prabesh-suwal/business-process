package com.enterprise.memo.service;

import io.minio.*;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
public class StorageService {

    @Autowired(required = false)
    private MinioClient minioClient;

    @Value("${storage.type:filesystem}") // Default to filesystem for dev
    private String storageType;

    @Value("${storage.location:uploads}")
    private String storageLocation;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @SneakyThrows
    public void uploadFile(String objectName, InputStream inputStream, String contentType, long size) {
        if ("filesystem".equalsIgnoreCase(storageType)) {
            // File System Logic
            java.nio.file.Path rootLocation = java.nio.file.Paths.get(storageLocation);
            java.nio.file.Files.createDirectories(rootLocation);

            // Allow subdirectories in objectName (e.g. memoId/filename)
            java.nio.file.Path destinationFile = rootLocation.resolve(objectName).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())
                    && !destinationFile.getParent().startsWith(rootLocation.toAbsolutePath())) {
                // Creating sub-directories if needed
                java.nio.file.Files.createDirectories(destinationFile.getParent());
            }

            java.nio.file.Files.copy(inputStream, destinationFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else {
            // MinIO Logic
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
        }
    }

    @SneakyThrows
    public InputStream downloadFile(String objectName) {
        if ("filesystem".equalsIgnoreCase(storageType)) {
            java.nio.file.Path file = java.nio.file.Paths.get(storageLocation).resolve(objectName);
            return java.nio.file.Files.newInputStream(file);
        } else {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        }
    }

    @SneakyThrows
    public void deleteFile(String objectName) {
        if ("filesystem".equalsIgnoreCase(storageType)) {
            java.nio.file.Path file = java.nio.file.Paths.get(storageLocation).resolve(objectName);
            java.nio.file.Files.deleteIfExists(file);
        } else {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        }
    }
}
