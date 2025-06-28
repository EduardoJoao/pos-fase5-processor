package com.core.domain.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private ResponseBytes<GetObjectResponse> responseBytes;

    @InjectMocks
    private S3StorageService service;

    @BeforeEach
    void setUp() throws Exception {
        var bucketField = S3StorageService.class.getDeclaredField("s3Bucket");
        bucketField.setAccessible(true);
        bucketField.set(service, "bucket");
        var processedField = S3StorageService.class.getDeclaredField("s3BucketProcessed");
        processedField.setAccessible(true);
        processedField.set(service, "bucket-processed");
    }

    @Test
    void download_shouldWriteFile() throws Exception {
        // Arrange
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asByteArray()).thenReturn("abc".getBytes());
        
        // Act
        Path file = service.download("test-key");
        
        // Assert
        assertThat(Files.readAllBytes(file)).isEqualTo("abc".getBytes());
        
        // Verify S3 call
        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(requestCaptor.capture());
        
        GetObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("bucket");
        assertThat(request.key()).isEqualTo("test-key");
        
        // Cleanup
        Files.deleteIfExists(file);
    }

    @Test
    void download_shouldThrowIOException_whenS3Fails() {
        // Arrange
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(new RuntimeException("S3 error"));
        
        // Act & Assert
        assertThrows(IOException.class, () -> service.download("test-key"));
    }

    @Test
    void upload_withBytes_shouldCallS3Client() throws Exception {
        // Arrange
        byte[] testData = "test content for zip".getBytes();
        
        // Act
        service.upload("test-key", testData);

        // Assert
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());
        
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("bucket-processed");
        assertThat(request.key()).isEqualTo("test-key");
        assertThat(request.contentType()).isEqualTo("application/zip");
        assertThat(request.contentLength()).isEqualTo(testData.length);
    }

    @Test
    void upload_withBytes_shouldThrowIOException_whenS3Fails() {
        // Arrange
        byte[] testData = "test content".getBytes();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(new RuntimeException("S3 upload error"));
        
        // Act & Assert
        assertThrows(IOException.class, () -> service.upload("test-key", testData));
    }

    @Test
    void upload_withEmptyBytes_shouldWork() throws Exception {
        // Arrange
        byte[] emptyData = new byte[0];
        
        // Act
        service.upload("empty-key", emptyData);

        // Assert
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.contentLength()).isEqualTo(0L);
    }

    @Test
    void upload_withLargeBytes_shouldWork() throws Exception {
        // Arrange
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        
        // Act
        service.upload("large-key", largeData);

        // Assert
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.contentLength()).isEqualTo(largeData.length);
        assertThat(request.contentType()).isEqualTo("application/zip");
    }
}