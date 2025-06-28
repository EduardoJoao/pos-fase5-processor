package com.core.domain.video.service;

import com.core.domain.video.service.FrameExtractorService.FrameData;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {
    
    public byte[] zipFramesInMemory(List<FrameData> frames, String name) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            // Configurar compressão máxima
            zos.setLevel(Deflater.BEST_COMPRESSION);
            
            for (FrameData frame : frames) {
                ZipEntry entry = new ZipEntry(frame.getFilename());
                zos.putNextEntry(entry);
                
                try (ByteArrayInputStream bais = new ByteArrayInputStream(frame.getData())) {
                    bais.transferTo(zos);
                }
                
                zos.closeEntry();
            }
            
            zos.finish();
            return baos.toByteArray();
        }
    }
}