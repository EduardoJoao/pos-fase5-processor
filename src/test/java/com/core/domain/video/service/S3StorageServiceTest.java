package com.core.domain.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(s3Client.getObjectAsBytes((GetObjectRequest) any())).thenReturn(responseBytes);
        when(responseBytes.asByteArray()).thenReturn("abc".getBytes());
        Path file = service.download("key");
        assertThat(Files.readAllBytes(file)).isEqualTo("abc".getBytes());
        Files.deleteIfExists(file);
    }

    @Test
    void upload_shouldCallS3Client() throws Exception {
        Path tempFile = Files.createTempFile("upload-test", ".txt");
        Files.writeString(tempFile, "conteudo");
        service.upload("key-upload", tempFile);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        Files.deleteIfExists(tempFile);
    }
}