package com.enterprise.document.service.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MinIO object storage implementation.
 * Files are organized by date: bucket/yyyy/MM/dd/filename
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "document.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    @Value("${document.storage.minio.endpoint}")
    private String endpoint;

    @Value("${document.storage.minio.access-key}")
    private String accessKey;

    @Value("${document.storage.minio.secret-key}")
    private String secretKey;

    @Value("${document.storage.minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Ensure bucket exists
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO", e);
            throw new RuntimeException("Failed to initialize MinIO", e);
        }
    }

    @Override
    public String store(MultipartFile file, String storedFilename) {
        try {
            return store(file.getInputStream(), storedFilename, file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file for storage", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String storedFilename, String contentType, long size) {
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectName = datePath + "/" + storedFilename;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            log.debug("Stored file in MinIO: {}/{}", bucket, objectName);
            return objectName;
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to store file in MinIO", e);
        }
    }

    @Override
    public InputStream retrieve(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve file from MinIO: " + storagePath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            log.debug("Deleted file from MinIO: {}", storagePath);
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", storagePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "MINIO";
    }
}
