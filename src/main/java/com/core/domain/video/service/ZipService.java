package com.core.domain.video.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {
    public Path zipDirectory(Path dir, String name) throws IOException {
        Path zipPath = Files.createTempFile(name, ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            // Definir nível máximo de compressão
            zos.setLevel(Deflater.BEST_COMPRESSION);
            
            Files.list(dir).forEach(path -> {
                try {
                    zos.putNextEntry(new ZipEntry(path.getFileName().toString()));
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) { throw new RuntimeException(e); }
            });
        }
        return zipPath;
    }
}