package com.core.domain.video.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrameExtractorServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void extractFrames_shouldCreateFramesDirectory() throws Exception {
        FrameExtractorService service = new FrameExtractorService();
        Path fakeVideo = Files.createTempFile(tempDir, "video", ".mp4");

        // Arquivo fake deve gerar exceção
        assertThrows(Exception.class, () -> service.extractFrames(fakeVideo));
    }

    @Test
    void extractFramesToMemory_withRealVideo_shouldExtractFrames() throws Exception {
        // Arrange
        FrameExtractorService service = new FrameExtractorService();
        Path realVideo = copyVideoFromResources("Teste2.mp4");
        
        try {
            // Act
            List<FrameExtractorService.FrameData> frames = service.extractFramesToMemory(realVideo);
            
            // Assert
            assertNotNull(frames);
            assertFalse(frames.isEmpty(), "Deveria extrair pelo menos 1 frame");
            assertTrue(frames.size() <= 50, "Não deveria extrair mais que 50 frames");
            
            // Verificar primeiro frame
            FrameExtractorService.FrameData firstFrame = frames.get(0);
            assertNotNull(firstFrame.getFilename());
            assertTrue(firstFrame.getFilename().startsWith("frame_"));
            assertTrue(firstFrame.getFilename().endsWith(".jpg"));
            assertNotNull(firstFrame.getData());
            assertTrue(firstFrame.getData().length > 0, "Frame deve ter dados");
            
            // Log para debug
            System.out.println("Frames extraídos: " + frames.size());
            System.out.println("Primeiro frame: " + firstFrame.getFilename() + " (" + firstFrame.getData().length + " bytes)");
            
        } finally {
            Files.deleteIfExists(realVideo);
        }
    }

    @Test
    void extractFrames_withRealVideo_shouldCreateDirectory() throws Exception {
        // Arrange
        FrameExtractorService service = new FrameExtractorService();
        Path realVideo = copyVideoFromResources("Teste2.mp4");
        
        try {
            // Act
            Path framesDir = service.extractFrames(realVideo);
            
            // Assert
            assertNotNull(framesDir);
            assertTrue(Files.exists(framesDir));
            assertTrue(Files.isDirectory(framesDir));
            
            // Verificar se tem arquivos
            long frameCount = Files.list(framesDir)
                    .filter(p -> p.getFileName().toString().endsWith(".jpg"))
                    .count();
            
            assertTrue(frameCount > 0, "Deveria ter criado pelo menos 1 frame");
            
            // Log para debug
            System.out.println("Diretório criado: " + framesDir);
            System.out.println("Frames criados: " + frameCount);
            
            // Cleanup
            Files.walk(framesDir)
                    .sorted((a, b) -> b.compareTo(a)) // Deletar arquivos antes de diretórios
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
            
        } finally {
            Files.deleteIfExists(realVideo);
        }
    }

    @Test
    void extractFramesToMemory_withFakeVideo_shouldThrow() throws Exception {
        FrameExtractorService service = new FrameExtractorService();
        Path fakeVideo = Files.createTempFile(tempDir, "video", ".mp4");

        // Arquivo fake deve gerar exceção
        assertThrows(Exception.class, () -> service.extractFramesToMemory(fakeVideo));
    }

    @Test
    void resizeImage_shouldResize() {
        FrameExtractorService service = new FrameExtractorService();
        BufferedImage original = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        
        try {
            var method = FrameExtractorService.class.getDeclaredMethod("resizeImage", BufferedImage.class);
            method.setAccessible(true);
            BufferedImage resized = (BufferedImage) method.invoke(service, original);
            assertNotNull(resized);
            assertTrue(resized.getWidth() <= 640); // MAX_WIDTH
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void convertToJpegBytes_shouldCreateBytes() {
        FrameExtractorService service = new FrameExtractorService();
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        
        try {
            var method = FrameExtractorService.class.getDeclaredMethod("convertToJpegBytes", BufferedImage.class, float.class);
            method.setAccessible(true);
            byte[] result = (byte[]) method.invoke(service, img, 0.6f);
            assertNotNull(result);
            assertTrue(result.length > 0);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void frameData_shouldStoreData() {
        String filename = "test.jpg";
        byte[] data = "test data".getBytes();
        
        FrameExtractorService.FrameData frameData = new FrameExtractorService.FrameData(filename, data);
        
        assertEquals(filename, frameData.getFilename());
        assertArrayEquals(data, frameData.getData());
    }

    /**
     * Copia o vídeo de test/resources para um arquivo temporário
     */
    private Path copyVideoFromResources(String videoName) throws IOException {
        InputStream videoStream = getClass().getClassLoader().getResourceAsStream(videoName);
        if (videoStream == null) {
            throw new IOException("Vídeo não encontrado em resources: " + videoName);
        }
        
        Path tempVideo = Files.createTempFile("test-video", ".mp4");
        Files.copy(videoStream, tempVideo, StandardCopyOption.REPLACE_EXISTING);
        videoStream.close();
        
        return tempVideo;
    }
}