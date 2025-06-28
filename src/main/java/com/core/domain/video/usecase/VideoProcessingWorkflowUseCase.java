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
import java.util.List;

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
                // Extração de frames EM MEMÓRIA
                List<FrameExtractorService.FrameData> frames = frameExtractorService.extractFramesToMemory(videoPath);
                
                // Criação do ZIP EM MEMÓRIA
                String baseFilename = request.getFilename().replaceFirst("[.][^.]+$", "");
                byte[] zipData = zipService.zipFramesInMemory(frames, baseFilename);
                
                // Upload direto da memória para S3
                String zipKey = clientId + "/" + baseFilename + ".zip";
                s3StorageService.upload(zipKey, zipData);
                
                // Atualizar status para SUCCESS
                String zipSize = String.format("%.2f MB", zipData.length / (1024.0 * 1024.0));
                updateVideoStatusApiClient.execute(clientId, videoId, "SUCCESS", null, baseFilename + ".zip", zipSize);
                
            } finally {
                Files.deleteIfExists(videoPath);
            }
            
        } catch (Exception e) {
            String errorMsg = "Erro ao processar vídeo: " + e.getMessage();
            log.error(errorMsg, e);
            
            updateVideoStatusApiClient.execute(clientId, videoId, "ERROR", errorMsg, null, null);
            emailNotificationService.sendProcessingErrorEmail(clientId, videoId, request.getFilename(), errorMsg);
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