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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

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

    public void upload(String key, byte[] data) throws IOException {
        try {
            log.info("Fazendo upload de {} bytes para o bucket {} com a chave {}", 
                    data.length, s3BucketProcessed, key);
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3BucketProcessed)
                    .key(key)
                    .contentLength((long) data.length)
                    .contentType("application/zip")
                    .build();

            RequestBody requestBody = RequestBody.fromBytes(data);
            s3Client.putObject(putRequest, requestBody);
            
            log.info("Upload de {} bytes concluído com sucesso", data.length);
        } catch (Exception e) {
            log.error("Erro ao fazer upload de bytes para S3: {}", e.getMessage(), e);
            throw new IOException("Falha no upload para S3", e);
        }
    }
}