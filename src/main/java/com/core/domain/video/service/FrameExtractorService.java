package com.core.domain.video.service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class FrameExtractorService {
    // Ajuste esses parâmetros para equilibrar qualidade e tamanho
    private static final float JPEG_QUALITY = 0.7f;  // 0.0-1.0 (menor = mais compressão)
    private static final int MAX_WIDTH = 640;       // Máxima largura em pixels
    private static final int FRAME_INTERVAL = 1;    // Capturar um frame a cada X segundos

    public Path extractFrames(Path videoPath) throws IOException {
        Path framesDir = Files.createTempDirectory("frames");
        log.info("Extraindo frames do vídeo {} para o diretório {}", videoPath, framesDir);
        
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath.toFile())) {
            grabber.start();
            
            double frameRate = grabber.getVideoFrameRate();
            log.info("Taxa de frames do vídeo: {} fps", frameRate);
            
            // Capturar um frame a cada X segundos
            int framesPerSecond = (int) Math.round(frameRate);
            int framesToSkip = framesPerSecond * FRAME_INTERVAL;
            
            Java2DFrameConverter converter = new Java2DFrameConverter();
            Frame frame;
            int frameNumber = 0;
            int savedFrames = 0;
            
            while ((frame = grabber.grab()) != null) {
                if (frameNumber % framesToSkip == 0 && frame.image != null) {
                    BufferedImage bufferedImage = converter.convert(frame);
                    
                    if (bufferedImage != null) {
                        // Redimensionar a imagem se necessário
                        if (bufferedImage.getWidth() > MAX_WIDTH) {
                            bufferedImage = resizeImage(bufferedImage);
                        }
                        
                        // Salvar como JPEG comprimido
                        Path outputPath = framesDir.resolve(String.format("frame_%04d.jpg", savedFrames));
                        saveAsCompressedJpeg(bufferedImage, outputPath, JPEG_QUALITY);
                        savedFrames++;
                    }
                }
                frameNumber++;
            }
            
            log.info("Extração concluída: {} frames salvos", savedFrames);
            return framesDir;
        } catch (Exception e) {
            log.error("Erro ao extrair frames: {}", e.getMessage(), e);
            throw new IOException("Falha ao processar o vídeo: " + e.getMessage(), e);
        }
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // Calcular nova altura mantendo a proporção
        int newHeight = (int) (originalHeight * ((double) MAX_WIDTH / originalWidth));
        
        BufferedImage resizedImage = new BufferedImage(MAX_WIDTH, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, MAX_WIDTH, newHeight, null);
        g.dispose();
        
        return resizedImage;
    }
    
    private void saveAsCompressedJpeg(BufferedImage image, Path outputPath, float quality) throws IOException {
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(quality);
        
        try (OutputStream os = Files.newOutputStream(outputPath);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            jpgWriter.setOutput(ios);
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
        } finally {
            jpgWriter.dispose();
        }
    }
}