package com.core.domain.video.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String emailFrom;

    public void sendProcessingErrorEmail(String clientId, String videoId, String filename, String errorMessage) {

        try {
            // Em um sistema real, você buscaria o email do cliente em um banco de dados usando o clientId
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(clientId); // Simulando um email
            message.setSubject("Falha no processamento do seu vídeo");
            
            // Construir corpo do email
            String emailContent = buildEmailContent(clientId, videoId, filename, errorMessage);
            message.setText(emailContent);
            
            mailSender.send(message);
            log.info("Email de erro enviado para cliente {} sobre o vídeo {}", clientId, videoId);
        } catch (Exception e) {
            log.error("Falha ao enviar email de notificação: {}", e.getMessage(), e);
        }
    }
    
    private String buildEmailContent(String clientId, String videoId, String filename, String errorMessage) {
        StringBuilder content = new StringBuilder();
        content.append("Prezado Cliente,\n\n");
        content.append("Infelizmente, não foi possível processar seu vídeo.\n\n");
        content.append("Detalhes do vídeo:\n");
        content.append("- Nome do arquivo: ").append(filename).append("\n");
        content.append("- Identificador do vídeo: ").append(videoId).append("\n\n");
        content.append("Por favor, entre em contato com nosso suporte técnico caso precise de ajuda adicional.\n\n");
        content.append("Atenciosamente,\nEquipe de Suporte");
        
        return content.toString();
    }
}