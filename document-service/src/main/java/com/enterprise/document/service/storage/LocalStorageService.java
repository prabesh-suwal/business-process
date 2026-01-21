package com.enterprise.document.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Local file system storage implementation.
 * Files are organized by date: base-path/yyyy/MM/dd/filename
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "document.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${document.storage.local.base-path:./uploads}")
    private String basePath;

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(basePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created local storage directory: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String storedFilename) {
        try {
            return store(file.getInputStream(), storedFilename, file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedFilename, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String storedFilename, String contentType, long size) {
        try {
            // Create date-based directory structure
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path directory = Paths.get(basePath, datePath);
            Files.createDirectories(directory);

            Path filePath = directory.resolve(storedFilename);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            String storagePath = datePath + "/" + storedFilename;
            log.debug("Stored file at: {}", storagePath);
            return storagePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedFilename, e);
        }
    }

    @Override
    public InputStream retrieve(String storagePath) {
        try {
            Path filePath = Paths.get(basePath, storagePath);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File not found: " + storagePath);
            }
            return new FileInputStream(filePath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve file: " + storagePath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path filePath = Paths.get(basePath, storagePath);
            Files.deleteIfExists(filePath);
            log.debug("Deleted file: {}", storagePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storagePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        Path filePath = Paths.get(basePath, storagePath);
        return Files.exists(filePath);
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }
}
