package com.core.domain.video.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ZipServiceTest {

    @Test
    void zipFramesInMemory_shouldCreateZipBytes() throws Exception {
        // Arrange
        List<FrameExtractorService.FrameData> frames = Arrays.asList(
            new FrameExtractorService.FrameData("frame1.jpg", "conteudo1".getBytes()),
            new FrameExtractorService.FrameData("frame2.jpg", "conteudo2".getBytes())
        );
        
        ZipService service = new ZipService();
        
        // Act
        byte[] zipBytes = service.zipFramesInMemory(frames, "test");
        
        // Assert
        assertThat(zipBytes).isNotEmpty();
        
        // Verificar conteúdo do ZIP
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry1 = zis.getNextEntry();
            assertThat(entry1.getName()).isEqualTo("frame1.jpg");
            
            ZipEntry entry2 = zis.getNextEntry();
            assertThat(entry2.getName()).isEqualTo("frame2.jpg");
        }
    }

    @Test
    void zipFramesInMemory_emptyList_shouldCreateEmptyZip() throws Exception {
        ZipService service = new ZipService();
        byte[] zipBytes = service.zipFramesInMemory(Arrays.asList(), "empty");
        assertThat(zipBytes).isNotEmpty(); // ZIP vazio ainda tem headers
    }
}