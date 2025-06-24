package com.core.domain.video.usecase;

import com.core.adapters.gateway.UpdateVideoStatusApiClient;
import com.core.domain.video.dto.VideoProcessRequest;
import com.core.domain.video.notification.EmailNotificationService;
import com.core.domain.video.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProcessingWorkflowUseCase {
    private final S3StorageService s3StorageService;
    private final FrameExtractorService frameExtractorService;
    private final ZipService zipService;
    private final UpdateVideoStatusApiClient updateVideoStatusApiClient;
    private final EmailNotificationService emailNotificationService;

    public void processVideo(VideoProcessRequest request) {
        String clientId = request.getClientId();
        String videoId = request.getVideoId();
        String videoKey = request.getS3Key();
        
        try {
            // Atualizar status para PROCESSING
            updateVideoStatusApiClient.execute(clientId, videoId, "PROCESSING", null, null, null);
            
            // Download do vídeo do S3
            Path videoPath = s3StorageService.download(videoKey);
            try {
                // Extração de frames
                Path framesDir = frameExtractorService.extractFrames(videoPath);
                try {
                    // Criação do ZIP
                    String baseFilename = request.getFilename().replaceFirst("[.][^.]+$", "");
                    Path zipPath = zipService.zipDirectory(framesDir, baseFilename);
                    try {
                        // Upload do ZIP para o S3
                        String zipKey = clientId + "/" + baseFilename + ".zip";
                        s3StorageService.upload(zipKey, zipPath);
                        long zipFileSize = Files.size(zipPath);
                        // Atualizar status para SUCCESS
                        updateVideoStatusApiClient.execute(clientId, videoId, "SUCCESS", null, baseFilename + ".zip", getVideoSizeInMB(zipFileSize));
                    } finally {
                        Files.deleteIfExists(zipPath);
                    }
                } finally {
                    deleteDirectory(framesDir);
                }
            } finally {
                Files.deleteIfExists(videoPath);
            }
        } catch (Exception e) {
            String errorMsg = "Erro ao processar vídeo: " + e.getMessage();
            log.error(errorMsg, e);
            
            // Atualizar status para ERROR na API Core
            updateVideoStatusApiClient.execute(clientId, videoId, "ERROR", errorMsg, null, null);

            // Enviar email de notificação sobre o erro
            emailNotificationService.sendProcessingErrorEmail(
                clientId, 
                videoId, 
                request.getFilename(), 
                errorMsg
            );
        }
    }

    private void deleteDirectory(Path directory) {
        try {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        log.error("Erro ao deletar arquivo: {}", path, e);
                    }
                });
        } catch (Exception e) {
            log.error("Erro ao deletar diretório: {}", directory, e);
        }
    }

    public String getVideoSizeInMB(long videoSize) {
        double sizeInMB = (double) videoSize / (1024 * 1024);
        return String.format("%.2f MB", sizeInMB);
    }
}