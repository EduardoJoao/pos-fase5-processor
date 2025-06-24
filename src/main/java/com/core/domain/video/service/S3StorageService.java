package com.core.domain.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {
    @Value("${s3.bucket}")
    private String s3Bucket;

    @Value("${s3.bucket.processed}")
    private String s3BucketProcessed;

    private final S3Client s3Client;

    public Path download(String key) throws IOException {
        try {
            // Criar o arquivo temporário primeiro
            Path tempFile = Files.createTempFile("video", ".mp4");
            
            // Log para verificar o caminho do arquivo
            log.info("Tentando baixar o arquivo {} do bucket {} para {}", key, s3Bucket, tempFile);
            
            // Baixar o objeto usando ResponseBytes (método alternativo)
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(key)
                            .build());
            
            // Escrever bytes no arquivo
            Files.write(tempFile, objectBytes.asByteArray(), StandardOpenOption.WRITE);
            
            log.info("Arquivo baixado com sucesso: {}", tempFile);
            return tempFile;
        } catch (S3Exception e) {
            log.error("Erro ao baixar arquivo do S3: {}", e.getMessage(), e);
            throw new IOException("Erro ao baixar arquivo do S3: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erro inesperado ao baixar arquivo: {}", e.getMessage(), e);
            throw new IOException("Erro inesperado ao baixar arquivo: " + e.getMessage(), e);
        }
    }

    public void upload(String key, Path file) {
        try {
            log.info("Fazendo upload do arquivo {} para o bucket {} com a chave {}", 
                    file, s3BucketProcessed, key);
                    
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3BucketProcessed)
                            .key(key)
                            .build(),
                    RequestBody.fromFile(file));
                    
            log.info("Upload concluído com sucesso");
        } catch (Exception e) {
            log.error("Erro ao fazer upload para o S3: {}", e.getMessage(), e);
            throw e;
        }
    }
}