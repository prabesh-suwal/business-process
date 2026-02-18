package com.enterprise.memo.client;

import com.cas.common.webclient.InternalWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for calling document-service.
 * Follows the same WebClient pattern as WorkflowClient.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceClient {

    @InternalWebClient
    private final WebClient.Builder webClientBuilder;

    @Value("${services.document-service.url:http://localhost:9005}")
    private String documentServiceUrl;

    /**
     * Upload a file to document-service.
     * Returns the document metadata including id, downloadUrl, etc.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> upload(MultipartFile file, String linkedEntityType, UUID linkedEntityId) {
        log.info("Uploading file '{}' to document-service (entity: {} / {})",
                file.getOriginalFilename(), linkedEntityType, linkedEntityId);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            builder.part("file", file.getResource());
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare file for upload", e);
        }

        if (linkedEntityType != null) {
            builder.part("linkedEntityType", linkedEntityType);
        }
        if (linkedEntityId != null) {
            builder.part("linkedEntityId", linkedEntityId.toString());
        }
        builder.part("documentType", "OTHER");

        // Note: X-User-Id and X-User-Name headers are automatically propagated
        // by UserContextWebClientFilter — do NOT set them manually here
        return webClientBuilder.build()
                .post()
                .uri(documentServiceUrl + "/api/documents")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * Download a file from document-service.
     * Returns the file as an InputStream.
     */
    public InputStream download(UUID documentId) {
        log.debug("Downloading document {} from document-service", documentId);

        Flux<DataBuffer> dataBufferFlux = webClientBuilder.build()
                .get()
                .uri(documentServiceUrl + "/api/documents/" + documentId + "/download")
                .retrieve()
                .bodyToFlux(DataBuffer.class);

        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream inputStream;
        try {
            inputStream = new PipedInputStream(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create piped stream", e);
        }

        DataBufferUtils.write(dataBufferFlux, outputStream)
                .subscribe(
                        DataBufferUtils.releaseConsumer(),
                        error -> {
                            try {
                                outputStream.close();
                            } catch (IOException ignored) {
                            }
                            log.error("Error downloading document {}", documentId, error);
                        },
                        () -> {
                            try {
                                outputStream.close();
                            } catch (IOException ignored) {
                            }
                        });

        return inputStream;
    }

    /**
     * Get document metadata from document-service.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDocument(UUID documentId) {
        return webClientBuilder.build()
                .get()
                .uri(documentServiceUrl + "/api/documents/" + documentId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * Soft-delete a document in document-service.
     */
    public void delete(UUID documentId) {
        log.info("Deleting document {} from document-service", documentId);

        webClientBuilder.build()
                .delete()
                .uri(documentServiceUrl + "/api/documents/" + documentId)
                // X-User-Id auto-propagated by UserContextWebClientFilter
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
