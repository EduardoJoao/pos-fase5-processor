package com.core.domain.video.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FrameExtractorServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void extractFrames_shouldCreateFramesDirectory() throws Exception {
        // Arrange
        FrameExtractorService service = new FrameExtractorService();
        // Cria um arquivo de vídeo fake (o método real espera um vídeo válido, então aqui só testamos a estrutura)
        Path fakeVideo = Files.createTempFile(tempDir, "video", ".mp4");

        // Act & Assert
        assertThrows(Exception.class, () -> service.extractFrames(fakeVideo));
        // O método real depende de um vídeo válido, então esperamos uma exceção.
    }

    @Test
    void extractFrames_withValidVideo_shouldNotThrow(@TempDir Path tempDir) throws Exception {
        FrameExtractorService service = new FrameExtractorService();
        // Copie um vídeo de teste válido para tempDir
        Path video = tempDir.resolve("Teste2.mp4");
        Files.copy(getClass().getResourceAsStream("/Teste2.mp4"), video);

        assertDoesNotThrow(() -> service.extractFrames(video));
    }

    @Test
    void resizeImage_shouldResize() {
        FrameExtractorService service = new FrameExtractorService();
        BufferedImage original = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        // Use reflection para acessar método privado se necessário
        try {
            var method = FrameExtractorService.class.getDeclaredMethod("resizeImage", BufferedImage.class);
            method.setAccessible(true);
            BufferedImage resized = (BufferedImage) method.invoke(service, original);
            assertNotNull(resized);
            assertTrue(resized.getWidth() <= 640); // MAX_WIDTH padrão
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void saveAsCompressedJpeg_shouldCreateFile(@TempDir Path tempDir) {
        FrameExtractorService service = new FrameExtractorService();
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Path output = tempDir.resolve("test.jpg");
        try {
            var method = FrameExtractorService.class.getDeclaredMethod("saveAsCompressedJpeg", BufferedImage.class, Path.class, float.class);
            method.setAccessible(true);
            method.invoke(service, img, output, 0.5f);
            assertTrue(Files.exists(output));
        } catch (Exception e) {
            fail(e);
        }
    }
}