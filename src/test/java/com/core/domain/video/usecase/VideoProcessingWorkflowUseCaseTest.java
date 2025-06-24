package com.core.domain.video.usecase;

import com.core.adapters.gateway.UpdateVideoStatusApiClient;
import com.core.domain.video.dto.VideoProcessRequest;
import com.core.domain.video.notification.EmailNotificationService;
import com.core.domain.video.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoProcessingWorkflowUseCaseTest {

    private S3StorageService s3StorageService;
    private FrameExtractorService frameExtractorService;
    private ZipService zipService;
    private UpdateVideoStatusApiClient updateVideoStatusApiClient;
    private EmailNotificationService emailNotificationService;
    private VideoProcessingWorkflowUseCase useCase;

    @BeforeEach
    void setUp() {
        s3StorageService = mock(S3StorageService.class);
        frameExtractorService = mock(FrameExtractorService.class);
        zipService = mock(ZipService.class);
        updateVideoStatusApiClient = mock(UpdateVideoStatusApiClient.class);
        emailNotificationService = mock(EmailNotificationService.class);
        useCase = new VideoProcessingWorkflowUseCase(
                s3StorageService, frameExtractorService, zipService,
                updateVideoStatusApiClient, emailNotificationService
        );
    }

    @Test
    void processVideo_success() throws Exception {
        VideoProcessRequest req = new VideoProcessRequest("key", "vid", "cli", "file.mp4", "video/mp4", 0L);
        Path videoPath = Files.createTempFile("v", ".mp4");
        Path framesDir = Files.createTempDirectory("f");
        Path zipPath = Files.createTempFile("z", ".zip");
        when(s3StorageService.download(anyString())).thenReturn(videoPath);
        when(frameExtractorService.extractFrames(any())).thenReturn(framesDir);
        when(zipService.zipDirectory(any(), anyString())).thenReturn(zipPath);

        useCase.processVideo(req);

        verify(updateVideoStatusApiClient).execute(eq("cli"), eq("vid"), eq("PROCESSING"), isNull(), isNull(), isNull());
        verify(updateVideoStatusApiClient).execute(eq("cli"), eq("vid"), eq("SUCCESS"), isNull(), anyString(), anyString());
        verify(emailNotificationService, never()).sendProcessingErrorEmail(any(), any(), any(), any());

        Files.deleteIfExists(videoPath);
        Files.deleteIfExists(zipPath);
        Files.deleteIfExists(framesDir);
    }

    @Test
    void processVideo_error() throws Exception {
        VideoProcessRequest req = new VideoProcessRequest("key", "vid", "cli", "file.mp4", "video/mp4", 0L);
        when(s3StorageService.download(anyString())).thenThrow(new RuntimeException("fail"));

        useCase.processVideo(req);

        verify(updateVideoStatusApiClient).execute(eq("cli"), eq("vid"), eq("PROCESSING"), isNull(), isNull(), isNull());
        verify(updateVideoStatusApiClient).execute(eq("cli"), eq("vid"), eq("ERROR"), contains("fail"), isNull(), isNull());
        verify(emailNotificationService).sendProcessingErrorEmail(eq("cli"), eq("vid"), eq("file.mp4"), contains("fail"));
    }
}